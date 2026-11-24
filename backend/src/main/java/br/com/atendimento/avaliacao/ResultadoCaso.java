package br.com.atendimento.avaliacao;

import br.com.atendimento.ai.ClassificacaoIA;

/** Resultado de um caso. classificacao == null significa que a IA devolveu resposta inválida. */
public record ResultadoCaso(CasoAvaliacao caso, ClassificacaoIA classificacao, String erro) {

    public boolean valido() {
        return classificacao != null;
    }

    public boolean acertouCategoria() {
        return valido() && classificacao.categoria() == caso.categoria();
    }

    public boolean acertouPrioridade() {
        return valido() && classificacao.prioridade() == caso.prioridade();
    }

    public boolean acertouAssunto() {
        return valido() && classificacao.assunto() == caso.assunto();
    }
}
