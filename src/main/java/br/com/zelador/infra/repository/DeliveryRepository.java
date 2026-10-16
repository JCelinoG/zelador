package br.com.zelador.infra.repository;

import br.com.zelador.domain.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    Optional<Delivery> findByWebhookIdAndIdempotencyKey(UUID webhookId, String idempotencyKey);
}