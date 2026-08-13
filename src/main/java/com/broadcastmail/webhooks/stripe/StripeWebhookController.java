package com.broadcastmail.webhooks.stripe;

import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.broadcastmail.webhooks.config.StripeProperties;

import java.util.Map;

@RestController
@RequestMapping("/webhooks/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {
    private final StripeSignatureVerifier webhookSignatureVerifier;
    private final StripeProperties stripeProperties;
    private final StripeWebhookService stripeWebhookService;


    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestBody String payload,
            @RequestHeader Map<String, String> headers
    ) {
        Event event = webhookSignatureVerifier.verify(stripeProperties.webhookSecret(), payload, headers);
        stripeWebhookService.process(event);
        return ResponseEntity.ok().build();
    }
}
