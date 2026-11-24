package br.com.atendimento.service;

import br.com.atendimento.config.ProcessamentoProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BackoffCalculatorTest {

    @Test
    void cresceExponencialmenteAteOTeto() {
        BackoffCalculator calc = new BackoffCalculator(
                new ProcessamentoProperties(false, 1000, 10, 8, 30, 600, BigDecimal.ONE, 5));

        assertThat(calc.atraso(1)).hasSeconds(30);
        assertThat(calc.atraso(2)).hasSeconds(60);
        assertThat(calc.atraso(5)).hasSeconds(480);
        assertThat(calc.atraso(6)).hasSeconds(600);
        assertThat(calc.atraso(50)).hasSeconds(600);
    }
}
