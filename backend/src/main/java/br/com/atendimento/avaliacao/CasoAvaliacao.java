package br.com.atendimento.avaliacao;

import br.com.atendimento.entity.Assunto;
import br.com.atendimento.entity.Categoria;
import br.com.atendimento.entity.Prioridade;

/** Uma mensagem do dataset com a classificação esperada (gabarito). */
public record CasoAvaliacao(String mensagem, Categoria categoria, Prioridade prioridade, Assunto assunto) {
}
