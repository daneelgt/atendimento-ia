package br.com.atendimento.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.ia")
public record IaProperties(String provedor, String url, String modelo, String apiKey,
                           int timeoutSegundos, String versaoPrompt) {
}
