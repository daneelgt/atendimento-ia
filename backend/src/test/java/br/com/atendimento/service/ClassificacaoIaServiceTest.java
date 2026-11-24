package br.com.atendimento.service;

import br.com.atendimento.ai.ClienteIA;
import br.com.atendimento.ai.IaIndisponivelException;
import br.com.atendimento.ai.PromptBuilder;
import br.com.atendimento.config.IaProperties;
import br.com.atendimento.entity.Categoria;
import br.com.atendimento.validation.ClassificacaoValidator;
import br.com.atendimento.validation.RespostaIaInvalidaException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ClassificacaoIaServiceTest {

    private static final String VALIDO = """
            {"categoria":"CANCELAMENTO","prioridade":"MEDIA","assunto":"CANCELAMENTO_ASSINATURA","confianca":0.9,
             "resposta_sugerida":"Olá! Recebemos sua solicitação."}""";

    private final ClienteIA cliente = mock(ClienteIA.class);
    private final ClassificacaoIaService service = new ClassificacaoIaService(cliente,
            new PromptBuilder(new IaProperties("fake", null, "m", null, 5, "v1")),
            new ClassificacaoValidator(new ObjectMapper()));

    @Test
    void classificaNaPrimeiraTentativa() {
        when(cliente.completar(anyString(), anyString())).thenReturn(VALIDO);

        assertThat(service.classificar("Quero cancelar").categoria()).isEqualTo(Categoria.CANCELAMENTO);
        verify(cliente, times(1)).completar(anyString(), anyString());
    }

    @Test
    void repeteUmaVezInformandoOsErrosQuandoRespostaInvalida() {
        when(cliente.completar(anyString(), anyString())).thenReturn("isso não é json", VALIDO);

        assertThat(service.classificar("Quero cancelar").categoria()).isEqualTo(Categoria.CANCELAMENTO);

        ArgumentCaptor<String> usuario = ArgumentCaptor.forClass(String.class);
        verify(cliente, times(2)).completar(anyString(), usuario.capture());
        assertThat(usuario.getAllValues().get(1)).contains("rejeitada").contains("JSON inválido");
    }

    @Test
    void desisteAposSegundaRespostaInvalida() {
        when(cliente.completar(anyString(), anyString())).thenReturn(VALIDO.replace("CANCELAMENTO\"", "INEXISTENTE\""));

        assertThatThrownBy(() -> service.classificar("Quero cancelar")).isInstanceOf(RespostaIaInvalidaException.class);
        verify(cliente, times(2)).completar(anyString(), anyString());
    }

    @Test
    void propagaIndisponibilidadeSemRetentarAqui() {
        when(cliente.completar(anyString(), anyString())).thenThrow(new IaIndisponivelException("HTTP 503"));

        assertThatThrownBy(() -> service.classificar("x")).isInstanceOf(IaIndisponivelException.class);
        verify(cliente, times(1)).completar(anyString(), anyString());
    }
}
