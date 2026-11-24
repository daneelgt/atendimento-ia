package br.com.atendimento.service;

import br.com.atendimento.ai.ClassificacaoIA;
import br.com.atendimento.ai.IaIndisponivelException;
import br.com.atendimento.entity.StatusAtendimento;
import br.com.atendimento.repository.AtendimentoRepository;
import br.com.atendimento.validation.RespostaIaInvalidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/**
 * Orquestra o processamento de UM atendimento:
 * 1) reivindica (transação curta)  2) chama a IA (FORA de transação: não segura conexão do banco
 * durante a chamada HTTP)  3) grava o desfecho (transação curta).
 */
@Service
public class ProcessamentoService {

    public enum Resultado { IGNORADO, CLASSIFICADO, AGUARDANDO_REVISAO, CLASSIFICACAO_MANUAL, FALHA_TEMPORARIA, IA_INDISPONIVEL }

    static final Set<StatusAtendimento> PROCESSAVEIS =
            EnumSet.of(StatusAtendimento.RECEBIDO, StatusAtendimento.AGUARDANDO_PROCESSAMENTO);

    private static final Logger log = LoggerFactory.getLogger(ProcessamentoService.class);

    private final AtendimentoRepository repository;
    private final ClassificacaoIaService classificacaoIa;
    private final ResultadoProcessamentoService resultado;

    public ProcessamentoService(AtendimentoRepository repository, ClassificacaoIaService classificacaoIa,
                                ResultadoProcessamentoService resultado) {
        this.repository = repository;
        this.classificacaoIa = classificacaoIa;
        this.resultado = resultado;
    }

    public Resultado processar(Long id) {
        if (repository.reivindicar(id, PROCESSAVEIS, StatusAtendimento.PROCESSANDO, Instant.now()) == 0) {
            log.debug("Atendimento {} já está sendo/foi processado; ignorando", id);
            return Resultado.IGNORADO;
        }
        String mensagem = repository.findById(id).orElseThrow().getMensagem();
        try {
            ClassificacaoIA c = classificacaoIa.classificar(mensagem);
            StatusAtendimento status = resultado.aplicarClassificacao(id, c, classificacaoIa.modelo(), classificacaoIa.versaoPrompt());
            return status == StatusAtendimento.CLASSIFICADO ? Resultado.CLASSIFICADO : Resultado.AGUARDANDO_REVISAO;
        } catch (IaIndisponivelException e) {
            resultado.registrarFalhaTemporaria(id, e.getMessage());
            return Resultado.IA_INDISPONIVEL;
        } catch (RespostaIaInvalidaException e) {
            resultado.enviarParaClassificacaoManual(id, String.join("; ", e.getErros()));
            return Resultado.CLASSIFICACAO_MANUAL;
        } catch (RuntimeException e) {
            log.error("Erro inesperado ao processar atendimento {}", id, e);
            resultado.registrarFalhaTemporaria(id, "Erro inesperado: " + e.getClass().getSimpleName());
            return Resultado.FALHA_TEMPORARIA;
        }
    }
}
