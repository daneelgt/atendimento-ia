package br.com.atendimento.entity;

public enum Categoria {
    FINANCEIRO(Departamento.FINANCEIRO),
    SUPORTE_TECNICO(Departamento.SUPORTE_TECNICO),
    CANCELAMENTO(Departamento.RETENCAO),
    COMERCIAL(Departamento.COMERCIAL),
    OUTROS(Departamento.ATENDIMENTO_GERAL);

    private final Departamento departamentoPadrao;

    Categoria(Departamento departamentoPadrao) {
        this.departamentoPadrao = departamentoPadrao;
    }

    public Departamento getDepartamentoPadrao() {
        return departamentoPadrao;
    }
}
