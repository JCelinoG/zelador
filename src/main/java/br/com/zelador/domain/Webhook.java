package br.com.zelador.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhooks")
public class Webhook {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false, length = 200)
    private String secret;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 5;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Webhook() {}

    public Webhook(UUID tenantId, String url, String secret) {
        this.tenantId = tenantId;
        this.url = url;
        this.secret = secret;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getUrl() { return url; }
    public String getSecret() { return secret; }
    public int getMaxAttempts() { return maxAttempts; }
    public boolean isActive() { return active; }
}