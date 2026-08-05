package com.broadcastmail.webhooks.resend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResendWebhookEvent(
        String type,
        @JsonProperty("created_at") String createdAt,
        ResendWebhookEventData data
) {
    public record ResendWebhookEventData(
            @JsonProperty("email_id") String emailId,
            @JsonProperty("bounce") BounceInfo bounce
    ) {
        public record BounceInfo(String message) {}
    }
}
