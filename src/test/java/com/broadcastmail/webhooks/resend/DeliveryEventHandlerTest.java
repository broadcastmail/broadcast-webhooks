package com.broadcastmail.webhooks.resend;

import com.broadcastmail.common.campaign.CampaignRepository;
import com.broadcastmail.common.campaign.recipient.CampaignRecipient;
import com.broadcastmail.common.campaign.recipient.CampaignRecipientRepository;
import com.broadcastmail.common.suppressions.SuppressionsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryEventHandlerTest {

    @Mock
    private CampaignRecipientRepository campaignRecipientRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private SuppressionsRepository suppressionsRepository;

    @InjectMocks
    private DeliveryEventHandler deliveryEventHandler;

    private static final String EMAIL_ID = "msg-123";
    private static final UUID CAMPAIGN_ID = UUID.randomUUID();
    private static final UUID RECIPIENT_ID = UUID.randomUUID();

    private CampaignRecipient recipient() {
        return CampaignRecipient.builder()
                .id(RECIPIENT_ID)
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
    void shouldMarkRecipientHardBouncedIncrementCounterAndSuppress() {
        // Given
        stubRecipient();

        // When
        deliveryEventHandler.handleHardBounced(EMAIL_ID);

        // Then
        verify(campaignRecipientRepository).markBounced(EMAIL_ID);
        verify(campaignRepository).incrementBouncedCount(CAMPAIGN_ID);
        verify(suppressionsRepository).insertFromRecipientWithReason(RECIPIENT_ID, "BOUNCE");
    }

    @Test
    void shouldSuppressRecipientWhoComplained() {
        // Given
        stubRecipient();

        // When
        deliveryEventHandler.handleComplained(EMAIL_ID);

        // Then
        verify(suppressionsRepository).insertFromRecipientWithReason(RECIPIENT_ID, "COMPLAINT");
    }

    @Test
    void shouldMarkRecipientSoftBouncedAndIncrementCounterWithoutSuppression() {
        // Given
        stubRecipient();

        // When
        deliveryEventHandler.handleSoftBounced(EMAIL_ID);

        // Then
        verify(campaignRecipientRepository).markBounced(EMAIL_ID);
        verify(campaignRepository).incrementBouncedCount(CAMPAIGN_ID);
        verify(suppressionsRepository, never()).insertFromRecipientWithReason(RECIPIENT_ID, "BOUNCE");
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

    static Stream<Arguments> eventsForUnknownRecipient() {
        return Stream.of(
                Arguments.of("delivered", (Consumer<DeliveryEventHandler>) h -> h.handleDelivered(EMAIL_ID)),
                Arguments.of("opened", (Consumer<DeliveryEventHandler>) h -> h.handleOpened(EMAIL_ID)),
                Arguments.of("soft bounce", (Consumer<DeliveryEventHandler>) h -> h.handleSoftBounced(EMAIL_ID)),
                Arguments.of("hard bounce", (Consumer<DeliveryEventHandler>) h -> h.handleHardBounced(EMAIL_ID)),
                Arguments.of("complained", (Consumer<DeliveryEventHandler>) h -> h.handleComplained(EMAIL_ID)),
                Arguments.of("failed", (Consumer<DeliveryEventHandler>) h -> h.handleFailed(EMAIL_ID, "reason")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("eventsForUnknownRecipient")
    void shouldIgnoreEventWhenRecipientNotFound(String name, Consumer<DeliveryEventHandler> handle) {
        // Given
        when(campaignRecipientRepository.findByResendMessageId(EMAIL_ID)).thenReturn(Optional.empty());

        // When
        handle.accept(deliveryEventHandler);

        // Then
        verify(campaignRecipientRepository).findByResendMessageId(EMAIL_ID);
        verifyNoMoreInteractions(campaignRecipientRepository);
        verifyNoInteractions(campaignRepository, suppressionsRepository);
    }
}
