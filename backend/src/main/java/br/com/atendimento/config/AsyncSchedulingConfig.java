package br.com.atendimento.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

public class AsyncSchedulingConfig {

    /** Desligável por configuração: nos testes o ciclo do worker é disparado manualmente. */
    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(name = "app.processamento.agendador-habilitado", havingValue = "true", matchIfMissing = true)
    static class Scheduling {
    }

    @Configuration
    @EnableAsync
    static class Async {
    }
}
