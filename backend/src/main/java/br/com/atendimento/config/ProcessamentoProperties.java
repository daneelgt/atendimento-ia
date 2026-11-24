package br.com.atendimento.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties("app.processamento")
public record ProcessamentoProperties(boolean agendadorHabilitado, long intervaloMs, int tamanhoLote,
                                      int maxTentativas, long backoffInicialSegundos, long backoffMaximoSegundos,
                                      BigDecimal confiancaMinima, long travadoAposMinutos) {
}
