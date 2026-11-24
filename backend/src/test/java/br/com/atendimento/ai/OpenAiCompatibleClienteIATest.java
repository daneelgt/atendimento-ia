package br.com.atendimento.ai;

import br.com.atendimento.config.IaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Usa um servidor HTTP local (JDK) para simular o provedor: sem bibliotecas extras. */
class OpenAiCompatibleClienteIATest {

    private HttpServer server;
    private final AtomicReference<String> corpoRecebido = new AtomicReference<>();

    @AfterEach
    void parar() {
        if (server != null) server.stop(0);
    }

    @Test
    void enviaRequisicaoNoFormatoChatCompletionsEExtraiConteudo() throws IOException {
        OpenAiCompatibleClienteIA cliente = iniciar(200,
                "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"{\\\"ok\\\":true}\"}}]}", 0);

        assertThat(cliente.completar("sistema", "usuario")).isEqualTo("{\"ok\":true}");
        assertThat(corpoRecebido.get()).contains("\"model\":\"modelo-x\"").contains("json_object").contains("usuario");
    }

    @Test
    void iaNaoRespondeDentroDoTimeout() throws IOException {
        OpenAiCompatibleClienteIA cliente = iniciar(200, "{}", 3000);

        assertThatThrownBy(() -> cliente.completar("s", "u"))
                .isInstanceOf(IaIndisponivelException.class).hasMessageContaining("Timeout");
    }

    @Test
    void apiIndisponivelRetornando503() throws IOException {
        OpenAiCompatibleClienteIA cliente = iniciar(503, "Service Unavailable", 0);

        assertThatThrownBy(() -> cliente.completar("s", "u"))
                .isInstanceOf(IaIndisponivelException.class).hasMessageContaining("503");
    }

    @Test
    void rateLimit429ETratadoComoIndisponibilidade() throws IOException {
        OpenAiCompatibleClienteIA cliente = iniciar(429, "{}", 0);
        assertThatThrownBy(() -> cliente.completar("s", "u")).isInstanceOf(IaIndisponivelException.class);
    }

    @Test
    void servidorForaDoArFalhaDeConexao() {
        OpenAiCompatibleClienteIA cliente = new OpenAiCompatibleClienteIA(
                new IaProperties("api", "http://127.0.0.1:1/v1/chat", "m", "chave", 1, "v1"), new ObjectMapper());
        assertThatThrownBy(() -> cliente.completar("s", "u")).isInstanceOf(IaIndisponivelException.class);
    }

    @Test
    void exigeChaveDeApi() {
        assertThatThrownBy(() -> new OpenAiCompatibleClienteIA(
                new IaProperties("api", "http://x", "m", " ", 1, "v1"), new ObjectMapper()))
                .isInstanceOf(IllegalStateException.class);
    }

    private OpenAiCompatibleClienteIA iniciar(int status, String corpo, long atrasoMs) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", ex -> {
            corpoRecebido.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try {
                Thread.sleep(atrasoMs);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(status, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        });
        server.start();
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions";
        return new OpenAiCompatibleClienteIA(new IaProperties("api", url, "modelo-x", "chave", 1, "v1"), new ObjectMapper());
    }
}
