package com.broadcastmail.webhooks.webhookevents;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    boolean existsByProviderEventId(String providerEventId);
}