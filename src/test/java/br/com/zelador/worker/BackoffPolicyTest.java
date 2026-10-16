package br.com.zelador.worker;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class BackoffPolicyTest {

    private final BackoffPolicy policy = new BackoffPolicy();

    @Test
    void primeiraTentativaFicaEntre1e3Segundos() {
        Instant agora = Instant.now();
        Instant proxima = policy.proximaTentativa(0);

        long segundos = ChronoUnit.SECONDS.between(agora, proxima);
        assertThat(segundos).isBetween(1L, 3L);
    }

    @Test
    void tentativasCrescemExponencialmente() {
        Instant t0 = policy.proximaTentativa(0);
        Instant t5 = policy.proximaTentativa(5);
        Instant t10 = policy.proximaTentativa(10);

        assertThat(t5).isAfter(t0);
        assertThat(t10).isAfter(t5);
    }

    @Test
    void respeitaTetoDe1Hora() {
        Instant agora = Instant.now();
        Instant proxima = policy.proximaTentativa(50); // tentativa absurda

        long minutos = ChronoUnit.MINUTES.between(agora, proxima);
        assertThat(minutos).isLessThanOrEqualTo(90L); // 1h + jitter
    }
}