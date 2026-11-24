package br.com.atendimento.integration;

import br.com.atendimento.entity.Atendimento;
import br.com.atendimento.entity.StatusAtendimento;
import br.com.atendimento.repository.AtendimentoRepository;
import br.com.atendimento.repository.HistoricoAtendimentoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RecebimentoMensagemIntegrationTest extends IntegrationTestBase {

    @Autowired TestRestTemplate http;
    @Autowired AtendimentoRepository atendimentos;
    @Autowired HistoricoAtendimentoRepository historicos;

    @Test
    void salvaMensagemImediatamenteComStatusRecebido() {
        ResponseEntity<Map> resp = http.postForEntity("/api/mensagens",
                Map.of("clienteEmail", "cliente@teste.com", "mensagem", "  Fui cobrado duas vezes.  "), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(resp.getHeaders().getLocation()).isNotNull();
        Long id = ((Number) resp.getBody().get("id")).longValue();

        Atendimento salvo = atendimentos.findById(id).orElseThrow();
        assertThat(salvo.getStatus()).isEqualTo(StatusAtendimento.RECEBIDO);
        assertThat(salvo.getMensagem()).isEqualTo("Fui cobrado duas vezes.");
        assertThat(salvo.getCategoria()).isNull();
        assertThat(historicos.findByAtendimentoIdOrderByCriadoEmAsc(id)).hasSize(1);
    }

    @Test
    void rejeitaRequisicaoInvalidaSemSalvar() {
        long antes = atendimentos.count();

        ResponseEntity<Map> resp = http.postForEntity("/api/mensagens",
                Map.of("clienteEmail", "nao-e-email", "mensagem", ""), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, Object>) resp.getBody().get("campos")).containsKeys("clienteEmail", "mensagem");
        assertThat(atendimentos.count()).isEqualTo(antes);
    }

    @Test
    void consultaAtendimentoPorIdERetorna404QuandoNaoExiste() {
        Long id = ((Number) http.postForEntity("/api/mensagens",
                Map.of("clienteEmail", "a@b.com", "mensagem", "Não consigo entrar na conta"), Map.class)
                .getBody().get("id")).longValue();

        ResponseEntity<Map> ok = http.getForEntity("/api/atendimentos/" + id, Map.class);
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ok.getBody().get("status")).isEqualTo("RECEBIDO");

        assertThat(http.getForEntity("/api/atendimentos/999999", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
