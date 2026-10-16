package br.com.zelador.worker;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class BackoffPolicy {

    private static final Duration BASE = Duration.ofSeconds(2);
    private static final Duration CAP = Duration.ofHours(1);

    /**
     * Backoff exponencial com jitter.
     *
     * formula: min(CAP, BASE * 2^attempt) * random(0.5, 1.5)
     *
     * O jitter evita que várias entregas que falharam ao mesmo tempo
     * voltem a tentar no mesmo instante (thundering herd).
     */
    public Instant proximaTentativa(int attemptAtual) {
        long baseMs = BASE.toMillis();
        long capMs = CAP.toMillis();

        // 2^attempt, com teto pra não estourar long
        long exponencial = baseMs * (1L << Math.min(attemptAtual, 20));

        // aplica o teto
        long comTeto = Math.min(exponencial, capMs);

        // jitter: 0.5x até 1.5x
        double fator = ThreadLocalRandom.current().nextDouble(0.5, 1.5);
        long comJitter = (long) (comTeto * fator);

        return Instant.now().plusMillis(comJitter);
    }
}