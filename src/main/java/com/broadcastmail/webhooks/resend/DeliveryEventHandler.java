package com.broadcastmail.webhooks.resend;

import com.broadcastmail.common.campaign.CampaignRepository;
import com.broadcastmail.common.campaign.recipient.CampaignRecipient;
import com.broadcastmail.common.campaign.recipient.CampaignRecipientRepository;
import com.broadcastmail.common.suppressions.SuppressionsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryEventHandler {

    private final CampaignRecipientRepository campaignRecipientRepository;
    private final CampaignRepository campaignRepository;
    private final SuppressionsRepository suppressionsRepository;

    @Transactional
    public void handleDelivered(String emailId) {
        UUID campaignId = getCampaignId(emailId);
        campaignRecipientRepository.markDelivered(emailId);
        campaignRepository.incrementDeliveredCount(campaignId);
    }

    @Transactional
    public void handleOpened(String emailId) {
        UUID campaignId = getCampaignId(emailId);
        campaignRecipientRepository.markOpened(emailId);
        campaignRepository.incrementOpenedCount(campaignId);
    }

    @Transactional
    public void handleSoftBounced(String emailId) {
        UUID campaignId = getCampaignId(emailId);
        campaignRecipientRepository.markBounced(emailId);
        campaignRepository.incrementBouncedCount(campaignId);
    }

    @Transactional
    public void handleHardBounced(String emailId) {
        CampaignRecipient recipient = getCampaignRecipient(emailId);
        campaignRecipientRepository.markBounced(emailId);
        campaignRepository.incrementBouncedCount(recipient.getCampaignId());
        suppressionsRepository.insertFromRecipientWithReason(recipient.getId(), "BOUNCE");
    }

    @Transactional
    public void handleFailed(String emailId, String reason) {
        UUID campaignId = getCampaignId(emailId);
        campaignRecipientRepository.markFailed(emailId, reason);
        campaignRepository.incrementFailedCount(campaignId);
    }

    private UUID getCampaignId(String emailId) {
        return campaignRecipientRepository.findByResendMessageId(emailId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient not found"))
                .getCampaignId();
    }

    private CampaignRecipient getCampaignRecipient(String emailId) {
        return campaignRecipientRepository.findByResendMessageId(emailId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient not found"));
    }
}
