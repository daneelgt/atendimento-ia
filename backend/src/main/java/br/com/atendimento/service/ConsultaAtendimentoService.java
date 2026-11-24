package br.com.atendimento.service;

import br.com.atendimento.entity.Atendimento;
import br.com.atendimento.entity.StatusAtendimento;
import br.com.atendimento.exception.RecursoNaoEncontradoException;
import br.com.atendimento.repository.AtendimentoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConsultaAtendimentoService {

    private final AtendimentoRepository repository;

    public ConsultaAtendimentoService(AtendimentoRepository repository) {
        this.repository = repository;
    }

    public Atendimento buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atendimento " + id + " não encontrado"));
    }

    public Page<Atendimento> listar(StatusAtendimento status, Pageable pageable) {
        return status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable);
    }
}
