package br.com.atendimento.avaliacao;

import br.com.atendimento.config.AvaliacaoProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/** Carrega o dataset fixo do classpath. Mesmo arquivo = avaliações comparáveis entre si. */
@Component
public class DatasetAvaliacao {

    private final String nome;
    private final List<CasoAvaliacao> casos;

    public DatasetAvaliacao(AvaliacaoProperties props, ObjectMapper mapper) {
        this.nome = props.dataset();
        try (InputStream in = new ClassPathResource(nome).getInputStream()) {
            this.casos = List.copyOf(mapper.readValue(in, new TypeReference<List<CasoAvaliacao>>() { }));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao carregar dataset de avaliação " + nome, e);
        }
        casos.forEach(c -> {
            if (c.assunto().getCategoria() != c.categoria()) {
                throw new IllegalStateException("Dataset inconsistente: " + c);
            }
        });
    }

    public String nome() {
        return nome;
    }

    public List<CasoAvaliacao> casos() {
        return casos;
    }
}
