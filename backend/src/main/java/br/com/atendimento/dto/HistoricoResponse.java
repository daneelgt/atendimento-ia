package br.com.atendimento.dto;

import br.com.atendimento.entity.HistoricoAtendimento;
import br.com.atendimento.entity.StatusAtendimento;

import java.time.Instant;

public record HistoricoResponse(StatusAtendimento statusAnterior, StatusAtendimento statusNovo,
                                String descricao, Instant criadoEm) {

    public static HistoricoResponse de(HistoricoAtendimento h) {
        return new HistoricoResponse(h.getStatusAnterior(), h.getStatusNovo(), h.getDescricao(), h.getCriadoEm());
    }
}
