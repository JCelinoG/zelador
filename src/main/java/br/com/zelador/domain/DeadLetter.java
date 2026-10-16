package br.com.zelador.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dead_letters")
public class DeadLetter {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "delivery_id", nullable = false)
    private UUID deliveryId;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    @Column(name = "moved_at", nullable = false)
    private Instant movedAt;

    @Column(name = "replayed_at")
    private Instant replayedAt;

    protected DeadLetter() {}

    public static DeadLetter para(UUID deliveryId, String reason) {
        DeadLetter dl = new DeadLetter();
        dl.deliveryId = deliveryId;
        dl.reason = reason;
        dl.movedAt = Instant.now();
        return dl;
    }

    public void marcarReplay() {
        this.replayedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDeliveryId() { return deliveryId; }
    public String getReason() { return reason; }
    public Instant getMovedAt() { return movedAt; }
    public Instant getReplayedAt() { return replayedAt; }
}