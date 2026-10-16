package br.com.zelador.infra.repository;

import br.com.zelador.domain.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebhookRepository extends JpaRepository<Webhook, UUID> {
}