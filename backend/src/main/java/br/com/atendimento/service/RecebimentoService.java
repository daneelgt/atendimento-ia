package br.com.atendimento.service;

import br.com.atendimento.dto.NovaMensagemRequest;
import br.com.atendimento.entity.Atendimento;
import br.com.atendimento.repository.AtendimentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Porta de entrada das mensagens. Só persiste: não chama a IA.
 * Assim o recebimento nunca depende da disponibilidade do provedor.
 */
@Service
public class RecebimentoService {

    private static final Logger log = LoggerFactory.getLogger(RecebimentoService.class);

    private final AtendimentoRepository repository;
    private final HistoricoService historico;

    public RecebimentoService(AtendimentoRepository repository, HistoricoService historico) {
        this.repository = repository;
        this.historico = historico;
    }

    @Transactional
    public Atendimento receber(NovaMensagemRequest request) {
        Atendimento atendimento = repository.save(
                Atendimento.novo(request.clienteEmail().trim(), request.mensagem().trim()));
        historico.registrar(atendimento, null, "Mensagem recebida");
        log.info("Atendimento {} recebido", atendimento.getId());
        return atendimento;
    }
}
