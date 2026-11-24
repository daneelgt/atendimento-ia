package br.com.atendimento.avaliacao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record MetricasAvaliacao(
        int total,
        int invalidas,
        BigDecimal taxaAcerto,
        BigDecimal taxaErro,
        BigDecimal acuraciaCategoria,
        BigDecimal acuraciaPrioridade,
        BigDecimal acuraciaAssunto,
        Map<String, MetricaCategoria> porCategoria,
        /** linha = categoria esperada, coluna = categoria retornada (ou INVALIDA). */
        Map<String, Map<String, Integer>> matrizConfusao,
        List<CasoDetalhe> casos) {

    public record MetricaCategoria(int esperados, int previstos, int acertos,
                                   BigDecimal precisao, BigDecimal recall) {
    }

    public record CasoDetalhe(String mensagem, String categoriaEsperada, String categoriaRetornada,
                              String prioridadeEsperada, String prioridadeRetornada,
                              String assuntoEsperado, String assuntoRetornado,
                              boolean acertoCompleto, String erro) {
    }
}
