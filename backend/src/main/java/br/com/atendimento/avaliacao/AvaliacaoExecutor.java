package br.com.atendimento.avaliacao;

import br.com.atendimento.ai.IaIndisponivelException;
import br.com.atendimento.config.AvaliacaoProperties;
import br.com.atendimento.entity.AvaliacaoExecucao;
import br.com.atendimento.repository.AvaliacaoExecucaoRepository;
import br.com.atendimento.service.ClassificacaoIaService;
import br.com.atendimento.validation.RespostaIaInvalidaException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Executa a avaliação em segundo plano (pode levar minutos com rate limit do plano gratuito).
 * Usa o MESMO ClassificacaoIaService da produção: avalia o prompt + validação reais.
 */
@Component
public class AvaliacaoExecutor {

    private static final Logger log = LoggerFactory.getLogger(AvaliacaoExecutor.class);
    private static final int TENTATIVAS_INDISPONIVEL = 3;

    private final ClassificacaoIaService classificacaoIa;
    private final DatasetAvaliacao dataset;
    private final AvaliacaoExecucaoRepository repository;
    private final AvaliacaoProperties props;
    private final ObjectMapper mapper;

    public AvaliacaoExecutor(ClassificacaoIaService classificacaoIa, DatasetAvaliacao dataset,
                             AvaliacaoExecucaoRepository repository, AvaliacaoProperties props, ObjectMapper mapper) {
        this.classificacaoIa = classificacaoIa;
        this.dataset = dataset;
        this.repository = repository;
        this.props = props;
        this.mapper = mapper;
    }

    @Async
    public void executar(Long execucaoId) {
        AvaliacaoExecucao execucao = repository.findById(execucaoId).orElseThrow();
        try {
            List<ResultadoCaso> resultados = new ArrayList<>();
            for (CasoAvaliacao caso : dataset.casos()) {
                resultados.add(avaliarCaso(caso));
                pausa(props.intervaloEntreChamadasMs());
            }
            MetricasAvaliacao m = MetricasCalculator.calcular(resultados);
            execucao.concluir(m.total(), m.acuraciaCategoria(), m.acuraciaPrioridade(), m.acuraciaAssunto(),
                    m.invalidas(), mapper.writeValueAsString(m));
            log.info("Avaliação {} concluída: acerto completo {}, categoria {}", execucaoId, m.taxaAcerto(), m.acuraciaCategoria());
        } catch (Exception e) {
            log.error("Avaliação {} falhou", execucaoId, e);
            execucao.falhar(e.getMessage());
        }
        repository.save(execucao);
    }

    private ResultadoCaso avaliarCaso(CasoAvaliacao caso) {
        for (int tentativa = 1; ; tentativa++) {
            try {
                return new ResultadoCaso(caso, classificacaoIa.classificar(caso.mensagem()), null);
            } catch (RespostaIaInvalidaException e) {
                return new ResultadoCaso(caso, null, String.join("; ", e.getErros()));
            } catch (IaIndisponivelException e) {
                if (tentativa >= TENTATIVAS_INDISPONIVEL) {
                    throw new IllegalStateException("IA indisponível durante a avaliação: " + e.getMessage(), e);
                }
                log.warn("IA indisponível na avaliação (tentativa {}), aguardando", tentativa);
                pausa(Math.max(2000L, props.intervaloEntreChamadasMs()) * tentativa);
            }
        }
    }

    private static void pausa(long ms) {
        if (ms <= 0) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Avaliação interrompida", e);
        }
    }
}
