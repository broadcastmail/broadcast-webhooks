package com.broadcastmail.webhooks.resend;

import com.broadcastmail.TestContainersConfiguration;
import com.broadcastmail.common.account.Account;
import com.broadcastmail.common.account.AccountRepository;
import com.broadcastmail.common.account.plan.Plan;
import com.broadcastmail.common.suppressions.SuppressionsRepository;
import com.broadcastmail.webhooks.webhookevents.WebhookEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;
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
    private AccountRepository accountRepository;

    @Autowired
    private SuppressionsRepository suppressionsRepository;

    private UUID accountId;

    @BeforeEach
    void setUp() {
        webhookEventRepository.deleteAll();
        accountId = accountRepository.save(Account.builder()
                .email(UUID.randomUUID() + "@example.com")
                .passwordHash("")
                .apiKeyHash(UUID.randomUUID().toString())
                .plan(Plan.FREE)
                .emailVerified(true)
                .build()).getId();
    }

    @AfterEach
    void tearDown() {
        suppressionsRepository.deleteAll();
        webhookEventRepository.deleteAll();
        accountRepository.deleteAll();
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
        assertThatNoException().isThrownBy(() -> resendWebhookService.process(accountId, svixId, payload));
        resendWebhookService.process(accountId, svixId, payload);

        // Then
        assertThat(webhookEventRepository.count()).isEqualTo(1);
        assertThat(webhookEventRepository.existsByProviderEventIdAndProcessedTrue(svixId)).isTrue();
    }

    @Test
    void shouldStoreTheAccountThatReceivedTheEvent() {
        // When
        resendWebhookService.process(accountId, "msg_account", DELIVERED.formatted(UUID.randomUUID()));

        // Then
        assertThat(webhookEventRepository.existsByProviderEventIdAndAccountId("msg_account", accountId)).isTrue();
    }

    @Test
    void shouldDeleteAnAccountsEventsWithTheAccount() {
        // Given
        resendWebhookService.process(accountId, "msg_gone", DELIVERED.formatted(UUID.randomUUID()));

        // When
        accountRepository.deleteById(accountId);

        // Then
        assertThat(webhookEventRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"type\":\"email.complained\",\"created_at\":\"2026-11-25T10:00:00Z\",\"data\":{\"email_id\":\"%s\",\"to\":[\"late@example.com\"]}}",
            "{\"type\":\"email.bounced\",\"created_at\":\"2026-11-25T10:00:00Z\",\"data\":{\"email_id\":\"%s\",\"to\":[\"late@example.com\"],\"bounce\":{\"type\":\"Permanent\",\"message\":\"gone\"}}}"
    })
    void shouldSuppressALateComplaintOrHardBounceByItsAddress(String template) {
        // Given: no recipient row has this email_id any more (retention deleted it)
        String payload = template.formatted(UUID.randomUUID());

        // When
        resendWebhookService.process(accountId, "msg_" + UUID.randomUUID(), payload);

        // Then
        assertThat(suppressionsRepository.findByAccountIdAndEmailIn(accountId, List.of("late@example.com"))).hasSize(1);
    }

    private static final String DELIVERED =
            "{\"type\":\"email.delivered\",\"created_at\":\"2026-11-25T10:00:00Z\",\"data\":{\"email_id\":\"%s\"}}";
}
