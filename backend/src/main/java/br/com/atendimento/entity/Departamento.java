package br.com.atendimento.entity;

public enum Departamento {
    FINANCEIRO("Financeiro"),
    SUPORTE_TECNICO("Suporte técnico"),
    RETENCAO("Retenção/Cancelamento"),
    COMERCIAL("Comercial"),
    ATENDIMENTO_GERAL("Atendimento geral");

    private final String descricao;

    Departamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
