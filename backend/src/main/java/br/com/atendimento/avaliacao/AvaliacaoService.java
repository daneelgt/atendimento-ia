package br.com.atendimento.avaliacao;

import br.com.atendimento.entity.AvaliacaoExecucao;
import br.com.atendimento.exception.ConflitoException;
import br.com.atendimento.exception.RecursoNaoEncontradoException;
import br.com.atendimento.repository.AvaliacaoExecucaoRepository;
import br.com.atendimento.service.ClassificacaoIaService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AvaliacaoService {

    private final AvaliacaoExecucaoRepository repository;
    private final AvaliacaoExecutor executor;
    private final ClassificacaoIaService classificacaoIa;
    private final DatasetAvaliacao dataset;

    public AvaliacaoService(AvaliacaoExecucaoRepository repository, AvaliacaoExecutor executor,
                            ClassificacaoIaService classificacaoIa, DatasetAvaliacao dataset) {
        this.repository = repository;
        this.executor = executor;
        this.classificacaoIa = classificacaoIa;
        this.dataset = dataset;
    }

    /** Uma avaliação por vez: evita estourar o limite de requisições do plano gratuito. */
    public synchronized AvaliacaoExecucao iniciar() {
        if (repository.existsByStatus(AvaliacaoExecucao.Status.EM_EXECUCAO)) {
            throw new ConflitoException("Já existe uma avaliação em execução");
        }
        AvaliacaoExecucao execucao = repository.save(AvaliacaoExecucao.iniciar(
                classificacaoIa.modelo(), classificacaoIa.versaoPrompt(), dataset.nome()));
        executor.executar(execucao.getId());
        return execucao;
    }

    public List<AvaliacaoExecucao> listar() {
        return repository.findAllByOrderByIniciadoEmDesc();
    }

    public AvaliacaoExecucao buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Avaliação " + id + " não encontrada"));
    }
}
