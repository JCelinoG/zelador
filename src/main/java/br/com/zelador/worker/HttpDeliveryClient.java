package br.com.zelador.worker;

import br.com.zelador.domain.Delivery;
import br.com.zelador.domain.Webhook;
import br.com.zelador.infra.repository.WebhookRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpDeliveryClient {

    private final WebhookRepository webhookRepository;
    private final RestClient restClient = RestClient.create();

    public HttpDeliveryClient(WebhookRepository webhookRepository) {
        this.webhookRepository = webhookRepository;
    }

    public int entregar(Delivery delivery) {
        Webhook webhook = webhookRepository.findById(delivery.getWebhookId())
                .orElseThrow(() -> new IllegalStateException("Webhook não encontrado"));

        return restClient.post()
                .uri(webhook.getUrl())
                .header("Content-Type", "application/json")
                .header("X-Zelador-Delivery", delivery.getId().toString())
                .body(delivery.getPayload())
                .exchange((req, res) -> res.getStatusCode().value());
    }
}