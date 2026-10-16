package br.com.zelador.worker;

import br.com.zelador.domain.Attempt;
import br.com.zelador.domain.DeadLetter;
import br.com.zelador.domain.Delivery;
import br.com.zelador.infra.repository.AttemptRepository;
import br.com.zelador.infra.repository.DeadLetterRepository;
import br.com.zelador.infra.repository.DeliveryRepository;
import br.com.zelador.observability.ZeladorMetrics;
import br.com.zelador.queue.DeliveryPublisher;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
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
    private static final int MAX_ATTEMPTS = 5;

    private final StringRedisTemplate redis;
    private final DeliveryRepository deliveryRepository;
    private final AttemptRepository attemptRepository;
    private final DeadLetterRepository deadLetterRepository;
    private final HttpDeliveryClient httpClient;
    private final BackoffPolicy backoffPolicy;
    private final ZeladorMetrics metrics;

    public DeliveryWorker(StringRedisTemplate redis,
                          DeliveryRepository deliveryRepository,
                          AttemptRepository attemptRepository,
                          DeadLetterRepository deadLetterRepository,
                          HttpDeliveryClient httpClient,
                          BackoffPolicy backoffPolicy,
                          ZeladorMetrics metrics) {
        this.redis = redis;
        this.deliveryRepository = deliveryRepository;
        this.attemptRepository = attemptRepository;
        this.deadLetterRepository = deadLetterRepository;
        this.httpClient = httpClient;
        this.backoffPolicy = backoffPolicy;
        this.metrics = metrics;
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

    @SuppressWarnings("unchecked")
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
            Duration latencia = Duration.ofMillis(System.currentTimeMillis() - inicio);

            delivery.incrementarTentativa();

            if (statusCode >= 200 && statusCode < 300) {
                delivery.marcarEntregue();
                attemptRepository.save(Attempt.sucesso(
                        delivery.getId(),
                        delivery.getAttemptsCount(),
                        statusCode,
                        (int) latencia.toMillis()
                ));
                metrics.registrarSucesso(latencia);
                log.info("Delivery {} entregue com {}", deliveryId, statusCode);
            } else {
                agendarRetry(delivery, statusCode, "HTTP " + statusCode, latencia);
                log.warn("Delivery {} falhou com {} — retry agendado", deliveryId, statusCode);
            }
        } catch (Exception e) {
            Duration latencia = Duration.ofMillis(System.currentTimeMillis() - inicio);
            delivery.incrementarTentativa();
            agendarRetry(delivery, null, e.getMessage(), latencia);
            log.error("Erro ao entregar {} — retry agendado", deliveryId, e);
        }

        deliveryRepository.save(delivery);
        ack(record);
    }

    private void agendarRetry(Delivery delivery, Integer statusCode, String erro, Duration latencia) {
        attemptRepository.save(Attempt.falha(
                delivery.getId(),
                delivery.getAttemptsCount(),
                statusCode,
                erro,
                (int) latencia.toMillis()
        ));

        if (delivery.getAttemptsCount() >= MAX_ATTEMPTS) {
            delivery.marcarDeadLetter();
            deadLetterRepository.save(DeadLetter.para(
                    delivery.getId(),
                    "Esgotou " + MAX_ATTEMPTS + " tentativas. Último erro: " + erro
            ));
            metrics.registrarDeadLetter(latencia);
            log.warn("Delivery {} esgotou tentativas — movida pra DEAD_LETTER", delivery.getId());
        } else {
            Instant proxima = backoffPolicy.proximaTentativa(delivery.getAttemptsCount());
            delivery.marcarAguardandoRetry(proxima);
            metrics.registrarRetry(latencia);
        }
    }

    private void ack(MapRecord<String, Object, Object> record) {
        redis.opsForStream().acknowledge(DeliveryPublisher.STREAM_KEY, GROUP, record.getId());
    }
}