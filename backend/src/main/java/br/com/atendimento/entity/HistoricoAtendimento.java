package br.com.atendimento.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "historico_atendimento")
public class HistoricoAtendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long atendimentoId;

    @Enumerated(EnumType.STRING)
    private StatusAtendimento statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAtendimento statusNovo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false)
    private Instant criadoEm;

    protected HistoricoAtendimento() {
    }

    public HistoricoAtendimento(Long atendimentoId, StatusAtendimento statusAnterior,
                                StatusAtendimento statusNovo, String descricao) {
        this.atendimentoId = atendimentoId;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.descricao = descricao;
        this.criadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public Long getAtendimentoId() { return atendimentoId; }
    public StatusAtendimento getStatusAnterior() { return statusAnterior; }
    public StatusAtendimento getStatusNovo() { return statusNovo; }
    public String getDescricao() { return descricao; }
    public Instant getCriadoEm() { return criadoEm; }
}
