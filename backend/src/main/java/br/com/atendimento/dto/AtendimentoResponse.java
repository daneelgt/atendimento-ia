package br.com.atendimento.dto;

import br.com.atendimento.entity.*;

import java.math.BigDecimal;
import java.time.Instant;

public record AtendimentoResponse(
        Long id,
        String clienteEmail,
        String mensagem,
        StatusAtendimento status,
        Categoria categoria,
        Prioridade prioridade,
        String assunto,
        Departamento departamento,
        BigDecimal confianca,
        String respostaSugerida,
        String respostaFinal,
        OrigemClassificacao origemClassificacao,
        int tentativas,
        String ultimoErro,
        Instant criadoEm,
        Instant atualizadoEm,
        Instant respondidoEm,
        long version) {

    public static AtendimentoResponse de(Atendimento a) {
        return new AtendimentoResponse(
                a.getId(), a.getClienteEmail(), a.getMensagem(), a.getStatus(),
                a.getCategoria(), a.getPrioridade(), a.getAssunto(), a.getDepartamento(),
                a.getConfianca(), a.getRespostaSugerida(), a.getRespostaFinal(),
                a.getOrigemClassificacao(), a.getTentativas(), a.getUltimoErro(),
                a.getCriadoEm(), a.getAtualizadoEm(), a.getRespondidoEm(), a.getVersion());
    }
}
