package br.com.atendimento.ai;

import br.com.atendimento.config.AvaliacaoProperties;
import br.com.atendimento.avaliacao.DatasetAvaliacao;
import br.com.atendimento.config.IaProperties;
import br.com.atendimento.validation.ClassificacaoValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FakeClienteIATest {

    @Test
    void todaRespostaDoFakePassaNoValidadorEDatasetTemAoMenos50Casos() {
        ObjectMapper mapper = new ObjectMapper();
        FakeClienteIA fake = new FakeClienteIA(mapper);
        PromptBuilder prompt = new PromptBuilder(new IaProperties("fake", null, null, null, 1, "v1"));
        ClassificacaoValidator validator = new ClassificacaoValidator(mapper);
        DatasetAvaliacao dataset = new DatasetAvaliacao(new AvaliacaoProperties("avaliacao/dataset-v1.json", 0), mapper);

        assertThat(dataset.casos()).hasSizeGreaterThanOrEqualTo(50);
        dataset.casos().forEach(caso -> assertThat(validator.validar(
                fake.completar(prompt.instrucoesSistema(), prompt.conteudoUsuario(caso.mensagem(), null)),
                caso.mensagem())).isNotNull());
    }
}
