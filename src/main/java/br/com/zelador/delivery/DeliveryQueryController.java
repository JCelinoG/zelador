package br.com.zelador.delivery;

import br.com.zelador.domain.Attempt;
import br.com.zelador.domain.Delivery;
import br.com.zelador.infra.repository.AttemptRepository;
import br.com.zelador.infra.repository.DeliveryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/deliveries")
public class DeliveryQueryController {

    private final DeliveryRepository deliveryRepository;
    private final AttemptRepository attemptRepository;

    public DeliveryQueryController(DeliveryRepository deliveryRepository,
                                   AttemptRepository attemptRepository) {
        this.deliveryRepository = deliveryRepository;
        this.attemptRepository = attemptRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detalhe(@PathVariable UUID id) {
        Delivery d = deliveryRepository.findById(id).orElse(null);
        if (d == null) return ResponseEntity.notFound().build();

        List<Attempt> attempts = attemptRepository.findByDeliveryIdOrderByAttemptNumberAsc(id);

        return ResponseEntity.ok(Map.of(
                "id", d.getId(),
                "status", d.getStatus(),
                "attemptsCount", d.getAttemptsCount(),
                "nextAttemptAt", d.getNextAttemptAt() != null ? d.getNextAttemptAt() : "",
                "idempotencyKey", d.getIdempotencyKey(),
                "attempts", attempts.stream().map(a -> Map.of(
                        "number", a.getAttemptNumber(),
                        "statusCode", a.getStatusCode() != null ? a.getStatusCode() : 0,
                        "error", a.getErrorMessage() != null ? a.getErrorMessage() : "",
                        "latencyMs", a.getLatencyMs() != null ? a.getLatencyMs() : 0,
                        "at", a.getAttemptedAt()
                )).toList()
        ));
    }
}