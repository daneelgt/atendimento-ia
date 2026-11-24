package br.com.atendimento.service;

import br.com.atendimento.config.ProcessamentoProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Backoff exponencial com teto: 30s, 60s, 120s, 240s, 480s, 600s, 600s... */
@Component
public class BackoffCalculator {

    private final ProcessamentoProperties props;

    public BackoffCalculator(ProcessamentoProperties props) {
        this.props = props;
    }

    public Duration atraso(int tentativa) {
        int expoente = Math.min(Math.max(tentativa - 1, 0), 20);
        long segundos = props.backoffInicialSegundos() * (1L << expoente);
        return Duration.ofSeconds(Math.min(segundos, props.backoffMaximoSegundos()));
    }
}
