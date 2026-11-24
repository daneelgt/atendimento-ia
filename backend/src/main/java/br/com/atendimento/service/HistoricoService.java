package br.com.atendimento.service;

import br.com.atendimento.entity.Atendimento;
import br.com.atendimento.entity.HistoricoAtendimento;
import br.com.atendimento.entity.StatusAtendimento;
import br.com.atendimento.repository.HistoricoAtendimentoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HistoricoService {

    private final HistoricoAtendimentoRepository repository;

    public HistoricoService(HistoricoAtendimentoRepository repository) {
        this.repository = repository;
    }

    public void registrar(Atendimento atendimento, StatusAtendimento anterior, String descricao) {
        repository.save(new HistoricoAtendimento(atendimento.getId(), anterior, atendimento.getStatus(), descricao));
    }

    public List<HistoricoAtendimento> listar(Long atendimentoId) {
        return repository.findByAtendimentoIdOrderByCriadoEmAsc(atendimentoId);
    }
}
