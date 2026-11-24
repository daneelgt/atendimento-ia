package br.com.atendimento.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "atendimento")
public class Atendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String clienteEmail;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAtendimento status;

    @Enumerated(EnumType.STRING)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    private Prioridade prioridade;

    private String assunto;

    @Enumerated(EnumType.STRING)
    private Departamento departamento;

    @Column(precision = 4, scale = 3)
    private BigDecimal confianca;

    @Column(columnDefinition = "TEXT")
    private String respostaSugerida;

    @Column(columnDefinition = "TEXT")
    private String respostaFinal;

    @Enumerated(EnumType.STRING)
    private OrigemClassificacao origemClassificacao;

    private String modeloIa;
    private String versaoPrompt;

    @Column(nullable = false)
    private int tentativas;

    private Instant proximaTentativaEm;

    @Column(columnDefinition = "TEXT")
    private String ultimoErro;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(nullable = false)
    private Instant atualizadoEm;

    private Instant respondidoEm;

    /** Lock otimista: impede que duas ações concorrentes sobrescrevam uma à outra. */
    @Version
    private long version;

    protected Atendimento() {
    }

    public static Atendimento novo(String clienteEmail, String mensagem) {
        Atendimento a = new Atendimento();
        a.clienteEmail = clienteEmail;
        a.mensagem = mensagem;
        a.status = StatusAtendimento.RECEBIDO;
        return a;
    }

    @PrePersist
    void aoCriar() {
        Instant agora = Instant.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = Instant.now();
    }

    // ---- Transições de estado (as regras de "quando pode" ficam nos services) ----

    public void registrarClassificacao(Categoria categoria, Prioridade prioridade, String assunto,
                                       BigDecimal confianca, String respostaSugerida,
                                       OrigemClassificacao origem, String modeloIa, String versaoPrompt) {
        this.categoria = categoria;
        this.prioridade = prioridade;
        this.assunto = assunto;
        this.confianca = confianca;
        this.respostaSugerida = respostaSugerida;
        this.origemClassificacao = origem;
        this.modeloIa = modeloIa;
        this.versaoPrompt = versaoPrompt;
        this.ultimoErro = null;
        this.proximaTentativaEm = null;
    }

    public void encaminharPara(Departamento departamento) {
        this.departamento = departamento;
        this.status = StatusAtendimento.CLASSIFICADO;
    }

    public void aguardarRevisaoManual() {
        this.departamento = null;
        this.status = StatusAtendimento.AGUARDANDO_REVISAO;
    }

    public void agendarNovaTentativa(Instant quando, String erro) {
        this.status = StatusAtendimento.AGUARDANDO_PROCESSAMENTO;
        this.proximaTentativaEm = quando;
        this.ultimoErro = erro;
    }

    public void marcarErroProcessamento(String erro) {
        this.status = StatusAtendimento.ERRO_PROCESSAMENTO;
        this.proximaTentativaEm = null;
        this.ultimoErro = erro;
    }

    public void marcarClassificacaoManual(String erro) {
        this.status = StatusAtendimento.CLASSIFICACAO_MANUAL;
        this.proximaTentativaEm = null;
        this.ultimoErro = erro;
    }

    public void rejeitarSugestao() {
        this.respostaSugerida = null;
    }

    public void aprovar(String respostaFinal) {
        this.respostaFinal = respostaFinal;
        this.status = StatusAtendimento.APROVADO;
    }

    public void marcarRespondido() {
        this.status = StatusAtendimento.RESPONDIDO;
        this.respondidoEm = Instant.now();
    }

    public void prepararReprocessamento() {
        registrarClassificacao(null, null, null, null, null, null, null, null);
        this.departamento = null;
        this.tentativas = 0;
        this.status = StatusAtendimento.AGUARDANDO_PROCESSAMENTO;
        this.proximaTentativaEm = Instant.now();
    }

    public Long getId() { return id; }
    public String getClienteEmail() { return clienteEmail; }
    public String getMensagem() { return mensagem; }
    public StatusAtendimento getStatus() { return status; }
    public Categoria getCategoria() { return categoria; }
    public Prioridade getPrioridade() { return prioridade; }
    public String getAssunto() { return assunto; }
    public Departamento getDepartamento() { return departamento; }
    public BigDecimal getConfianca() { return confianca; }
    public String getRespostaSugerida() { return respostaSugerida; }
    public String getRespostaFinal() { return respostaFinal; }
    public OrigemClassificacao getOrigemClassificacao() { return origemClassificacao; }
    public String getModeloIa() { return modeloIa; }
    public String getVersaoPrompt() { return versaoPrompt; }
    public int getTentativas() { return tentativas; }
    public Instant getProximaTentativaEm() { return proximaTentativaEm; }
    public String getUltimoErro() { return ultimoErro; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public Instant getRespondidoEm() { return respondidoEm; }
    public long getVersion() { return version; }
}
