package br.com.atendimento.avaliacao;

import br.com.atendimento.ai.ClassificacaoIA;
import br.com.atendimento.entity.Assunto;
import br.com.atendimento.entity.Categoria;
import br.com.atendimento.entity.Prioridade;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MetricasCalculatorTest {

    @Test
    void calculaAcuraciaPrecisaoEMatrizDeConfusao() {
        CasoAvaliacao fin = new CasoAvaliacao("a", Categoria.FINANCEIRO, Prioridade.ALTA, Assunto.COBRANCA_DUPLICADA);
        CasoAvaliacao can = new CasoAvaliacao("b", Categoria.CANCELAMENTO, Prioridade.MEDIA, Assunto.CANCELAMENTO_ASSINATURA);

        List<ResultadoCaso> rs = List.of(
                new ResultadoCaso(fin, c(Categoria.FINANCEIRO, Prioridade.ALTA, Assunto.COBRANCA_DUPLICADA), null),
                new ResultadoCaso(fin, c(Categoria.FINANCEIRO, Prioridade.MEDIA, Assunto.COBRANCA_INDEVIDA), null),
                new ResultadoCaso(can, c(Categoria.FINANCEIRO, Prioridade.MEDIA, Assunto.REEMBOLSO), null),
                new ResultadoCaso(can, null, "JSON inválido"));

        MetricasAvaliacao m = MetricasCalculator.calcular(rs);

        assertThat(m.total()).isEqualTo(4);
        assertThat(m.invalidas()).isEqualTo(1);
        assertThat(m.acuraciaCategoria()).isEqualByComparingTo("0.5");
        assertThat(m.taxaAcerto()).isEqualByComparingTo("0.25");
        assertThat(m.taxaErro()).isEqualByComparingTo("0.75");
        assertThat(m.porCategoria().get("FINANCEIRO").precisao()).isEqualByComparingTo(new BigDecimal("0.6667"));
        assertThat(m.porCategoria().get("FINANCEIRO").recall()).isEqualByComparingTo("1");
        assertThat(m.porCategoria().get("CANCELAMENTO").recall()).isEqualByComparingTo("0");
        assertThat(m.matrizConfusao().get("CANCELAMENTO")).containsEntry("FINANCEIRO", 1).containsEntry("INVALIDA", 1);
    }

    private static ClassificacaoIA c(Categoria cat, Prioridade p, Assunto a) {
        return new ClassificacaoIA(cat, p, a, BigDecimal.ONE, "Olá");
    }
}
