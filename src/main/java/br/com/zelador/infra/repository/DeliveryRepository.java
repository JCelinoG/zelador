package br.com.zelador.infra.repository;

import br.com.zelador.domain.Delivery;
import br.com.zelador.domain.DeliveryStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    Optional<Delivery> findByWebhookIdAndIdempotencyKey(UUID webhookId, String idempotencyKey);

    @Query("""
            SELECT d FROM Delivery d
            WHERE d.status = :status
              AND d.nextAttemptAt <= :agora
            ORDER BY d.nextAttemptAt
            """)
    List<Delivery> buscarParaRetry(@Param("status") DeliveryStatus status,
                                   @Param("agora") Instant agora,
                                   Pageable pageable);

    default List<Delivery> buscarParaRetry(DeliveryStatus status, Instant agora, int limit) {
        return buscarParaRetry(status, agora, PageRequest.of(0, limit));
    }
}