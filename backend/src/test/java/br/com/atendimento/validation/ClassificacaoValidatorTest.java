package br.com.atendimento.validation;

import br.com.atendimento.ai.ClassificacaoIA;
import br.com.atendimento.entity.Assunto;
import br.com.atendimento.entity.Categoria;
import br.com.atendimento.entity.Prioridade;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClassificacaoValidatorTest {

    private final ClassificacaoValidator validator = new ClassificacaoValidator(new ObjectMapper());
    private static final String MSG = "Fui cobrado duas vezes pela mesma assinatura.";

    @Test
    void aceitaRespostaValida() {
        ClassificacaoIA c = validator.validar("""
                {"categoria":"FINANCEIRO","prioridade":"ALTA","assunto":"COBRANCA_DUPLICADA","confianca":0.96,
                 "resposta_sugerida":"Olá! Vamos verificar sua cobrança."}""", MSG);

        assertThat(c.categoria()).isEqualTo(Categoria.FINANCEIRO);
        assertThat(c.prioridade()).isEqualTo(Prioridade.ALTA);
        assertThat(c.assunto()).isEqualTo(Assunto.COBRANCA_DUPLICADA);
        assertThat(c.confianca()).isEqualByComparingTo("0.96");
    }

    @Test
    void aceitaJsonDentroDeCercaMarkdownENormalizaCaixa() {
        ClassificacaoIA c = validator.validar("""
                ```json
                {"categoria":"financeiro","prioridade":" alta ","assunto":"COBRANCA_DUPLICADA","confianca":1,
                 "resposta_sugerida":"Olá!"}
                ```""", MSG);
        assertThat(c.categoria()).isEqualTo(Categoria.FINANCEIRO);
        assertThat(c.confianca()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "não é json", "{\"categoria\": ", "[1,2]", "Claro! Aqui está: {}"})
    void rejeitaJsonInvalido(String bruto) {
        assertThatThrownBy(() -> validator.validar(bruto, MSG)).isInstanceOf(RespostaIaInvalidaException.class);
    }

    @Test
    void rejeitaCategoriaInexistente() {
        assertThatThrownBy(() -> validator.validar("""
                {"categoria":"JURIDICO","prioridade":"ALTA","assunto":"COBRANCA_DUPLICADA","confianca":0.9,
                 "resposta_sugerida":"Olá!"}""", MSG))
                .isInstanceOf(RespostaIaInvalidaException.class)
                .satisfies(e -> assertThat(((RespostaIaInvalidaException) e).getErros())
                        .anyMatch(m -> m.contains("categoria") && m.contains("JURIDICO")));
    }

    @Test
    void coletaTodosOsErrosDeCamposAusentesETiposErrados() {
        assertThatThrownBy(() -> validator.validar("""
                {"categoria":1,"prioridade":"ALTISSIMA","confianca":"0.9","resposta_sugerida":"  "}""", MSG))
                .isInstanceOf(RespostaIaInvalidaException.class)
                .satisfies(e -> assertThat(((RespostaIaInvalidaException) e).getErros()).containsExactlyInAnyOrder(
                        "Campo categoria deve ser texto",
                        "Valor inválido para prioridade: 'ALTISSIMA'",
                        "Campo obrigatório ausente: assunto",
                        "Campo confianca deve ser numérico",
                        "resposta_sugerida está vazia"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.1", "1.01", "96"})
    void rejeitaConfiancaForaDoIntervalo(String confianca) {
        assertThatThrownBy(() -> validator.validar("""
                {"categoria":"FINANCEIRO","prioridade":"ALTA","assunto":"COBRANCA_DUPLICADA","confianca":%s,
                 "resposta_sugerida":"Olá!"}""".formatted(confianca), MSG))
                .isInstanceOf(RespostaIaInvalidaException.class)
                .hasMessageContaining("Confiança fora do intervalo");
    }

    @Test
    void rejeitaAssuntoQueNaoPertenceACategoria() {
        assertThatThrownBy(() -> validator.validar("""
                {"categoria":"COMERCIAL","prioridade":"ALTA","assunto":"COBRANCA_DUPLICADA","confianca":0.9,
                 "resposta_sugerida":"Olá!"}""", MSG))
                .hasMessageContaining("não pertence à categoria");
    }

    @Test
    void rejeitaRespostaComValoresInventados() {
        assertThatThrownBy(() -> validator.validar("""
                {"categoria":"FINANCEIRO","prioridade":"ALTA","assunto":"REEMBOLSO","confianca":0.9,
                 "resposta_sugerida":"Seu reembolso de R$ 59,90 foi aprovado, protocolo 123456."}""", MSG))
                .hasMessageContaining("valor/número não informado");
    }
}
