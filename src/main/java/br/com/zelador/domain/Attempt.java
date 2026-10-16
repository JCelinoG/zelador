package br.com.zelador.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attempts")
public class Attempt {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "delivery_id", nullable = false)
    private UUID deliveryId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    protected Attempt() {}

    public static Attempt sucesso(UUID deliveryId, int attemptNumber, int statusCode, int latencyMs) {
        Attempt a = new Attempt();
        a.deliveryId = deliveryId;
        a.attemptNumber = attemptNumber;
        a.statusCode = statusCode;
        a.latencyMs = latencyMs;
        a.attemptedAt = Instant.now();
        return a;
    }

    public static Attempt falha(UUID deliveryId, int attemptNumber, Integer statusCode, String error, int latencyMs) {
        Attempt a = new Attempt();
        a.deliveryId = deliveryId;
        a.attemptNumber = attemptNumber;
        a.statusCode = statusCode;
        a.errorMessage = error;
        a.latencyMs = latencyMs;
        a.attemptedAt = Instant.now();
        return a;
    }

    public UUID getId() { return id; }
    public UUID getDeliveryId() { return deliveryId; }
    public int getAttemptNumber() { return attemptNumber; }
    public Integer getStatusCode() { return statusCode; }
    public String getErrorMessage() { return errorMessage; }
    public Integer getLatencyMs() { return latencyMs; }
    public Instant getAttemptedAt() { return attemptedAt; }
}