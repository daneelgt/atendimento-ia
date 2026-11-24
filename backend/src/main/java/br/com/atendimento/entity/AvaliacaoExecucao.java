package br.com.atendimento.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "avaliacao_execucao")
public class AvaliacaoExecucao {

    public enum Status { EM_EXECUCAO, CONCLUIDA, FALHOU }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private String modelo;
    @Column(nullable = false)
    private String versaoPrompt;
    @Column(nullable = false)
    private String dataset;

    private Integer total;
    private BigDecimal acuraciaCategoria;
    private BigDecimal acuraciaPrioridade;
    private BigDecimal acuraciaAssunto;
    private Integer respostasInvalidas;

    /** Métricas completas (precisão por categoria, matriz de confusão, casos) serializadas em JSON. */
    @Column(columnDefinition = "TEXT")
    private String resultadoJson;

    @Column(columnDefinition = "TEXT")
    private String erro;

    @Column(nullable = false)
    private Instant iniciadoEm;
    private Instant concluidoEm;

    protected AvaliacaoExecucao() {
    }

    public static AvaliacaoExecucao iniciar(String modelo, String versaoPrompt, String dataset) {
        AvaliacaoExecucao e = new AvaliacaoExecucao();
        e.status = Status.EM_EXECUCAO;
        e.modelo = modelo;
        e.versaoPrompt = versaoPrompt;
        e.dataset = dataset;
        e.iniciadoEm = Instant.now();
        return e;
    }

    public void concluir(int total, BigDecimal categoria, BigDecimal prioridade, BigDecimal assunto,
                         int invalidas, String resultadoJson) {
        this.status = Status.CONCLUIDA;
        this.total = total;
        this.acuraciaCategoria = categoria;
        this.acuraciaPrioridade = prioridade;
        this.acuraciaAssunto = assunto;
        this.respostasInvalidas = invalidas;
        this.resultadoJson = resultadoJson;
        this.concluidoEm = Instant.now();
    }

    public void falhar(String erro) {
        this.status = Status.FALHOU;
        this.erro = erro;
        this.concluidoEm = Instant.now();
    }

    public Long getId() { return id; }
    public Status getStatus() { return status; }
    public String getModelo() { return modelo; }
    public String getVersaoPrompt() { return versaoPrompt; }
    public String getDataset() { return dataset; }
    public Integer getTotal() { return total; }
    public BigDecimal getAcuraciaCategoria() { return acuraciaCategoria; }
    public BigDecimal getAcuraciaPrioridade() { return acuraciaPrioridade; }
    public BigDecimal getAcuraciaAssunto() { return acuraciaAssunto; }
    public Integer getRespostasInvalidas() { return respostasInvalidas; }
    public String getResultadoJson() { return resultadoJson; }
    public String getErro() { return erro; }
    public Instant getIniciadoEm() { return iniciadoEm; }
    public Instant getConcluidoEm() { return concluidoEm; }
}
