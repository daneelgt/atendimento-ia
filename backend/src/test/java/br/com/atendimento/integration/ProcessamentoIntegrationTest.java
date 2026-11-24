package br.com.atendimento.integration;

import br.com.atendimento.ai.IaIndisponivelException;
import br.com.atendimento.dto.NovaMensagemRequest;
import br.com.atendimento.entity.*;
import br.com.atendimento.repository.AtendimentoRepository;
import br.com.atendimento.repository.HistoricoAtendimentoRepository;
import br.com.atendimento.scheduler.ProcessamentoScheduler;
import br.com.atendimento.service.ProcessamentoService;
import br.com.atendimento.service.RecebimentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ProcessamentoIntegrationTest extends IntegrationTestBase {

    @Autowired RecebimentoService recebimento;
    @Autowired ProcessamentoService processamento;
    @Autowired ProcessamentoScheduler scheduler;
    @Autowired AtendimentoRepository repository;
    @Autowired HistoricoAtendimentoRepository historico;

    @Test
    void cenario1_mensagemClassificadaEEncaminhadaCorretamente() {
        when(clienteIA.completar(anyString(), anyString())).thenReturn(json("FINANCEIRO", "ALTA", "COBRANCA_DUPLICADA", 0.96));
        when(clienteIA.modelo()).thenReturn("modelo-teste");
        Long id = receber("Fui cobrado duas vezes na minha fatura.");

        scheduler.executarCiclo();

        Atendimento a = repository.findById(id).orElseThrow();
        assertThat(a.getStatus()).isEqualTo(StatusAtendimento.CLASSIFICADO);
        assertThat(a.getCategoria()).isEqualTo(Categoria.FINANCEIRO);
        assertThat(a.getDepartamento()).isEqualTo(Departamento.FINANCEIRO);
        assertThat(a.getConfianca()).isEqualByComparingTo("0.96");
        assertThat(a.getOrigemClassificacao()).isEqualTo(OrigemClassificacao.IA);
        assertThat(a.getModeloIa()).isEqualTo("modelo-teste");
        assertThat(a.getVersaoPrompt()).isEqualTo("v1");
        assertThat(a.getRespostaSugerida()).isNotBlank();
        assertThat(a.getRespostaFinal()).as("IA nunca responde sozinha").isNull();
    }

    @Test
    void cenarios2e3_jsonInvalidoECategoriaInexistenteVaoParaClassificacaoManual() {
        when(clienteIA.completar(anyString(), anyString()))
                .thenReturn("Claro! A categoria é financeiro.", json("JURIDICO", "ALTA", "COBRANCA_DUPLICADA", 0.9));
        Long id = receber("Fui cobrado duas vezes.");

        assertThat(processamento.processar(id)).isEqualTo(ProcessamentoService.Resultado.CLASSIFICACAO_MANUAL);

        Atendimento a = repository.findById(id).orElseThrow();
        assertThat(a.getStatus()).isEqualTo(StatusAtendimento.CLASSIFICACAO_MANUAL);
        assertThat(a.getCategoria()).as("JSON inválido não pode ser salvo como classificação").isNull();
        assertThat(a.getUltimoErro()).contains("JURIDICO");
        verify(clienteIA, times(2)).completar(anyString(), anyString());
    }

    @Test
    void cenarios4a6_iaNaoRespondeMensagemContinuaSalvaComBackoff() {
        when(clienteIA.completar(anyString(), anyString())).thenThrow(new IaIndisponivelException("Timeout ao chamar a IA (20s)"));
        Long id = receber("Não consigo entrar na minha conta.");

        Instant antes = Instant.now();
        scheduler.executarCiclo();

        Atendimento a = repository.findById(id).orElseThrow();
        assertThat(a.getStatus()).isEqualTo(StatusAtendimento.AGUARDANDO_PROCESSAMENTO);
        assertThat(a.getMensagem()).isEqualTo("Não consigo entrar na minha conta.");
        assertThat(a.getTentativas()).isEqualTo(1);
        assertThat(a.getUltimoErro()).contains("Timeout");
        assertThat(a.getProximaTentativaEm()).isAfter(antes.plusSeconds(25));

        scheduler.executarCiclo();
        verify(clienteIA, times(1)).completar(anyString(), anyString());
    }

    @Test
    void cenario5_apiIndisponivelInterrompeCicloENaoMartelaAApi() {
        when(clienteIA.completar(anyString(), anyString())).thenThrow(new IaIndisponivelException("IA indisponível: HTTP 503"));
        receber("msg 1");
        receber("msg 2");
        receber("msg 3");

        scheduler.executarCiclo();

        verify(clienteIA, times(1)).completar(anyString(), anyString());
        assertThat(repository.count()).isEqualTo(3);
        assertThat(repository.findAll()).extracting(Atendimento::getStatus)
                .containsOnly(StatusAtendimento.AGUARDANDO_PROCESSAMENTO, StatusAtendimento.RECEBIDO);
    }

    @Test
    void cenario6_recebimentoFuncionaComIaForaDoAr() {
        when(clienteIA.completar(anyString(), anyString())).thenThrow(new IaIndisponivelException("HTTP 503"));
        for (int i = 0; i < 10; i++) {
            receber("mensagem " + i);
            scheduler.executarCiclo();
        }
        assertThat(repository.count()).isEqualTo(10);
    }

    @Test
    void cenario7_mensagemReprocessadaQuandoIaVolta() {
        when(clienteIA.completar(anyString(), anyString()))
                .thenThrow(new IaIndisponivelException("HTTP 503"))
                .thenReturn(json("CANCELAMENTO", "MEDIA", "CANCELAMENTO_ASSINATURA", 0.9));
        Long id = receber("Quero cancelar minha assinatura.");
        scheduler.executarCiclo();
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(StatusAtendimento.AGUARDANDO_PROCESSAMENTO);

        jdbc.update("UPDATE atendimento SET proxima_tentativa_em = now() - interval '1 second' WHERE id = ?", id);
        scheduler.executarCiclo();

        Atendimento a = repository.findById(id).orElseThrow();
        assertThat(a.getStatus()).isEqualTo(StatusAtendimento.CLASSIFICADO);
        assertThat(a.getDepartamento()).isEqualTo(Departamento.RETENCAO);
        assertThat(a.getTentativas()).isEqualTo(2);
        assertThat(a.getUltimoErro()).isNull();
    }

    @Test
    void excedeLimiteDeTentativasVaiParaErroProcessamento() {
        when(clienteIA.completar(anyString(), anyString())).thenThrow(new IaIndisponivelException("HTTP 503"));
        Long id = receber("Quero um reembolso.");
        jdbc.update("UPDATE atendimento SET tentativas = 2, status = 'AGUARDANDO_PROCESSAMENTO' WHERE id = ?", id);

        processamento.processar(id);

        Atendimento a = repository.findById(id).orElseThrow();
        assertThat(a.getStatus()).isEqualTo(StatusAtendimento.ERRO_PROCESSAMENTO);
        assertThat(a.getTentativas()).isEqualTo(3);
    }

    @Test
    void cenario8_mensagemNaoEProcessadaDuasVezesMesmoEmParalelo() throws Exception {
        when(clienteIA.completar(anyString(), anyString())).thenAnswer(inv -> {
            Thread.sleep(300);
            return json("FINANCEIRO", "MEDIA", "REEMBOLSO", 0.9);
        });
        Long id = receber("Gostaria de solicitar um reembolso.");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);
        Callable<ProcessamentoService.Resultado> tarefa = () -> {
            largada.await();
            return processamento.processar(id);
        };
        Future<ProcessamentoService.Resultado> f1 = pool.submit(tarefa);
        Future<ProcessamentoService.Resultado> f2 = pool.submit(tarefa);
        largada.countDown();
        List<ProcessamentoService.Resultado> resultados = List.of(f1.get(5, TimeUnit.SECONDS), f2.get(5, TimeUnit.SECONDS));
        pool.shutdown();

        assertThat(resultados).containsExactlyInAnyOrder(
                ProcessamentoService.Resultado.CLASSIFICADO, ProcessamentoService.Resultado.IGNORADO);
        verify(clienteIA, times(1)).completar(anyString(), anyString());
        assertThat(processamento.processar(id)).isEqualTo(ProcessamentoService.Resultado.IGNORADO);
    }

    @Test
    void cenario12_baixaConfiancaAguardaRevisaoSemEncaminhar() {
        when(clienteIA.completar(anyString(), anyString())).thenReturn(json("OUTROS", "BAIXA", "DUVIDA_GERAL", 0.41));
        Long id = receber("Olá, tudo bem?");

        scheduler.executarCiclo();

        Atendimento a = repository.findById(id).orElseThrow();
        assertThat(a.getStatus()).isEqualTo(StatusAtendimento.AGUARDANDO_REVISAO);
        assertThat(a.getDepartamento()).isNull();
        assertThat(a.getCategoria()).isEqualTo(Categoria.OUTROS);
    }

    @Test
    void reenfileiraAtendimentoPresoEmProcessando() {
        Long id = receber("msg");
        jdbc.update("UPDATE atendimento SET status = 'PROCESSANDO', atualizado_em = now() - interval '1 hour' WHERE id = ?", id);
        when(clienteIA.completar(anyString(), anyString())).thenReturn(json("OUTROS", "BAIXA", "ELOGIO", 0.9));

        scheduler.executarCiclo();

        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(StatusAtendimento.CLASSIFICADO);
        assertThat(historico.findByAtendimentoIdOrderByCriadoEmAsc(id)).isNotEmpty();
    }

    private Long receber(String mensagem) {
        return recebimento.receber(new NovaMensagemRequest("cliente@teste.com", mensagem)).getId();
    }
}
