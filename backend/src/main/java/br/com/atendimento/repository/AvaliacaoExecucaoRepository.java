package br.com.atendimento.repository;

import br.com.atendimento.entity.AvaliacaoExecucao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvaliacaoExecucaoRepository extends JpaRepository<AvaliacaoExecucao, Long> {

    boolean existsByStatus(AvaliacaoExecucao.Status status);

    List<AvaliacaoExecucao> findAllByOrderByIniciadoEmDesc();
}
