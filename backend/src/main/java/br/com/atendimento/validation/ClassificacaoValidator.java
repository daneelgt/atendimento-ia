package br.com.atendimento.validation;

import br.com.atendimento.ai.ClassificacaoIA;
import br.com.atendimento.entity.Assunto;
import br.com.atendimento.entity.Categoria;
import br.com.atendimento.entity.Prioridade;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Única porta de entrada da resposta da IA no domínio. Nada vira ClassificacaoIA sem passar aqui.
 * Coleta TODOS os erros (não para no primeiro) para que a re-tentativa de correção seja informativa.
 */
@Component
public class ClassificacaoValidator {

    static final int TAMANHO_MAXIMO_RESPOSTA = 1000;
    private static final Pattern CERCA_MARKDOWN = Pattern.compile("^```(?:json)?\\s*(.*?)\\s*```$", Pattern.DOTALL);
    private static final Pattern NUMERO_SENSIVEL = Pattern.compile("R\\$\\s?[\\d.,]+|\\d{4,}");

    private final ObjectMapper mapper;

    public ClassificacaoValidator(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public ClassificacaoIA validar(String bruto, String mensagemOriginal) {
        if (bruto == null || bruto.isBlank()) {
            throw new RespostaIaInvalidaException(List.of("Resposta vazia"));
        }
        JsonNode raiz;
        try {
            raiz = mapper.readTree(removerCercaMarkdown(bruto.trim()));
        } catch (JsonProcessingException e) {
            throw new RespostaIaInvalidaException(List.of("JSON inválido: " + e.getOriginalMessage()));
        }
        if (raiz == null || !raiz.isObject()) {
            throw new RespostaIaInvalidaException(List.of("A resposta deve ser um objeto JSON"));
        }

        List<String> erros = new ArrayList<>();
        Categoria categoria = lerEnum(raiz, "categoria", Categoria.class, erros);
        Prioridade prioridade = lerEnum(raiz, "prioridade", Prioridade.class, erros);
        Assunto assunto = lerEnum(raiz, "assunto", Assunto.class, erros);
        BigDecimal confianca = lerConfianca(raiz, erros);
        String resposta = lerResposta(raiz, mensagemOriginal, erros);

        if (categoria != null && assunto != null && assunto.getCategoria() != categoria) {
            erros.add("Assunto " + assunto + " não pertence à categoria " + categoria);
        }
        if (!erros.isEmpty()) {
            throw new RespostaIaInvalidaException(erros);
        }
        return new ClassificacaoIA(categoria, prioridade, assunto, confianca, resposta);
    }

    /** Correção tolerada: alguns modelos envolvem o JSON em ```json ... ``` mesmo instruídos a não fazer. */
    private static String removerCercaMarkdown(String s) {
        Matcher m = CERCA_MARKDOWN.matcher(s);
        return m.matches() ? m.group(1) : s;
    }

    private static <E extends Enum<E>> E lerEnum(JsonNode raiz, String campo, Class<E> tipo, List<String> erros) {
        JsonNode no = raiz.get(campo);
        if (no == null || no.isNull()) {
            erros.add("Campo obrigatório ausente: " + campo);
            return null;
        }
        if (!no.isTextual()) {
            erros.add("Campo " + campo + " deve ser texto");
            return null;
        }
        try {
            return Enum.valueOf(tipo, no.asText().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            erros.add("Valor inválido para " + campo + ": '" + no.asText() + "'");
            return null;
        }
    }

    private static BigDecimal lerConfianca(JsonNode raiz, List<String> erros) {
        JsonNode no = raiz.get("confianca");
        if (no == null || no.isNull()) {
            erros.add("Campo obrigatório ausente: confianca");
            return null;
        }
        if (!no.isNumber()) {
            erros.add("Campo confianca deve ser numérico");
            return null;
        }
        BigDecimal valor = no.decimalValue();
        if (valor.compareTo(BigDecimal.ZERO) < 0 || valor.compareTo(BigDecimal.ONE) > 0) {
            erros.add("Confiança fora do intervalo [0, 1]: " + valor);
            return null;
        }
        return valor.setScale(3, RoundingMode.HALF_UP);
    }

    private static String lerResposta(JsonNode raiz, String mensagemOriginal, List<String> erros) {
        JsonNode no = raiz.get("resposta_sugerida");
        if (no == null || no.isNull()) {
            erros.add("Campo obrigatório ausente: resposta_sugerida");
            return null;
        }
        if (!no.isTextual()) {
            erros.add("Campo resposta_sugerida deve ser texto");
            return null;
        }
        String texto = no.asText().trim();
        if (texto.isEmpty()) {
            erros.add("resposta_sugerida está vazia");
            return null;
        }
        if (texto.length() > TAMANHO_MAXIMO_RESPOSTA) {
            erros.add("resposta_sugerida excede " + TAMANHO_MAXIMO_RESPOSTA + " caracteres");
        }
        // Heurística contra informação inventada: valores em R$ ou números longos (protocolo, pedido)
        // que não aparecem na mensagem do cliente.
        Matcher m = NUMERO_SENSIVEL.matcher(texto);
        while (m.find()) {
            if (mensagemOriginal == null || !mensagemOriginal.contains(m.group())) {
                erros.add("resposta_sugerida contém valor/número não informado pelo cliente: " + m.group());
                break;
            }
        }
        return texto;
    }
}
