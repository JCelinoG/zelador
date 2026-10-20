package br.com.zelador.dlq;

import br.com.zelador.domain.DeadLetter;
import br.com.zelador.infra.repository.DeadLetterRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/dlq")
public class DlqController {

    private final DeadLetterRepository deadLetterRepository;

    public DlqController(DeadLetterRepository deadLetterRepository) {
        this.deadLetterRepository = deadLetterRepository;
    }

    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<DeadLetter> resultado = deadLetterRepository
                .findByReplayedAtIsNullOrderByMovedAtDesc(
                        PageRequest.of(page, Math.min(size, 100))
                );

        return ResponseEntity.ok(Map.of(
                "content", resultado.getContent().stream().map(this::resumir).toList(),
                "page", resultado.getNumber(),
                "size", resultado.getSize(),
                "totalElements", resultado.getTotalElements(),
                "totalPages", resultado.getTotalPages()
        ));
    }

    @GetMapping("/count")
    public ResponseEntity<?> count() {
        return ResponseEntity.ok(Map.of(
                "count", deadLetterRepository.countByReplayedAtIsNull()
        ));
    }

    private Map<String, Object> resumir(DeadLetter dl) {
        return Map.of(
                "id", dl.getId(),
                "deliveryId", dl.getDeliveryId(),
                "reason", dl.getReason(),
                "movedAt", dl.getMovedAt()
        );
    }
}