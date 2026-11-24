package br.com.atendimento.scheduler;

import br.com.atendimento.config.ProcessamentoProperties;
import br.com.atendimento.entity.StatusAtendimento;
import br.com.atendimento.repository.AtendimentoRepository;
import br.com.atendimento.service.ProcessamentoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

/**
 * Worker: a cada ciclo busca atendimentos prontos e processa um por um.
 * Se a IA estiver indisponível, encerra o ciclo (não adianta martelar a API com o resto do lote);
 * as mensagens restantes continuam salvas e entram no próximo ciclo.
 */
@Component
public class ProcessamentoScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProcessamentoScheduler.class);

    private final AtendimentoRepository repository;
    private final ProcessamentoService processamento;
    private final ProcessamentoProperties props;

    public ProcessamentoScheduler(AtendimentoRepository repository, ProcessamentoService processamento,
                                  ProcessamentoProperties props) {
        this.repository = repository;
        this.processamento = processamento;
        this.props = props;
    }

    @Scheduled(fixedDelayString = "${app.processamento.intervalo-ms}")
    public void executarCiclo() {
        Instant agora = Instant.now();
        int travados = repository.reenfileirarTravados(StatusAtendimento.PROCESSANDO,
                StatusAtendimento.AGUARDANDO_PROCESSAMENTO, agora.minus(Duration.ofMinutes(props.travadoAposMinutos())), agora);
        if (travados > 0) {
            log.warn("{} atendimento(s) preso(s) em PROCESSANDO foram reenfileirados", travados);
        }

        List<Long> ids = repository.findIdsProntosParaProcessar(
                EnumSet.of(StatusAtendimento.RECEBIDO, StatusAtendimento.AGUARDANDO_PROCESSAMENTO),
                agora, Limit.of(props.tamanhoLote()));
        for (Long id : ids) {
            if (processamento.processar(id) == ProcessamentoService.Resultado.IA_INDISPONIVEL) {
                log.warn("IA indisponível; encerrando ciclo. {} atendimento(s) seguem na fila", ids.size());
                break;
            }
        }
    }
}
