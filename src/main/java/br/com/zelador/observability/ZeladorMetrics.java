package br.com.zelador.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ZeladorMetrics {

    private final Counter entregasSucesso;
    private final Counter entregasRetry;
    private final Counter entregasDeadLetter;
    private final Counter tentativasSucesso;
    private final Counter tentativasFalha;
    private final Timer latenciaEntrega;

    private final AtomicLong filaPendente = new AtomicLong();
    private final AtomicLong filaAguardandoRetry = new AtomicLong();
    private final AtomicLong filaDeadLetter = new AtomicLong();

    public ZeladorMetrics(MeterRegistry registry) {
        this.entregasSucesso = Counter.builder("zelador_deliveries_total")
                .tag("resultado", "sucesso")
                .description("Total de entregas concluídas com sucesso")
                .register(registry);

        this.entregasRetry = Counter.builder("zelador_deliveries_total")
                .tag("resultado", "retry")
                .description("Total de entregas que entraram em retry")
                .register(registry);

        this.entregasDeadLetter = Counter.builder("zelador_deliveries_total")
                .tag("resultado", "dead_letter")
                .description("Total de entregas movidas para dead-letter")
                .register(registry);

        this.tentativasSucesso = Counter.builder("zelador_attempts_total")
                .tag("resultado", "sucesso")
                .description("Total de tentativas de entrega com sucesso")
                .register(registry);

        this.tentativasFalha = Counter.builder("zelador_attempts_total")
                .tag("resultado", "falha")
                .description("Total de tentativas de entrega com falha")
                .register(registry);

        this.latenciaEntrega = Timer.builder("zelador_delivery_duration_seconds")
                .description("Latência das entregas HTTP")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);

        Gauge.builder("zelador_queue_size", filaPendente, AtomicLong::get)
                .tag("status", "pendente")
                .description("Deliveries no estado PENDENTE")
                .register(registry);

        Gauge.builder("zelador_queue_size", filaAguardandoRetry, AtomicLong::get)
                .tag("status", "aguardando_retry")
                .description("Deliveries no estado AGUARDANDO_RETRY")
                .register(registry);

        Gauge.builder("zelador_queue_size", filaDeadLetter, AtomicLong::get)
                .tag("status", "dead_letter")
                .description("Deliveries no estado DEAD_LETTER")
                .register(registry);
    }

    public void registrarSucesso(Duration latencia) {
        entregasSucesso.increment();
        tentativasSucesso.increment();
        latenciaEntrega.record(latencia);
    }

    public void registrarRetry(Duration latencia) {
        entregasRetry.increment();
        tentativasFalha.increment();
        latenciaEntrega.record(latencia);
    }

    public void registrarDeadLetter(Duration latencia) {
        entregasDeadLetter.increment();
        tentativasFalha.increment();
        latenciaEntrega.record(latencia);
    }

    public void atualizarTamanhosFila(long pendente, long aguardandoRetry, long deadLetter) {
        filaPendente.set(pendente);
        filaAguardandoRetry.set(aguardandoRetry);
        filaDeadLetter.set(deadLetter);
    }
}