package br.com.atendimento.entity;

/** Catálogo fechado de assuntos. Cada assunto pertence a uma categoria (validado na resposta da IA). */
public enum Assunto {
    COBRANCA_DUPLICADA(Categoria.FINANCEIRO),
    COBRANCA_INDEVIDA(Categoria.FINANCEIRO),
    REEMBOLSO(Categoria.FINANCEIRO),
    SEGUNDA_VIA(Categoria.FINANCEIRO),
    ALTERACAO_PAGAMENTO(Categoria.FINANCEIRO),

    ACESSO_CONTA(Categoria.SUPORTE_TECNICO),
    CONTA_INVADIDA(Categoria.SUPORTE_TECNICO),
    ERRO_SISTEMA(Categoria.SUPORTE_TECNICO),
    LENTIDAO(Categoria.SUPORTE_TECNICO),
    SERVICO_INDISPONIVEL(Categoria.SUPORTE_TECNICO),

    CANCELAMENTO_ASSINATURA(Categoria.CANCELAMENTO),

    INFORMACAO_PLANOS(Categoria.COMERCIAL),
    UPGRADE_PLANO(Categoria.COMERCIAL),
    PROPOSTA_EMPRESARIAL(Categoria.COMERCIAL),

    ELOGIO(Categoria.OUTROS),
    SUGESTAO(Categoria.OUTROS),
    DUVIDA_GERAL(Categoria.OUTROS);

    private final Categoria categoria;

    Assunto(Categoria categoria) {
        this.categoria = categoria;
    }

    public Categoria getCategoria() {
        return categoria;
    }
}
