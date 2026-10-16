package br.com.zelador.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deliveries")
public class Delivery {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "webhook_id", nullable = false)
    private UUID webhookId;

    @Column(name = "idempotency_key", nullable = false, length = 200)
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeliveryStatus status;

    @Column(name = "attempts_count", nullable = false)
    private int attemptsCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Delivery() {}

    public Delivery(UUID tenantId, UUID webhookId, String idempotencyKey, String payload) {
        this.tenantId = tenantId;
        this.webhookId = webhookId;
        this.idempotencyKey = idempotencyKey;
        this.payload = payload;
        this.status = DeliveryStatus.PENDENTE;
        this.attemptsCount = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void marcarEmProcessamento() {
        this.status = DeliveryStatus.EM_PROCESSAMENTO;
        this.updatedAt = Instant.now();
    }

    public void marcarEntregue() {
        this.status = DeliveryStatus.ENTREGUE;
        this.updatedAt = Instant.now();
    }

    public void incrementarTentativa() {
        this.attemptsCount++;
        this.updatedAt = Instant.now();
    }

    public void marcarAguardandoRetry(Instant nextAttemptAt) {
    this.status = DeliveryStatus.AGUARDANDO_RETRY;
    this.nextAttemptAt = nextAttemptAt;
    this.updatedAt = Instant.now();
    }

    public void marcarDeadLetter() {
        this.status = DeliveryStatus.DEAD_LETTER;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getWebhookId() { return webhookId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getPayload() { return payload; }
    public DeliveryStatus getStatus() { return status; }
    public int getAttemptsCount() { return attemptsCount; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
}