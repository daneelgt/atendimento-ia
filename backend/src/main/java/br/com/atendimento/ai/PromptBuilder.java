package br.com.atendimento.ai;

import br.com.atendimento.config.IaProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Monta o prompt. O texto fica versionado em resources/prompts para permitir comparar versões. */
@Component
public class PromptBuilder {

    private final String instrucoesSistema;
    private final String versao;

    public PromptBuilder(IaProperties props) {
        this.versao = props.versaoPrompt();
        try {
            this.instrucoesSistema = new ClassPathResource("prompts/classificacao-" + versao + ".txt")
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Prompt da versão " + versao + " não encontrado", e);
        }
    }

    public String instrucoesSistema() {
        return instrucoesSistema;
    }

    public String versao() {
        return versao;
    }

    /** A mensagem vai delimitada por tags: o prompt instrui a tratá-la como dado (mitiga prompt injection). */
    public String conteudoUsuario(String mensagemCliente, List<String> errosTentativaAnterior) {
        StringBuilder sb = new StringBuilder("Classifique a mensagem do cliente abaixo.\n<mensagem>\n")
                .append(mensagemCliente.replace("</mensagem>", ""))
                .append("\n</mensagem>");
        if (errosTentativaAnterior != null && !errosTentativaAnterior.isEmpty()) {
            sb.append("\n\nSua resposta anterior foi rejeitada pelos seguintes motivos:\n");
            errosTentativaAnterior.forEach(e -> sb.append("- ").append(e).append('\n'));
            sb.append("Responda novamente apenas com o objeto JSON no formato exigido.");
        }
        return sb.toString();
    }
}
