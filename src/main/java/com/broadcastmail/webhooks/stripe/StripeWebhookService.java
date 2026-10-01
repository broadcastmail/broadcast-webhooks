package com.broadcastmail.webhooks.stripe;

import com.broadcastmail.common.account.AccountRepository;
import com.broadcastmail.common.account.plan.Plan;
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
            case "customer.subscription.updated" -> handleSubscriptionUpdated(event);
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
        String customerId = session.getCustomer();

        if (accountId == null) {
            log.error("Missing accountId metadata in checkout session {}", session.getId());
            return;
        }

        accountRepository.findById(UUID.fromString(accountId))
                .ifPresentOrElse(
                        account -> {
                            account.setPlan(Plan.PRO);
                            account.setStripeCustomerId(customerId);
                            account.setStripeSubscriptionStatus("active");
                            accountRepository.save(account);
                            log.info("Account {} upgraded to PRO", accountId);
                        },
                        () -> log.error("Account not found for id {}", accountId)
                );
    }

    private void handleSubscriptionUpdated(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Optional<StripeObject> stripeObject = deserializer.getObject();

        if (stripeObject.isEmpty() || !(stripeObject.get() instanceof Subscription subscription)) {
            log.error("Could not deserialize subscription from event {}", event.getId());
            return;
        }

        String customerId = subscription.getCustomer();
        String status = subscription.getStatus();

        accountRepository.findByStripeCustomerId(customerId).ifPresentOrElse(
                account -> {
                    account.setStripeSubscriptionStatus(status);
                    accountRepository.save(account);
                    log.info("Account {} subscription status updated to {}", account.getId(), status);
                },
                () -> log.error("No account found for Stripe customer {}", customerId)
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
                    account.setPlan(Plan.FREE);
                    account.setStripeSubscriptionStatus("canceled");
                    accountRepository.save(account);
                    log.info("Account {} downgraded to FREE", account.getId());
                },
                () -> log.error("No account found for Stripe customer {}", customerId)
        );
    }
}
