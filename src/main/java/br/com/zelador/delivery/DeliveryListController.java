package br.com.zelador.delivery;

import br.com.zelador.domain.Delivery;
import br.com.zelador.domain.DeliveryStatus;
import br.com.zelador.infra.repository.DeliveryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/deliveries")
public class DeliveryListController {

    private final DeliveryRepository deliveryRepository;

    public DeliveryListController(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false) DeliveryStatus status,
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Delivery> resultado = deliveryRepository.listar(
                status, tenantId, desde, PageRequest.of(page, Math.min(size, 100))
        );

        return ResponseEntity.ok(Map.of(
                "content", resultado.getContent().stream().map(this::resumir).toList(),
                "page", resultado.getNumber(),
                "size", resultado.getSize(),
                "totalElements", resultado.getTotalElements(),
                "totalPages", resultado.getTotalPages()
        ));
    }

    private Map<String, Object> resumir(Delivery d) {
        return Map.of(
                "id", d.getId(),
                "status", d.getStatus(),
                "attemptsCount", d.getAttemptsCount(),
                "idempotencyKey", d.getIdempotencyKey(),
                "tenantId", d.getTenantId(),
                "createdAt", d.getCreatedAt(),
                "nextAttemptAt", d.getNextAttemptAt() != null ? d.getNextAttemptAt() : ""
        );
    }
}