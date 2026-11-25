package com.broadcastmail.webhooks.resend;

import com.broadcastmail.TestContainersConfiguration;
import com.broadcastmail.webhooks.webhookevents.WebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@SpringBootTest
@Import(TestContainersConfiguration.class)
class ResendWebhookServiceIntegrationTest {

    @Autowired
    private ResendWebhookService resendWebhookService;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        webhookEventRepository.deleteAll();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"type\":\"email.delivered\",\"created_at\":\"2026-11-25T10:00:00Z\",\"data\":{\"email_id\":\"%s\"}}",
            "{\"type\":\"email.opened\",\"created_at\":\"2026-11-25T10:00:00Z\",\"data\":{\"email_id\":\"%s\"}}",
            "{\"type\":\"email.complained\",\"created_at\":\"2026-11-25T10:00:00Z\",\"data\":{\"email_id\":\"%s\"}}",
            "{\"type\":\"email.bounced\",\"created_at\":\"2026-11-25T10:00:00Z\",\"data\":{\"email_id\":\"%s\",\"bounce\":{\"type\":\"Permanent\",\"message\":\"gone\"}}}",
            "{\"type\":\"email.failed\",\"created_at\":\"2026-11-25T10:00:00Z\",\"data\":{\"email_id\":\"%s\"}}"
    })
    void shouldStoreEventForUnknownEmailIdAsProcessed(String template) {
        // Given: an email_id no recipient has (a test send, or deleted by retention)
        String svixId = "msg_" + UUID.randomUUID();
        String payload = template.formatted(UUID.randomUUID());

        // When: Resend delivers it, then retries it
        assertThatNoException().isThrownBy(() -> resendWebhookService.process(svixId, payload));
        resendWebhookService.process(svixId, payload);

        // Then
        List<Map<String, Object>> events = jdbcTemplate.queryForList(
                "SELECT provider_event_id, processed FROM webhook_events");
        assertThat(events).hasSize(1);
        assertThat(events.getFirst())
                .containsEntry("provider_event_id", svixId)
                .containsEntry("processed", true);
    }
}
