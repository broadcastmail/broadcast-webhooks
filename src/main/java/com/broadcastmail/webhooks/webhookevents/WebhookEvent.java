package com.broadcastmail.webhooks.webhookevents;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name="webhook_events")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "provider_event_id", nullable = false)
    private String providerEventId;

    @Column(name = "campaign_recipient_id")
    private UUID campaignRecipient;

    @NotNull
    @Column(name = "event_type", nullable = false)
    private String eventType;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rawPayload;

    @Setter
    @Builder.Default
    @Column(name = "processed", nullable = false)
    private boolean processed = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

}
