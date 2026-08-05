package com.broadcastmail.webhooks.resend;

import com.broadcastmail.common.campaign.CampaignRepository;
import com.broadcastmail.common.campaign.recipient.CampaignRecipient;
import com.broadcastmail.common.campaign.recipient.CampaignRecipientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryEventHandlerTest {

    @Mock
    private CampaignRecipientRepository campaignRecipientRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @InjectMocks
    private DeliveryEventHandler deliveryEventHandler;

    private static final String EMAIL_ID = "msg-123";
    private static final UUID CAMPAIGN_ID = UUID.randomUUID();

    private CampaignRecipient recipient() {
        return CampaignRecipient.builder()
                .id(UUID.randomUUID())
                .campaignId(CAMPAIGN_ID)
                .build();
    }

    private void stubRecipient() {
        when(campaignRecipientRepository.findByResendMessageId(EMAIL_ID))
                .thenReturn(Optional.of(recipient()));
    }

    @Test
    void shouldMarkRecipientDeliveredAndIncrementCounter() {
        // Given
        stubRecipient();

        // When
        deliveryEventHandler.handleDelivered(EMAIL_ID);

        // Then
        verify(campaignRecipientRepository).markDelivered(EMAIL_ID);
        verify(campaignRepository).incrementDeliveredCount(CAMPAIGN_ID);
    }

    @Test
    void shouldMarkRecipientOpenedAndIncrementCounter() {
        // Given
        stubRecipient();

        // When
        deliveryEventHandler.handleOpened(EMAIL_ID);

        // Then
        verify(campaignRecipientRepository).markOpened(EMAIL_ID);
        verify(campaignRepository).incrementOpenedCount(CAMPAIGN_ID);
    }

    @Test
    void shouldMarkRecipientBouncedAndIncrementCounter() {
        // Given
        stubRecipient();

        // When
        deliveryEventHandler.handleBounced(EMAIL_ID);

        // Then
        verify(campaignRecipientRepository).markBounced(EMAIL_ID);
        verify(campaignRepository).incrementBouncedCount(CAMPAIGN_ID);
    }

    @Test
    void shouldMarkRecipientFailedAndIncrementCounter() {
        // Given
        stubRecipient();

        // When
        deliveryEventHandler.handleFailed(EMAIL_ID, "Bounce: hard bounce");

        // Then
        verify(campaignRecipientRepository).markFailed(EMAIL_ID, "Bounce: hard bounce");
        verify(campaignRepository).incrementFailedCount(CAMPAIGN_ID);
    }

    @Test
    void shouldThrowWhenRecipientNotFound() {
        // Given
        when(campaignRecipientRepository.findByResendMessageId(EMAIL_ID))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> deliveryEventHandler.handleDelivered(EMAIL_ID))
                .isInstanceOf(ResponseStatusException.class);
    }
}