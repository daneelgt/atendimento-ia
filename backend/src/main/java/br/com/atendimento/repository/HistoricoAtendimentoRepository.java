package br.com.atendimento.repository;

import br.com.atendimento.entity.HistoricoAtendimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoAtendimentoRepository extends JpaRepository<HistoricoAtendimento, Long> {

    List<HistoricoAtendimento> findByAtendimentoIdOrderByCriadoEmAsc(Long atendimentoId);
}
