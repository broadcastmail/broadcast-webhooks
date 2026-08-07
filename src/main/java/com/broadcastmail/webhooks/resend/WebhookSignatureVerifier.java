package com.broadcastmail.webhooks.resend;

import com.resend.core.exception.ResendException;
import com.resend.services.webhooks.Webhooks;
import com.resend.services.webhooks.model.VerifyWebhookOptions;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Component
public class WebhookSignatureVerifier {

    public void verify(String secret, String payload, Map<String, String> headers) {
        VerifyWebhookOptions options = VerifyWebhookOptions.builder()
                .addHeaders(headers)
                .payload(payload)
                .secret(secret)
                .build();
        try {
            new Webhooks("unused").verify(options);
        } catch (ResendException _) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        }
    }
}
