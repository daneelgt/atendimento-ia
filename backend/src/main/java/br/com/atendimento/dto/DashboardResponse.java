package br.com.atendimento.dto;

import java.util.Map;

public record DashboardResponse(long total, long pendentes, long aguardandoRevisao, long encaminhados,
                                long respondidos, long erroProcessamento, long classificacaoManual,
                                Map<String, Long> porStatus, Map<String, Long> porDepartamento,
                                Map<String, Long> porPrioridade) {
}
