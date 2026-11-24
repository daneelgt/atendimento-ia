package br.com.atendimento.ai;

import br.com.atendimento.entity.Assunto;
import br.com.atendimento.entity.Categoria;
import br.com.atendimento.entity.Prioridade;

import java.math.BigDecimal;

/** Classificação JÁ VALIDADA. Só o ClassificacaoValidator cria instâncias a partir da resposta da IA. */
public record ClassificacaoIA(Categoria categoria, Prioridade prioridade, Assunto assunto,
                              BigDecimal confianca, String respostaSugerida) {
}
