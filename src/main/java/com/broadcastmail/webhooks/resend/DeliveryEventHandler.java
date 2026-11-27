package com.broadcastmail.webhooks.resend;

import com.broadcastmail.common.campaign.CampaignRepository;
import com.broadcastmail.common.campaign.recipient.CampaignRecipient;
import com.broadcastmail.common.campaign.recipient.CampaignRecipientRepository;
import com.broadcastmail.common.suppressions.SuppressionsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Applies Resend delivery events to the recipient they belong to.
 * <p>
 * An event whose {@code email_id} matches no recipient is ignored (logged, not an error): test sends are never in
 * {@code campaign_recipients}, and retention deletes recipients long before late events such as opens stop arriving.
 * Failing would make Resend retry the event for about a day.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryEventHandler {

    private final CampaignRecipientRepository campaignRecipientRepository;
    private final CampaignRepository campaignRepository;
    private final SuppressionsRepository suppressionsRepository;

    @Transactional
    public void handleDelivered(String emailId) {
        findRecipient(emailId, "email.delivered").ifPresent(recipient -> {
            campaignRecipientRepository.markDelivered(emailId);
            campaignRepository.incrementDeliveredCount(recipient.getCampaignId());
        });
    }

    @Transactional
    public void handleOpened(String emailId) {
        findRecipient(emailId, "email.opened").ifPresent(recipient -> {
            campaignRecipientRepository.markOpened(emailId);
            campaignRepository.incrementOpenedCount(recipient.getCampaignId());
        });
    }

    @Transactional
    public void handleSoftBounced(String emailId) {
        findRecipient(emailId, "email.bounced").ifPresent(recipient -> {
            campaignRecipientRepository.markBounced(emailId);
            campaignRepository.incrementBouncedCount(recipient.getCampaignId());
        });
    }

    @Transactional
    public void handleHardBounced(UUID accountId, String emailId, List<String> to) {
        findRecipient(emailId, "email.bounced").ifPresent(recipient -> {
            campaignRecipientRepository.markBounced(emailId);
            campaignRepository.incrementBouncedCount(recipient.getCampaignId());
        });
        suppress(accountId, to, "BOUNCE");
    }

    @Transactional
    public void handleComplained(UUID accountId, List<String> to) {
        suppress(accountId, to, "COMPLAINT");
    }

    // Suppresses by the address Resend reports, not the recipient row, so it works after retention deleted the row.
    private void suppress(UUID accountId, List<String> to, String reason) {
        if (to == null || to.isEmpty()) {
            log.warn("Cannot suppress for {}: event has no recipient address", reason);
            return;
        }
        to.forEach(email -> suppressionsRepository.insertWithReason(accountId, email, reason));
    }

    @Transactional
    public void handleFailed(String emailId, String reason) {
        findRecipient(emailId, "email.failed").ifPresent(recipient -> {
            campaignRecipientRepository.markFailed(emailId, reason);
            campaignRepository.incrementFailedCount(recipient.getCampaignId());
        });
    }

    private Optional<CampaignRecipient> findRecipient(String emailId, String eventType) {
        Optional<CampaignRecipient> recipient = campaignRecipientRepository.findByResendMessageId(emailId);
        if (recipient.isEmpty()) {
            log.info("Ignoring {} for email {}: no recipient (test send, or deleted by retention)", eventType, emailId);
        }
        return recipient;
    }
}
