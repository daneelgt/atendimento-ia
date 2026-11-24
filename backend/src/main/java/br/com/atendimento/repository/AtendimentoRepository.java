package br.com.atendimento.repository;

import br.com.atendimento.entity.Atendimento;
import br.com.atendimento.entity.StatusAtendimento;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {

    Page<Atendimento> findByStatus(StatusAtendimento status, Pageable pageable);

    @Query("""
            select a.id from Atendimento a
            where a.status in :status and (a.proximaTentativaEm is null or a.proximaTentativaEm <= :agora)
            order by a.criadoEm""")
    List<Long> findIdsProntosParaProcessar(Collection<StatusAtendimento> status, Instant agora, Limit limit);

    /**
     * Reivindicação atômica: só UMA transação consegue mudar o status para PROCESSANDO.
     * Retorna 1 para quem ganhou e 0 para os demais (outra thread/instância ou status já mudou).
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Atendimento a
            set a.status = :processando, a.tentativas = a.tentativas + 1,
                a.atualizadoEm = :agora, a.version = a.version + 1
            where a.id = :id and a.status in :permitidos""")
    int reivindicar(Long id, Collection<StatusAtendimento> permitidos, StatusAtendimento processando, Instant agora);

    /** Recupera mensagens presas em PROCESSANDO (ex.: aplicação caiu no meio da chamada à IA). */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Atendimento a
            set a.status = :aguardando, a.proximaTentativaEm = :agora,
                a.ultimoErro = 'Processamento interrompido; reenfileirado automaticamente',
                a.atualizadoEm = :agora, a.version = a.version + 1
            where a.status = :processando and a.atualizadoEm < :limite""")
    int reenfileirarTravados(StatusAtendimento processando, StatusAtendimento aguardando, Instant limite, Instant agora);

    @Query("select a.status, count(a) from Atendimento a group by a.status")
    List<Object[]> contarPorStatus();

    @Query("select a.departamento, count(a) from Atendimento a where a.departamento is not null group by a.departamento")
    List<Object[]> contarPorDepartamento();

    @Query("select a.prioridade, count(a) from Atendimento a where a.prioridade is not null group by a.prioridade")
    List<Object[]> contarPorPrioridade();
}
