package br.com.atendimento.dto;

import br.com.atendimento.entity.AvaliacaoExecucao;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;

import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AvaliacaoResponse(Long id, AvaliacaoExecucao.Status status, String modelo, String versaoPrompt,
                                String dataset, Integer total, BigDecimal acuraciaCategoria,
                                BigDecimal acuraciaPrioridade, BigDecimal acuraciaAssunto,
                                Integer respostasInvalidas, String erro, Instant iniciadoEm, Instant concluidoEm,
                                @JsonRawValue String metricas) {

    public static AvaliacaoResponse resumo(AvaliacaoExecucao e) {
        return de(e, null);
    }

    public static AvaliacaoResponse completo(AvaliacaoExecucao e) {
        return de(e, e.getResultadoJson());
    }

    private static AvaliacaoResponse de(AvaliacaoExecucao e, String metricas) {
        return new AvaliacaoResponse(e.getId(), e.getStatus(), e.getModelo(), e.getVersaoPrompt(), e.getDataset(),
                e.getTotal(), e.getAcuraciaCategoria(), e.getAcuraciaPrioridade(), e.getAcuraciaAssunto(),
                e.getRespostasInvalidas(), e.getErro(), e.getIniciadoEm(), e.getConcluidoEm(), metricas);
    }
}
