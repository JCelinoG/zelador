package br.com.zelador.ingestion;

import br.com.zelador.domain.Delivery;
import br.com.zelador.domain.Webhook;
import br.com.zelador.infra.repository.DeliveryRepository;
import br.com.zelador.infra.repository.WebhookRepository;
import br.com.zelador.ingestion.dto.NotaRequest;
import br.com.zelador.queue.DeliveryPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class IngestionService {

    private final WebhookRepository webhookRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryPublisher publisher;

    public IngestionService(WebhookRepository webhookRepository,
                            DeliveryRepository deliveryRepository,
                            DeliveryPublisher publisher) {
        this.webhookRepository = webhookRepository;
        this.deliveryRepository = deliveryRepository;
        this.publisher = publisher;
    }

    @Transactional
    public Delivery ingerir(UUID webhookId, String idempotencyKey, NotaRequest request) {
        Webhook webhook = webhookRepository.findById(webhookId)
                .orElseThrow(() -> new IllegalArgumentException("Webhook não encontrado: " + webhookId));

        if (!webhook.isActive()) {
            throw new IllegalStateException("Webhook inativo: " + webhookId);
        }

        // Fast path: já existe?
        Optional<Delivery> existente = deliveryRepository
                .findByWebhookIdAndIdempotencyKey(webhookId, idempotencyKey);
        if (existente.isPresent()) {
            return existente.get();
        }

        // Slow path: tenta inserir. Se perder a corrida, alguém inseriu antes.
        Delivery nova = new Delivery(webhook.getTenantId(), webhookId, idempotencyKey, request.toJson());
        try {
            Delivery salva = deliveryRepository.saveAndFlush(nova);
            publisher.publicar(salva);
            return salva;
        } catch (DataIntegrityViolationException e) {
            // Outro request inseriu a mesma delivery entre o SELECT e o INSERT.
            return deliveryRepository
                    .findByWebhookIdAndIdempotencyKey(webhookId, idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException(
                            "Conflito de idempotência mas delivery não encontrada", e));
        }
    }
}