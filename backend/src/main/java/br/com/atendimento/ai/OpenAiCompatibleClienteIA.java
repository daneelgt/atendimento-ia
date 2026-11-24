package br.com.atendimento.ai;

import br.com.atendimento.config.IaProperties;
import br.com.atendimento.validation.RespostaIaInvalidaException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;

/**
 * Cliente HTTP para qualquer API no formato "chat completions" da OpenAI.
 * Gemini, Groq, Ollama e DeepSeek expõem esse formato; trocar de provedor = trocar url/modelo/chave.
 */
@Component
@ConditionalOnProperty(name = "app.ia.provedor", havingValue = "api")
public class OpenAiCompatibleClienteIA implements ClienteIA {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleClienteIA.class);

    private final IaProperties props;
    private final ObjectMapper mapper;
    private final HttpClient http;

    public OpenAiCompatibleClienteIA(IaProperties props, ObjectMapper mapper) {
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            throw new IllegalStateException("IA_API_KEY não definida (obrigatória com IA_PROVEDOR=api)");
        }
        this.props = props;
        this.mapper = mapper;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override
    public String completar(String instrucoesSistema, String conteudoUsuario) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(props.url()))
                .timeout(Duration.ofSeconds(props.timeoutSegundos()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + props.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(corpo(instrucoesSistema, conteudoUsuario)))
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new IaIndisponivelException("Timeout ao chamar a IA (" + props.timeoutSegundos() + "s)", e);
        } catch (IOException e) {
            throw new IaIndisponivelException("Falha de conexão com a IA: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IaIndisponivelException("Chamada à IA interrompida", e);
        }

        int status = response.statusCode();
        if (status == 429 || status >= 500) {
            throw new IaIndisponivelException("IA indisponível: HTTP " + status);
        }
        if (status >= 400) {
            // 401/403/404 normalmente é configuração (chave ou modelo). Entra no mesmo backoff,
            // mas o log de erro deixa claro que retentar não resolve sozinho.
            log.error("IA recusou a requisição: HTTP {} - verifique IA_API_KEY/IA_MODELO. Corpo: {}",
                    status, abreviar(response.body()));
            throw new IaIndisponivelException("IA recusou a requisição: HTTP " + status);
        }
        return extrairConteudo(response.body());
    }

    @Override
    public String modelo() {
        return props.modelo();
    }

    private String corpo(String sistema, String usuario) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", props.modelo());
        body.put("temperature", 0);
        body.putObject("response_format").put("type", "json_object");
        var messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", sistema);
        messages.addObject().put("role", "user").put("content", usuario);
        return body.toString();
    }

    private String extrairConteudo(String corpo) {
        try {
            JsonNode content = mapper.readTree(corpo).path("choices").path(0).path("message").path("content");
            if (!content.isTextual()) {
                throw new RespostaIaInvalidaException(List.of("Resposta do provedor sem 'choices[0].message.content'"));
            }
            return content.asText();
        } catch (IOException e) {
            throw new RespostaIaInvalidaException(List.of("Corpo da resposta do provedor não é JSON"));
        }
    }

    private static String abreviar(String s) {
        return s == null ? "" : s.substring(0, Math.min(300, s.length()));
    }
}
