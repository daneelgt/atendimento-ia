package br.com.atendimento.dto;

import br.com.atendimento.entity.Atendimento;
import br.com.atendimento.entity.StatusAtendimento;

import java.time.Instant;

public record MensagemRecebidaResponse(Long id, StatusAtendimento status, Instant criadoEm) {

    public static MensagemRecebidaResponse de(Atendimento a) {
        return new MensagemRecebidaResponse(a.getId(), a.getStatus(), a.getCriadoEm());
    }
}
