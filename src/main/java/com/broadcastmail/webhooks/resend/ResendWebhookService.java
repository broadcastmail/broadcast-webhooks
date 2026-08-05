package com.broadcastmail.webhooks.resend;

import com.broadcastmail.webhooks.resend.dto.ResendWebhookEvent;
import com.broadcastmail.webhooks.webhookevents.WebhookEvent;
import com.broadcastmail.webhooks.webhookevents.WebhookEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.json.JsonParseException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResendWebhookService {

    private final DeliveryEventHandler deliveryEventHandler;
    private final WebhookEventRepository webhookEventRepository;
    private final ObjectMapper mapper;

    @Transactional
    public void process(String svixId, String payload) {
        if (webhookEventRepository.existsByProviderEventId(svixId)) {
            return;
        }

        try {
            ResendWebhookEvent event = mapper.readValue(payload, ResendWebhookEvent.class);
            String emailId = event.data().emailId();
            Map<String, Object> rawPayloadMap = mapper.readValue(payload,
                    new TypeReference<Map<String, Object>>() {});

            WebhookEvent webhookEvent = WebhookEvent.builder()
                    .providerEventId(svixId)
                    .eventType(event.type())
                    .rawPayload(rawPayloadMap)
                    .processed(false)
                    .build();
            webhookEventRepository.save(webhookEvent);

            switch (event.type()) {
                case "email.delivered" -> deliveryEventHandler.handleDelivered(emailId);
                case "email.opened"    -> deliveryEventHandler.handleOpened(emailId);
                case "email.bounced"   -> deliveryEventHandler.handleBounced(emailId);
                case "email.failed" -> deliveryEventHandler.handleFailed(emailId,
                        event.data().bounce() != null ? event.data().bounce().message() : "Unknown error");
                default -> {
                    // Empty block
                }
            }

            webhookEvent.setProcessed(true);
            webhookEventRepository.save(webhookEvent);

        } catch (JsonParseException _) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payload");
        }
    }
}
