CREATE TABLE avaliacao_execucao (
    id                  BIGSERIAL PRIMARY KEY,
    status              VARCHAR(20)  NOT NULL,
    modelo              VARCHAR(100) NOT NULL,
    versao_prompt       VARCHAR(20)  NOT NULL,
    dataset             VARCHAR(100) NOT NULL,
    total               INT,
    acuracia_categoria  NUMERIC(5,4),
    acuracia_prioridade NUMERIC(5,4),
    acuracia_assunto    NUMERIC(5,4),
    respostas_invalidas INT,
    resultado_json      TEXT,
    erro                TEXT,
    iniciado_em         TIMESTAMPTZ  NOT NULL,
    concluido_em        TIMESTAMPTZ
);
