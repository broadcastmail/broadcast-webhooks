package com.broadcastmail.webhooks.resend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ResendWebhookEvent(
        String type,
        @JsonProperty("created_at") String createdAt,
        ResendWebhookEventData data
) {
    public record ResendWebhookEventData(
            @JsonProperty("email_id") String emailId,
            @JsonProperty("to") List<String> to,
            @JsonProperty("bounce") BounceInfo bounce
    ) {
        public record BounceInfo(String type, String message) {}
    }
}
