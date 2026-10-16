package br.com.zelador.ingestion;

import br.com.zelador.domain.Delivery;
import br.com.zelador.ingestion.dto.NotaRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/webhooks")
public class IngestionController {

    private final IngestionService service;

    public IngestionController(IngestionService service) {
        this.service = service;
    }

    @PostMapping("/{webhookId}/nota")
    public ResponseEntity<?> receberNota(
            @PathVariable UUID webhookId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody NotaRequest request) {

        Delivery delivery = service.ingerir(webhookId, idempotencyKey, request);

        return ResponseEntity.accepted().body(Map.of(
                "deliveryId", delivery.getId(),
                "status", delivery.getStatus(),
                "attemptsCount", delivery.getAttemptsCount()
        ));
    }
}