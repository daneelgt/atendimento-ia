package br.com.atendimento.service;

import br.com.atendimento.dto.DashboardResponse;
import br.com.atendimento.entity.Departamento;
import br.com.atendimento.entity.Prioridade;
import br.com.atendimento.entity.StatusAtendimento;
import br.com.atendimento.repository.AtendimentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static br.com.atendimento.entity.StatusAtendimento.*;

/** Contagens agregadas no banco (GROUP BY): não carrega atendimentos em memória. */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final AtendimentoRepository repository;

    public DashboardService(AtendimentoRepository repository) {
        this.repository = repository;
    }

    public DashboardResponse resumo() {
        Map<String, Long> porStatus = agrupar(repository.contarPorStatus(), StatusAtendimento.values());
        Map<String, Long> porDepartamento = agrupar(repository.contarPorDepartamento(), Departamento.values());
        Map<String, Long> porPrioridade = agrupar(repository.contarPorPrioridade(), Prioridade.values());

        long total = porStatus.values().stream().mapToLong(Long::longValue).sum();
        long pendentes = porStatus.get(RECEBIDO.name()) + porStatus.get(AGUARDANDO_PROCESSAMENTO.name())
                + porStatus.get(PROCESSANDO.name());
        return new DashboardResponse(total, pendentes, porStatus.get(AGUARDANDO_REVISAO.name()),
                porStatus.get(CLASSIFICADO.name()), porStatus.get(RESPONDIDO.name()),
                porStatus.get(ERRO_PROCESSAMENTO.name()), porStatus.get(CLASSIFICACAO_MANUAL.name()),
                porStatus, porDepartamento, porPrioridade);
    }

    /** Inclui todos os valores do enum (com zero) para o painel ter sempre as mesmas chaves. */
    private static Map<String, Long> agrupar(List<Object[]> linhas, Enum<?>[] todos) {
        Map<String, Long> mapa = new LinkedHashMap<>();
        for (Enum<?> e : todos) {
            mapa.put(e.name(), 0L);
        }
        for (Object[] linha : linhas) {
            mapa.put(((Enum<?>) linha[0]).name(), (Long) linha[1]);
        }
        return mapa;
    }
}
