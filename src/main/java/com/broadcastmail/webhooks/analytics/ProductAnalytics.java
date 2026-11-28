package com.broadcastmail.webhooks.analytics;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class ProductAnalytics {
    private final String apiKey;
    private final Set<UUID> e2eAccountIds;
    private final RestClient restClient;

    public ProductAnalytics(
            @Value("${app.posthog.api-key:}") String apiKey,
            @Value("${app.posthog.host:https://eu.i.posthog.com}") String host,
            @Value("${app.e2e-account-ids:}") Set<UUID> e2eAccountIds) {
        this.apiKey = apiKey;
        this.e2eAccountIds = e2eAccountIds;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(2));
        this.restClient = RestClient.builder().baseUrl(host).requestFactory(requestFactory).build();
    }

    public void capture(UUID accountId, String event, String stripeEventId, long createdEpochSeconds) {
        if (apiKey.isBlank() || e2eAccountIds.contains(accountId)) return;
        try {
            restClient.post()
                    .uri("/i/v0/e/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "api_key", apiKey,
                            "event", event,
                            "distinct_id", accountId.toString(),
                            "uuid", UUID.nameUUIDFromBytes((event + stripeEventId).getBytes(StandardCharsets.UTF_8)).toString(),
                            "timestamp", Instant.ofEpochSecond(createdEpochSeconds).toString()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            log.warn("PostHog capture of {} for account {} failed: {}", event, accountId, e.getMessage());
        }
    }
}
