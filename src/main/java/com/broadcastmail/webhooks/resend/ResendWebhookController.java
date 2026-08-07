package com.broadcastmail.webhooks.resend;

import com.broadcastmail.common.emailprovider.EmailProvider;
import com.broadcastmail.common.emailprovider.EmailProviderRepository;
import com.broadcastmail.webhooks.common.SecurityUtil;
import com.broadcastmail.webhooks.config.EncryptionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/webhooks/resend")
@RequiredArgsConstructor
public class ResendWebhookController {

    private final EmailProviderRepository emailProviderRepository;
    private final EncryptionProperties encryptionProperties;
    private final ResendWebhookService resendWebhookService;
    private final WebhookSignatureVerifier signatureVerifier;

    @PostMapping("/{accountId}")
    public ResponseEntity<Void> receive(
            @PathVariable UUID accountId,
            @RequestBody String payload,
            @RequestHeader Map<String, String> headers
    ) {
        EmailProvider provider = emailProviderRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown account"));

        if (provider.getEncryptedWebhookSecret() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No webhook secret registered for account");
        }

        String secret = SecurityUtil.decrypt(provider.getEncryptedWebhookSecret(), encryptionProperties.key());

        signatureVerifier.verify(secret, payload, headers);
        String svixId = headers.get("svix-id");
        if (svixId == null || svixId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing svix-id header");
        }
        resendWebhookService.process(svixId, payload);

        return ResponseEntity.ok().build();
    }
}
