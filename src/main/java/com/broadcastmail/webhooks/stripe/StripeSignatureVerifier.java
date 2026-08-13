package com.broadcastmail.webhooks.stripe;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Component
public class StripeSignatureVerifier {

    public Event verify(String secret, String payload, Map<String, String> headers) {
        String sigHeader = headers.get("stripe-signature");
        if (sigHeader == null || sigHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Stripe-Signature header");
        }
        try {
            return Webhook.constructEvent(payload, sigHeader, secret);
        } catch (SignatureVerificationException _) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Stripe webhook signature");
        }
    }
}