package br.com.atendimento.service;

import br.com.atendimento.ai.ClassificacaoIA;
import br.com.atendimento.config.ProcessamentoProperties;
import br.com.atendimento.entity.Atendimento;
import br.com.atendimento.entity.OrigemClassificacao;
import br.com.atendimento.entity.StatusAtendimento;
import br.com.atendimento.repository.AtendimentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Grava o desfecho de uma tentativa de processamento. Cada método é uma transação curta. */
@Service
public class ResultadoProcessamentoService {

    private static final Logger log = LoggerFactory.getLogger(ResultadoProcessamentoService.class);

    private final AtendimentoRepository repository;
    private final EncaminhamentoService encaminhamento;
    private final BackoffCalculator backoff;
    private final HistoricoService historico;
    private final ProcessamentoProperties props;

    public ResultadoProcessamentoService(AtendimentoRepository repository, EncaminhamentoService encaminhamento,
                                         BackoffCalculator backoff, HistoricoService historico,
                                         ProcessamentoProperties props) {
        this.repository = repository;
        this.encaminhamento = encaminhamento;
        this.backoff = backoff;
        this.historico = historico;
        this.props = props;
    }

    @Transactional
    public StatusAtendimento aplicarClassificacao(Long id, ClassificacaoIA c, String modelo, String versaoPrompt) {
        Atendimento a = repository.findById(id).orElseThrow();
        a.registrarClassificacao(c.categoria(), c.prioridade(), c.assunto().name(), c.confianca(),
                c.respostaSugerida(), OrigemClassificacao.IA, modelo, versaoPrompt);
        encaminhamento.encaminharAutomaticamente(a);
        String descricao = a.getStatus() == StatusAtendimento.CLASSIFICADO
                ? "Classificado pela IA como %s (confiança %s) e encaminhado para %s"
                        .formatted(c.categoria(), c.confianca(), a.getDepartamento().getDescricao())
                : "Classificado pela IA como %s com baixa confiança (%s); aguardando revisão"
                        .formatted(c.categoria(), c.confianca());
        historico.registrar(a, StatusAtendimento.PROCESSANDO, descricao);
        return a.getStatus();
    }

    @Transactional
    public void registrarFalhaTemporaria(Long id, String erro) {
        Atendimento a = repository.findById(id).orElseThrow();
        if (a.getTentativas() >= props.maxTentativas()) {
            a.marcarErroProcessamento(erro);
            log.error("Atendimento {} excedeu {} tentativas: {}", id, props.maxTentativas(), erro);
            historico.registrar(a, StatusAtendimento.PROCESSANDO,
                    "Limite de tentativas atingido. Último erro: " + erro);
            return;
        }
        Instant quando = Instant.now().plus(backoff.atraso(a.getTentativas()));
        a.agendarNovaTentativa(quando, erro);
        log.warn("Atendimento {} tentativa {} falhou ({}). Nova tentativa em {}", id, a.getTentativas(), erro, quando);
        historico.registrar(a, StatusAtendimento.PROCESSANDO,
                "Tentativa %d falhou: %s. Nova tentativa agendada".formatted(a.getTentativas(), erro));
    }

    @Transactional
    public void enviarParaClassificacaoManual(Long id, String erro) {
        Atendimento a = repository.findById(id).orElseThrow();
        a.marcarClassificacaoManual(erro);
        historico.registrar(a, StatusAtendimento.PROCESSANDO, "IA retornou resposta inválida: " + erro);
    }
}
