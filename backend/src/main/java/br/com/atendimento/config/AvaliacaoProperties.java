package br.com.atendimento.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.avaliacao")
public record AvaliacaoProperties(String dataset, long intervaloEntreChamadasMs) {
}
