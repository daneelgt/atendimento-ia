package br.com.atendimento.entity;

/**
 * RECEBIDO: salvo, nunca passou pela IA.
 * AGUARDANDO_PROCESSAMENTO: uma tentativa falhou; aguarda a próxima (backoff).
 */
public enum StatusAtendimento {
    RECEBIDO,
    AGUARDANDO_PROCESSAMENTO,
    PROCESSANDO,
    CLASSIFICADO,
    AGUARDANDO_REVISAO,
    APROVADO,
    RESPONDIDO,
    ERRO_PROCESSAMENTO,
    CLASSIFICACAO_MANUAL
}
