package com.broadcastmail.webhooks.stripe;

import com.broadcastmail.common.account.Account;
import com.broadcastmail.common.account.AccountRepository;
import com.broadcastmail.common.account.plan.Plan;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @InjectMocks
    private StripeWebhookService stripeWebhookService;

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();
    private static final String CUSTOMER_ID = "cus_test123";

    @Test
    void shouldUpgradePlanOnCheckoutCompleted() {
        // Given
        Account account = Account.builder()
                .id(UUID.fromString(ACCOUNT_ID))
                .plan(Plan.FREE)
                .build();
        when(accountRepository.findById(UUID.fromString(ACCOUNT_ID)))
                .thenReturn(Optional.of(account));

        Event event = mockCheckoutEvent(ACCOUNT_ID, Plan.PRO.name(), CUSTOMER_ID);

        // When
        stripeWebhookService.process(event);

        // Then
        assertThat(account.getPlan()).isEqualTo(Plan.PRO);
        assertThat(account.getStripeCustomerId()).isEqualTo(CUSTOMER_ID);
        verify(accountRepository).save(account);
    }

    @Test
    void shouldDowngradePlanOnSubscriptionDeleted() {
        // Given
        Account account = Account.builder()
                .id(UUID.fromString(ACCOUNT_ID))
                .plan(Plan.PRO)
                .stripeCustomerId(CUSTOMER_ID)
                .build();
        when(accountRepository.findByStripeCustomerId(CUSTOMER_ID))
                .thenReturn(Optional.of(account));

        Event event = mockSubscriptionEvent(CUSTOMER_ID);

        // When
        stripeWebhookService.process(event);

        // Then
        assertThat(account.getPlan()).isEqualTo(Plan.FREE);
        verify(accountRepository).save(account);
    }

    @Test
    void shouldLogErrorWhenAccountNotFoundOnCheckout() {
        // Given
        when(accountRepository.findById(any())).thenReturn(Optional.empty());
        Event event = mockCheckoutEvent(ACCOUNT_ID, Plan.PRO.name(), CUSTOMER_ID);

        // When — should not throw
        assertThatNoException().isThrownBy(() -> stripeWebhookService.process(event));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void shouldLogErrorWhenAccountNotFoundOnSubscriptionDeleted() {
        // Given
        when(accountRepository.findByStripeCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());
        Event event = mockSubscriptionEvent(CUSTOMER_ID);

        // When — should not throw
        assertThatNoException().isThrownBy(() -> stripeWebhookService.process(event));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void shouldIgnoreUnhandledEventTypes() {
        // Given
        Event event = mock(Event.class);
        when(event.getType()).thenReturn("payment_intent.created");

        // When
        assertThatNoException().isThrownBy(() -> stripeWebhookService.process(event));
        verifyNoInteractions(accountRepository);
    }

    private Event mockCheckoutEvent(String accountId, String plan, String customerId) {
        Session session = mock(Session.class);
        when(session.getMetadata()).thenReturn(Map.of("accountId", accountId, "plan", plan));
        when(session.getCustomer()).thenReturn(customerId);

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(session));

        Event event = mock(Event.class);
        when(event.getType()).thenReturn("checkout.session.completed");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        return event;
    }

    private Event mockSubscriptionEvent(String customerId) {
        Subscription subscription = mock(Subscription.class);
        when(subscription.getCustomer()).thenReturn(customerId);

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(subscription));

        Event event = mock(Event.class);
        when(event.getType()).thenReturn("customer.subscription.deleted");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        return event;
    }
}