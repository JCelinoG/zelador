package br.com.zelador.dlq;

import br.com.zelador.domain.DeadLetter;
import br.com.zelador.domain.Delivery;
import br.com.zelador.infra.repository.DeadLetterRepository;
import br.com.zelador.infra.repository.DeliveryRepository;
import br.com.zelador.queue.DeliveryPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReplayService {

    private final DeliveryRepository deliveryRepository;
    private final DeadLetterRepository deadLetterRepository;
    private final DeliveryPublisher publisher;

    public ReplayService(DeliveryRepository deliveryRepository,
                         DeadLetterRepository deadLetterRepository,
                         DeliveryPublisher publisher) {
        this.deliveryRepository = deliveryRepository;
        this.deadLetterRepository = deadLetterRepository;
        this.publisher = publisher;
    }

    @Transactional
    public Delivery replay(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery não encontrada: " + deliveryId));

        DeadLetter dlq = deadLetterRepository
                .findByDeliveryIdAndReplayedAtIsNull(deliveryId)
                .orElseThrow(() -> new IllegalStateException("Delivery não está na DLQ"));

        delivery.reiniciarParaReplay();
        deliveryRepository.save(delivery);

        dlq.marcarReplay();
        deadLetterRepository.save(dlq);

        publisher.publicar(delivery);

        return delivery;
    }
}