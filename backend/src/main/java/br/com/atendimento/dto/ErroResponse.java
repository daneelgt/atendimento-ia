package br.com.atendimento.dto;

import java.time.Instant;
import java.util.Map;

public record ErroResponse(int status, String erro, String mensagem, Map<String, String> campos, Instant timestamp) {

    public static ErroResponse de(int status, String erro, String mensagem) {
        return new ErroResponse(status, erro, mensagem, Map.of(), Instant.now());
    }
}
