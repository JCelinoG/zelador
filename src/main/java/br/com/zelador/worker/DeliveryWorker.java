package br.com.zelador.worker;

import br.com.zelador.domain.Attempt;
import br.com.zelador.domain.Delivery;
import br.com.zelador.infra.repository.AttemptRepository;
import br.com.zelador.infra.repository.DeliveryRepository;
import br.com.zelador.queue.DeliveryPublisher;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class DeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(DeliveryWorker.class);
    private static final String GROUP = "zelador-workers";
    private static final String CONSUMER = "worker-" + UUID.randomUUID();

    private final StringRedisTemplate redis;
    private final DeliveryRepository deliveryRepository;
    private final AttemptRepository attemptRepository;
    private final HttpDeliveryClient httpClient;
    private final BackoffPolicy backoffPolicy;

    public DeliveryWorker(StringRedisTemplate redis,
                          DeliveryRepository deliveryRepository,
                          AttemptRepository attemptRepository,
                          HttpDeliveryClient httpClient,
                        BackoffPolicy backoffPolicy) {
        this.redis = redis;
        this.deliveryRepository = deliveryRepository;
        this.attemptRepository = attemptRepository;
        this.httpClient = httpClient;
        this.backoffPolicy = backoffPolicy;
    }

    @PostConstruct
    public void iniciar() {
        garantirGrupo();
        Thread t = new Thread(this::loop, "zelador-worker");
        t.setDaemon(true);
        t.start();
    }

    private void garantirGrupo() {
        try {
            redis.opsForStream().createGroup(DeliveryPublisher.STREAM_KEY, GROUP);
        } catch (Exception e) {
            log.info("Consumer group já existe: {}", GROUP);
        }
    }

    private void loop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
                        Consumer.from(GROUP, CONSUMER),
                        StreamReadOptions.empty().count(10).block(Duration.ofSeconds(2)),
                        StreamOffset.create(DeliveryPublisher.STREAM_KEY, ReadOffset.lastConsumed())
                );

                if (records == null) continue;

                for (MapRecord<String, Object, Object> record : records) {
                    processar(record);
                }
            } catch (Exception e) {
                log.error("Erro no loop do worker", e);
            }
        }
    }

private void processar(MapRecord<String, Object, Object> record) {
    UUID deliveryId = UUID.fromString((String) record.getValue().get("deliveryId"));

    Delivery delivery = deliveryRepository.findById(deliveryId).orElse(null);
    if (delivery == null) {
        log.warn("Delivery não encontrada: {}", deliveryId);
        ack(record);
        return;
    }

    delivery.marcarEmProcessamento();
    deliveryRepository.save(delivery);

    long inicio = System.currentTimeMillis();
    try {
        int statusCode = httpClient.entregar(delivery);
        int latencia = (int) (System.currentTimeMillis() - inicio);

        delivery.incrementarTentativa();

        if (statusCode >= 200 && statusCode < 300) {
            delivery.marcarEntregue();
            attemptRepository.save(Attempt.sucesso(delivery.getId(), delivery.getAttemptsCount(), statusCode, latencia));
            log.info("Delivery {} entregue com {}", deliveryId, statusCode);
        } else {
            agendarRetry(delivery, statusCode, "HTTP " + statusCode, latencia);
            log.warn("Delivery {} falhou com {} — retry agendado", deliveryId, statusCode);
        }
    } catch (Exception e) {
        int latencia = (int) (System.currentTimeMillis() - inicio);
        delivery.incrementarTentativa();
        agendarRetry(delivery, null, e.getMessage(), latencia);
        log.error("Erro ao entregar {} — retry agendado", deliveryId, e);
    }

    deliveryRepository.save(delivery);
    ack(record);
}

private void agendarRetry(Delivery delivery, Integer statusCode, String erro, int latencia) {
    attemptRepository.save(Attempt.falha(
            delivery.getId(),
            delivery.getAttemptsCount(),
            statusCode,
            erro,
            latencia
    ));

    // Ainda tem tentativa? Agenda retry. Esgotou? Vai pra DLQ.
    // (max_attempts vem do webhook; por enquanto hardcoded em 5)
    int maxAttempts = 5;
    if (delivery.getAttemptsCount() >= maxAttempts) {
        delivery.marcarDeadLetter();
        log.warn("Delivery {} esgotou tentativas — movida pra DEAD_LETTER", delivery.getId());
    } else {
        Instant proxima = backoffPolicy.proximaTentativa(delivery.getAttemptsCount());
        delivery.marcarAguardandoRetry(proxima);
    }
}        UUID deliveryId = UUID.fromString((String) record.getValue().get("deliveryId"));

        Delivery delivery = deliveryRepository.findById(deliveryId).orElse(null);
        if (delivery == null) {
            log.warn("Delivery não encontrada: {}", deliveryId);
            ack(record);
            return;
        }

        delivery.marcarEmProcessamento();
        deliveryRepository.save(delivery);

        long inicio = System.currentTimeMillis();
        try {
            int statusCode = httpClient.entregar(delivery);
            int latencia = (int) (System.currentTimeMillis() - inicio);

            if (statusCode >= 200 && statusCode < 300) {
                delivery.marcarEntregue();
                delivery.incrementarTentativa();
                attemptRepository.save(Attempt.sucesso(delivery.getId(), delivery.getAttemptsCount(), statusCode, latencia));
                log.info("Delivery {} entregue com {}", deliveryId, statusCode);
            } else {
                delivery.incrementarTentativa();
                attemptRepository.save(Attempt.falha(delivery.getId(), delivery.getAttemptsCount(), statusCode, "HTTP " + statusCode, latencia));
                log.warn("Delivery {} falhou com {}", deliveryId, statusCode);
            }
        } catch (Exception e) {
            int latencia = (int) (System.currentTimeMillis() - inicio);
            delivery.incrementarTentativa();
            attemptRepository.save(Attempt.falha(delivery.getId(), delivery.getAttemptsCount(), null, e.getMessage(), latencia));
            log.error("Erro ao entregar {}", deliveryId, e);
        }

        deliveryRepository.save(delivery);
        ack(record);
    }

    private void ack(MapRecord<String, Object, Object> record) {
        redis.opsForStream().acknowledge(DeliveryPublisher.STREAM_KEY, GROUP, record.getId());
    }
}