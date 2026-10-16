package br.com.zelador.worker;

import br.com.zelador.domain.Delivery;
import br.com.zelador.domain.DeliveryStatus;
import br.com.zelador.infra.repository.DeliveryRepository;
import br.com.zelador.queue.DeliveryPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class RetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetryScheduler.class);

    private final DeliveryRepository deliveryRepository;
    private final DeliveryPublisher publisher;

    public RetryScheduler(DeliveryRepository deliveryRepository, DeliveryPublisher publisher) {
        this.deliveryRepository = deliveryRepository;
        this.publisher = publisher;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void reprocessar() {
        Instant agora = Instant.now();
      List<Delivery> pendentes = deliveryRepository.buscarParaRetry(
        DeliveryStatus.AGUARDANDO_RETRY,
        agora,
        100
);

        if (pendentes.isEmpty()) return;

        log.info("Reprocessando {} deliveries aguardando retry", pendentes.size());

        for (Delivery d : pendentes) {
            // Marca como PENDENTE antes de publicar, pra não ser pega 2x
            // se o scheduler rodar de novo antes do worker consumir.
            d.marcarEmProcessamento();
            deliveryRepository.save(d);
            publisher.publicar(d);
        }
    }
}