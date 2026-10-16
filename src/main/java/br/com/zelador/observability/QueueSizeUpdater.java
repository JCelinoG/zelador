package br.com.zelador.observability;

import br.com.zelador.domain.DeliveryStatus;
import br.com.zelador.infra.repository.DeliveryRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class QueueSizeUpdater {

    private final DeliveryRepository deliveryRepository;
    private final ZeladorMetrics metrics;

    public QueueSizeUpdater(DeliveryRepository deliveryRepository, ZeladorMetrics metrics) {
        this.deliveryRepository = deliveryRepository;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelay = 15000)
    public void atualizar() {
        long pendente = deliveryRepository.countByStatus(DeliveryStatus.PENDENTE);
        long aguardandoRetry = deliveryRepository.countByStatus(DeliveryStatus.AGUARDANDO_RETRY);
        long deadLetter = deliveryRepository.countByStatus(DeliveryStatus.DEAD_LETTER);

        metrics.atualizarTamanhosFila(pendente, aguardandoRetry, deadLetter);
    }
}