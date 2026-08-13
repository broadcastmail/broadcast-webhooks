package com.broadcastmail.webhooks.stripe;

import com.broadcastmail.common.account.AccountRepository;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookService {
    private final AccountRepository accountRepository;

    @Transactional
    public void process(Event event) {
        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event);
            case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }
    }

    private void handleCheckoutCompleted(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Optional<StripeObject> stripeObject = deserializer.getObject();

        if (stripeObject.isEmpty() || !(stripeObject.get() instanceof Session session)) {
            log.error("Could not deserialize session from event {}", event.getId());
            return;
        }
        String accountId = session.getMetadata().get("accountId");
        String plan = session.getMetadata().get("plan");
        String customerId = session.getCustomer();

        if (accountId == null || plan == null) {
            log.error("Missing metadata in checkout session {} - accountId: {}, plan: {}",session.getId(), accountId, plan);
            return;
        }
        accountRepository.findById(UUID.fromString(accountId))
                .ifPresentOrElse(
                        account -> {
                            account.setPlan(plan);
                            account.setStripeCustomerId(customerId);
                            accountRepository.save(account);
                            log.info("Account {} upgraded to plan {}", accountId, plan);
                        },
                        () -> log.error("Account not found for id {}", accountId)
                );
    }

    private void handleSubscriptionDeleted(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Optional<StripeObject> stripeObject = deserializer.getObject();

        if (stripeObject.isEmpty() || !(stripeObject.get() instanceof Subscription subscription)) {
            log.error("Could not deserialize subscription from event {}", event.getId());
            return;
        }

        String customerId = subscription.getCustomer();

        accountRepository.findByStripeCustomerId(customerId).ifPresentOrElse(
                account -> {
                    account.setPlan("free");
                    accountRepository.save(account);
                    log.info("Account {} downgraded to free", account.getId());
                },
                () -> log.error("No account found for Stripe customer {}", customerId)
        );
    }
}
