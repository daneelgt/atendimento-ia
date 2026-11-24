package br.com.atendimento.service;

import br.com.atendimento.config.ProcessamentoProperties;
import br.com.atendimento.entity.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EncaminhamentoServiceTest {

    private final EncaminhamentoService service = new EncaminhamentoService(
            new ProcessamentoProperties(false, 1000, 10, 3, 30, 600, new BigDecimal("0.70"), 5));

    @ParameterizedTest
    @CsvSource({
            "FINANCEIRO, COBRANCA_DUPLICADA, FINANCEIRO",
            "SUPORTE_TECNICO, ACESSO_CONTA, SUPORTE_TECNICO",
            "CANCELAMENTO, CANCELAMENTO_ASSINATURA, RETENCAO",
            "COMERCIAL, INFORMACAO_PLANOS, COMERCIAL",
            "OUTROS, ELOGIO, ATENDIMENTO_GERAL"})
    void encaminhaParaODepartamentoCorreto(Categoria categoria, Assunto assunto, Departamento esperado) {
        Atendimento a = classificado(categoria, assunto, "0.95");

        service.encaminharAutomaticamente(a);

        assertThat(a.getDepartamento()).isEqualTo(esperado);
        assertThat(a.getStatus()).isEqualTo(StatusAtendimento.CLASSIFICADO);
    }

    @Test
    void baixaConfiancaFicaAguardandoRevisaoSemDepartamento() {
        Atendimento a = classificado(Categoria.FINANCEIRO, Assunto.REEMBOLSO, "0.69");

        service.encaminharAutomaticamente(a);

        assertThat(a.getStatus()).isEqualTo(StatusAtendimento.AGUARDANDO_REVISAO);
        assertThat(a.getDepartamento()).isNull();
    }

    @Test
    void confiancaIgualAoLimiteEncaminha() {
        Atendimento a = classificado(Categoria.FINANCEIRO, Assunto.REEMBOLSO, "0.70");
        service.encaminharAutomaticamente(a);
        assertThat(a.getStatus()).isEqualTo(StatusAtendimento.CLASSIFICADO);
    }

    private static Atendimento classificado(Categoria c, Assunto assunto, String confianca) {
        Atendimento a = Atendimento.novo("a@b.com", "msg");
        a.registrarClassificacao(c, Prioridade.MEDIA, assunto.name(), new BigDecimal(confianca), "Olá",
                OrigemClassificacao.IA, "m", "v1");
        return a;
    }
}
