package br.com.atendimento.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AvaliacaoIntegrationTest extends IntegrationTestBase {

    @Autowired TestRestTemplate http;

    @Test
    void executaAvaliacaoSobreODatasetECalculaMetricas() throws InterruptedException {
        when(clienteIA.modelo()).thenReturn("modelo-teste");
        when(clienteIA.completar(anyString(), anyString())).thenReturn(json("FINANCEIRO", "ALTA", "COBRANCA_DUPLICADA", 0.9));

        ResponseEntity<Map> inicio = http.postForEntity("/api/avaliacoes", null, Map.class);
        assertThat(inicio.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        Object id = inicio.getBody().get("id");

        Map resultado = null;
        for (int i = 0; i < 50; i++) {
            resultado = http.getForObject("/api/avaliacoes/" + id, Map.class);
            if (!"EM_EXECUCAO".equals(resultado.get("status"))) break;
            Thread.sleep(100);
        }

        assertThat(resultado).containsEntry("status", "CONCLUIDA").containsEntry("modelo", "modelo-teste")
                .containsEntry("total", 55);
        Map metricas = (Map) resultado.get("metricas");
        assertThat(metricas).containsKeys("matrizConfusao", "porCategoria", "taxaAcerto", "casos");
        assertThat(((Number) resultado.get("acuraciaCategoria")).doubleValue()).isBetween(0.2, 0.4);
    }
}
