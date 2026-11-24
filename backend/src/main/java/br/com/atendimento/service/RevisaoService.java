package br.com.atendimento.service;

import br.com.atendimento.dto.RevisaoRequests;
import br.com.atendimento.entity.Atendimento;
import br.com.atendimento.entity.OrigemClassificacao;
import br.com.atendimento.entity.StatusAtendimento;
import br.com.atendimento.exception.ConflitoException;
import br.com.atendimento.exception.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

import static br.com.atendimento.entity.StatusAtendimento.*;

/** Ações do funcionário. Cada método valida o status atual antes de mudar o atendimento. */
@Service
@Transactional
public class RevisaoService {

    /** Estados em que o funcionário pode corrigir a classificação ou encaminhar manualmente. */
    private static final Set<StatusAtendimento> EDITAVEIS =
            EnumSet.of(CLASSIFICADO, AGUARDANDO_REVISAO, CLASSIFICACAO_MANUAL, ERRO_PROCESSAMENTO);
    private static final Set<StatusAtendimento> REPROCESSAVEIS =
            EnumSet.of(CLASSIFICADO, AGUARDANDO_REVISAO, CLASSIFICACAO_MANUAL, ERRO_PROCESSAMENTO);

    private final ConsultaAtendimentoService consulta;
    private final EnvioRespostaService envio;
    private final HistoricoService historico;

    public RevisaoService(ConsultaAtendimentoService consulta, EnvioRespostaService envio, HistoricoService historico) {
        this.consulta = consulta;
        this.envio = envio;
        this.historico = historico;
    }

    /** Aprova a sugestão (ou a versão editada) e envia. Exige atendimento já encaminhado (CLASSIFICADO). */
    public Atendimento aprovar(Long id, RevisaoRequests.Aprovar req) {
        Atendimento a = carregar(id, req.version(), EnumSet.of(CLASSIFICADO));
        boolean editada = req.resposta() != null && !req.resposta().isBlank();
        String resposta = editada ? req.resposta().trim() : a.getRespostaSugerida();
        if (resposta == null || resposta.isBlank()) {
            throw new RegraNegocioException("Não há resposta sugerida; escreva a resposta antes de aprovar");
        }
        a.aprovar(resposta);
        historico.registrar(a, CLASSIFICADO, editada ? "Resposta editada e aprovada pelo funcionário" : "Resposta sugerida aprovada");
        envio.enviar(a);
        a.marcarRespondido();
        historico.registrar(a, APROVADO, "Resposta enviada ao cliente");
        return a;
    }

    public Atendimento rejeitar(Long id, RevisaoRequests.Rejeitar req) {
        Atendimento a = carregar(id, req.version(), EnumSet.of(CLASSIFICADO, AGUARDANDO_REVISAO));
        a.rejeitarSugestao();
        historico.registrar(a, a.getStatus(), "Sugestão de resposta rejeitada: " + req.motivo());
        return a;
    }

    public Atendimento reclassificar(Long id, RevisaoRequests.Reclassificar req) {
        if (req.assunto().getCategoria() != req.categoria()) {
            throw new RegraNegocioException("Assunto " + req.assunto() + " não pertence à categoria " + req.categoria());
        }
        Atendimento a = carregar(id, req.version(), EDITAVEIS);
        StatusAtendimento anterior = a.getStatus();
        a.registrarClassificacao(req.categoria(), req.prioridade(), req.assunto().name(), null,
                a.getRespostaSugerida(), OrigemClassificacao.MANUAL, a.getModeloIa(), a.getVersaoPrompt());
        a.encaminharPara(req.departamento() != null ? req.departamento() : req.categoria().getDepartamentoPadrao());
        historico.registrar(a, anterior, "Classificação definida pelo funcionário: %s / %s, encaminhado para %s"
                .formatted(req.categoria(), req.prioridade(), a.getDepartamento().getDescricao()));
        return a;
    }

    public Atendimento encaminhar(Long id, RevisaoRequests.Encaminhar req) {
        Atendimento a = carregar(id, req.version(), EDITAVEIS);
        StatusAtendimento anterior = a.getStatus();
        a.encaminharPara(req.departamento());
        historico.registrar(a, anterior, "Encaminhado manualmente para " + req.departamento().getDescricao());
        return a;
    }

    public Atendimento reprocessar(Long id, RevisaoRequests.Reprocessar req) {
        Atendimento a = carregar(id, req.version(), REPROCESSAVEIS);
        StatusAtendimento anterior = a.getStatus();
        a.prepararReprocessamento();
        historico.registrar(a, anterior, "Reprocessamento solicitado pelo funcionário");
        return a;
    }

    private Atendimento carregar(Long id, long versionEsperada, Set<StatusAtendimento> permitidos) {
        Atendimento a = consulta.buscar(id);
        if (a.getVersion() != versionEsperada) {
            throw new ConflitoException("O atendimento foi alterado por outra operação. Recarregue e tente novamente.");
        }
        if (!permitidos.contains(a.getStatus())) {
            throw new ConflitoException("Ação não permitida para atendimento com status " + a.getStatus());
        }
        return a;
    }
}
