package br.com.atendimento.integration;

import br.com.atendimento.dto.NovaMensagemRequest;
import br.com.atendimento.entity.StatusAtendimento;
import br.com.atendimento.repository.AtendimentoRepository;
import br.com.atendimento.service.ProcessamentoService;
import br.com.atendimento.service.RecebimentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** Ações do funcionário testadas pela API HTTP, como o painel as usa. */
class RevisaoIntegrationTest extends IntegrationTestBase {

    @Autowired TestRestTemplate http;
    @Autowired RecebimentoService recebimento;
    @Autowired ProcessamentoService processamento;
    @Autowired AtendimentoRepository repository;

    @Test
    void cenario9_funcionarioAlteraClassificacaoDeBaixaConfianca() {
        Long id = classificado(json("OUTROS", "BAIXA", "DUVIDA_GERAL", 0.4), "Paguei e continua bloqueado");
        assertThat(status(id)).isEqualTo("AGUARDANDO_REVISAO");

        ResponseEntity<Map> resp = acao(id, "reclassificar", Map.of(
                "categoria", "SUPORTE_TECNICO", "prioridade", "ALTA", "assunto", "ACESSO_CONTA"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("status", "CLASSIFICADO")
                .containsEntry("categoria", "SUPORTE_TECNICO")
                .containsEntry("departamento", "SUPORTE_TECNICO")
                .containsEntry("origemClassificacao", "MANUAL");
    }

    @Test
    void funcionarioAlteraClassificacaoEDepartamentoDeFormaIndependente() {
        Long id = classificado(json("FINANCEIRO", "ALTA", "COBRANCA_DUPLICADA", 0.9), "cobrado duas vezes");

        ResponseEntity<Map> resp = acao(id, "reclassificar", Map.of("categoria", "FINANCEIRO", "prioridade", "URGENTE",
                "assunto", "COBRANCA_DUPLICADA", "departamento", "RETENCAO"));

        assertThat(resp.getBody()).containsEntry("prioridade", "URGENTE").containsEntry("departamento", "RETENCAO");
    }

    @Test
    void cenario10_funcionarioEditaEAprovaResposta() {
        Long id = classificado(json("FINANCEIRO", "ALTA", "COBRANCA_DUPLICADA", 0.95), "cobrado duas vezes");

        ResponseEntity<Map> resp = acao(id, "aprovar", Map.of("resposta", "Olá, Maria! Já pedimos a análise ao financeiro."));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("status", "RESPONDIDO")
                .containsEntry("respostaFinal", "Olá, Maria! Já pedimos a análise ao financeiro.");
        assertThat(resp.getBody().get("respondidoEm")).isNotNull();
        List<Map> hist = http.getForObject("/api/atendimentos/" + id + "/historico", List.class);
        assertThat(hist).extracting(h -> h.get("statusNovo")).contains("APROVADO", "RESPONDIDO");
    }

    @Test
    void funcionarioAprovaSugestaoSemEditar() {
        Long id = classificado(json("CANCELAMENTO", "MEDIA", "CANCELAMENTO_ASSINATURA", 0.9), "quero cancelar");

        ResponseEntity<Map> resp = acao(id, "aprovar", Map.of());

        assertThat(resp.getBody()).containsEntry("status", "RESPONDIDO");
        assertThat(resp.getBody().get("respostaFinal")).isEqualTo(resp.getBody().get("respostaSugerida"));
    }

    @Test
    void naoAprovaAntesDaRevisaoDaClassificacao() {
        Long id = classificado(json("OUTROS", "BAIXA", "DUVIDA_GERAL", 0.3), "???");

        assertThat(acao(id, "aprovar", Map.of()).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(status(id)).isEqualTo("AGUARDANDO_REVISAO");
    }

    @Test
    void rejeitarSugestaoExigeRespostaEscritaParaAprovar() {
        Long id = classificado(json("FINANCEIRO", "MEDIA", "REEMBOLSO", 0.9), "reembolso");

        assertThat(acao(id, "rejeitar", Map.of("motivo", "Tom inadequado")).getBody().get("respostaSugerida")).isNull();
        assertThat(acao(id, "aprovar", Map.of()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(acao(id, "aprovar", Map.of("resposta", "Resposta escrita pelo funcionário")).getBody())
                .containsEntry("status", "RESPONDIDO");
    }

    @Test
    void versaoDesatualizadaRetorna409() {
        Long id = classificado(json("FINANCEIRO", "MEDIA", "REEMBOLSO", 0.9), "reembolso");
        long versaoAntiga = repository.findById(id).orElseThrow().getVersion();
        acao(id, "encaminhar", Map.of("departamento", "COMERCIAL"));

        ResponseEntity<Map> resp = http.postForEntity("/api/atendimentos/" + id + "/aprovar",
                Map.of("version", versaoAntiga), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void encaminhamentoManualEReprocessamentoDeClassificacaoManual() {
        Long id = classificado("resposta inválida", "mensagem estranha");
        assertThat(status(id)).isEqualTo("CLASSIFICACAO_MANUAL");

        assertThat(acao(id, "encaminhar", Map.of("departamento", "ATENDIMENTO_GERAL")).getBody())
                .containsEntry("status", "CLASSIFICADO").containsEntry("departamento", "ATENDIMENTO_GERAL");

        Long outro = classificado("resposta inválida", "outra mensagem estranha");
        assertThat(acao(outro, "reprocessar", Map.of()).getBody())
                .containsEntry("status", "AGUARDANDO_PROCESSAMENTO").containsEntry("tentativas", 0);
        assertThat(acao(outro, "encaminhar", Map.of("departamento", "COMERCIAL")).getStatusCode())
                .as("na fila de processamento não aceita ação manual").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void dashboardContaPorStatusDepartamentoEPrioridade() {
        classificado(json("FINANCEIRO", "ALTA", "COBRANCA_DUPLICADA", 0.9), "a");
        classificado(json("OUTROS", "BAIXA", "DUVIDA_GERAL", 0.3), "b");
        recebimento.receber(new NovaMensagemRequest("c@c.com", "pendente"));

        Map dash = http.getForObject("/api/dashboard", Map.class);

        assertThat(dash).containsEntry("total", 3).containsEntry("pendentes", 1).containsEntry("aguardandoRevisao", 1);
        assertThat((Map<String, Object>) dash.get("porDepartamento")).containsEntry("FINANCEIRO", 1);
        assertThat((Map<String, Object>) dash.get("porPrioridade")).containsEntry("ALTA", 1).containsEntry("BAIXA", 1);
    }

    private Long classificado(String respostaIa, String mensagem) {
        when(clienteIA.completar(anyString(), anyString())).thenReturn(respostaIa);
        Long id = recebimento.receber(new NovaMensagemRequest("cliente@teste.com", mensagem)).getId();
        processamento.processar(id);
        return id;
    }

    private ResponseEntity<Map> acao(Long id, String acao, Map<String, Object> corpo) {
        Map<String, Object> body = new HashMap<>(corpo);
        body.put("version", repository.findById(id).orElseThrow().getVersion());
        return http.postForEntity("/api/atendimentos/" + id + "/" + acao, body, Map.class);
    }

    private String status(Long id) {
        return (String) http.getForObject("/api/atendimentos/" + id, Map.class).get("status");
    }
}
