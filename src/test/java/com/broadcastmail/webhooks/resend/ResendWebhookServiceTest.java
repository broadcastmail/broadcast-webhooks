package com.broadcastmail.webhooks.resend;

import com.broadcastmail.webhooks.resend.dto.ResendWebhookEvent;
import com.broadcastmail.webhooks.webhookevents.WebhookEvent;
import com.broadcastmail.webhooks.webhookevents.WebhookEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.json.JsonParseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResendWebhookServiceTest {

    @Mock
    private DeliveryEventHandler deliveryEventHandler;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    private ResendWebhookService resendWebhookService;

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final String SVIX_ID = "msg_2PCQznFMnfnmYNzM79oDe9Rl3Jz";
    private static final String PAYLOAD = """
            {"type":"email.delivered","created_at":"2026-08-05T10:00:00Z","data":{"email_id":"msg-123"}}
            """;
    private static final String COMPLAINT_PAYLOAD = """
            {"type":"email.complained","created_at":"2026-08-05T10:00:00Z","data":{"email_id":"msg-123"}}
            """;

    @Test
    void shouldProcessDeliveredEventSuccessfully() throws Exception {
        // Given
        when(webhookEventRepository.existsByProviderEventId(SVIX_ID)).thenReturn(false);
        ResendWebhookEvent event = new ResendWebhookEvent(
                "email.delivered", "2026-08-05T10:00:00Z",
                new ResendWebhookEvent.ResendWebhookEventData("msg-123", List.of("user@example.com"), null));
        when(mapper.readValue(PAYLOAD, ResendWebhookEvent.class)).thenReturn(event);

        // When
        resendWebhookService.process(ACCOUNT_ID, SVIX_ID, PAYLOAD);

        // Then
        verify(deliveryEventHandler).handleDelivered("msg-123");
        verify(webhookEventRepository, times(2)).save(any(WebhookEvent.class));    }

    @Test
    void shouldProcessComplainedEventSuccessfully() {
        // Given
        when(webhookEventRepository.existsByProviderEventId(SVIX_ID)).thenReturn(false);
        ResendWebhookEvent event = new ResendWebhookEvent(
                "email.complained", "2026-08-05T10:00:00Z",
                new ResendWebhookEvent.ResendWebhookEventData("msg-123", List.of("user@example.com"), null));
        when(mapper.readValue(COMPLAINT_PAYLOAD, ResendWebhookEvent.class)).thenReturn(event);

        // When
        resendWebhookService.process(ACCOUNT_ID, SVIX_ID, COMPLAINT_PAYLOAD);

        // Then
        verify(deliveryEventHandler).handleComplained(ACCOUNT_ID, List.of("user@example.com"));
        verify(webhookEventRepository, times(2)).save(any(WebhookEvent.class));
    }

    @Test
    void shouldSkipDuplicateEventByProviderEventId() {
        // Given
        when(webhookEventRepository.existsByProviderEventId(SVIX_ID)).thenReturn(true);

        // When
        resendWebhookService.process(ACCOUNT_ID, SVIX_ID, PAYLOAD);

        // Then
        verify(deliveryEventHandler, never()).handleDelivered(any());
        verify(webhookEventRepository, never()).save(any());
    }

    @Test
    void shouldThrowOnInvalidPayload() throws Exception {
        // Given
        when(webhookEventRepository.existsByProviderEventId(SVIX_ID)).thenReturn(false);
        when(mapper.readValue(PAYLOAD, ResendWebhookEvent.class))
                .thenThrow(new JsonParseException() {});

        // When / Then
        assertThatThrownBy(() -> resendWebhookService.process(ACCOUNT_ID, SVIX_ID, PAYLOAD))
                .isInstanceOf(ResponseStatusException.class);
    }
}