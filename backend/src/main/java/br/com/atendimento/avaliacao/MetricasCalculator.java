package br.com.atendimento.avaliacao;

import br.com.atendimento.entity.Categoria;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Cálculo puro (sem Spring/IA) das métricas. Definições:
 * - taxaAcerto: % de casos com categoria, prioridade E assunto corretos; taxaErro = 1 - taxaAcerto.
 * - precisão(C) = acertos em C / vezes que a IA respondeu C; recall(C) = acertos em C / casos esperados C.
 * Respostas inválidas contam como erro em tudo e aparecem na coluna INVALIDA da matriz.
 */
public final class MetricasCalculator {

    public static final String INVALIDA = "INVALIDA";

    private MetricasCalculator() {
    }

    public static MetricasAvaliacao calcular(List<ResultadoCaso> resultados) {
        int total = resultados.size();
        int invalidas = (int) resultados.stream().filter(r -> !r.valido()).count();
        BigDecimal taxaAcerto = proporcao(resultados, r -> r.acertouCategoria() && r.acertouPrioridade() && r.acertouAssunto());

        Map<String, Map<String, Integer>> matriz = new LinkedHashMap<>();
        for (Categoria esperada : Categoria.values()) {
            Map<String, Integer> linha = new LinkedHashMap<>();
            for (Categoria retornada : Categoria.values()) {
                linha.put(retornada.name(), 0);
            }
            linha.put(INVALIDA, 0);
            matriz.put(esperada.name(), linha);
        }
        resultados.forEach(r -> matriz.get(r.caso().categoria().name())
                .merge(r.valido() ? r.classificacao().categoria().name() : INVALIDA, 1, Integer::sum));

        Map<String, MetricasAvaliacao.MetricaCategoria> porCategoria = new LinkedHashMap<>();
        for (Categoria c : Categoria.values()) {
            int esperados = (int) resultados.stream().filter(r -> r.caso().categoria() == c).count();
            int previstos = (int) resultados.stream().filter(r -> r.valido() && r.classificacao().categoria() == c).count();
            int acertos = matriz.get(c.name()).get(c.name());
            porCategoria.put(c.name(), new MetricasAvaliacao.MetricaCategoria(
                    esperados, previstos, acertos, divisao(acertos, previstos), divisao(acertos, esperados)));
        }

        List<MetricasAvaliacao.CasoDetalhe> casos = resultados.stream().map(r -> new MetricasAvaliacao.CasoDetalhe(
                r.caso().mensagem(),
                r.caso().categoria().name(), r.valido() ? r.classificacao().categoria().name() : null,
                r.caso().prioridade().name(), r.valido() ? r.classificacao().prioridade().name() : null,
                r.caso().assunto().name(), r.valido() ? r.classificacao().assunto().name() : null,
                r.acertouCategoria() && r.acertouPrioridade() && r.acertouAssunto(), r.erro())).toList();

        return new MetricasAvaliacao(total, invalidas, taxaAcerto, BigDecimal.ONE.subtract(taxaAcerto),
                proporcao(resultados, ResultadoCaso::acertouCategoria),
                proporcao(resultados, ResultadoCaso::acertouPrioridade),
                proporcao(resultados, ResultadoCaso::acertouAssunto),
                porCategoria, matriz, casos);
    }

    private static BigDecimal proporcao(List<ResultadoCaso> rs, Predicate<ResultadoCaso> criterio) {
        return divisao((int) rs.stream().filter(criterio).count(), rs.size());
    }

    private static BigDecimal divisao(int a, int b) {
        return b == 0 ? BigDecimal.ZERO.setScale(4) : BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b), 4, RoundingMode.HALF_UP);
    }
}
