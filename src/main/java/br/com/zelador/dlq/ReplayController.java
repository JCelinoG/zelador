package br.com.zelador.dlq;

import br.com.zelador.domain.Delivery;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/deliveries")
public class ReplayController {

    private final ReplayService service;

    public ReplayController(ReplayService service) {
        this.service = service;
    }

    @PostMapping("/{id}/replay")
    public ResponseEntity<?> replay(@PathVariable UUID id) {
        Delivery d = service.replay(id);
        return ResponseEntity.accepted().body(Map.of(
                "deliveryId", d.getId(),
                "status", d.getStatus(),
                "attemptsCount", d.getAttemptsCount()
        ));
    }
}