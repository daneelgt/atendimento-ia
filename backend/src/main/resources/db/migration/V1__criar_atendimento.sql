CREATE TABLE atendimento (
    id                    BIGSERIAL PRIMARY KEY,
    cliente_email         VARCHAR(255)  NOT NULL,
    mensagem              TEXT          NOT NULL,
    status                VARCHAR(40)   NOT NULL,

    categoria             VARCHAR(40),
    prioridade            VARCHAR(20),
    assunto               VARCHAR(100),
    departamento          VARCHAR(40),
    confianca             NUMERIC(4,3)  CHECK (confianca BETWEEN 0 AND 1),
    resposta_sugerida     TEXT,
    resposta_final        TEXT,
    origem_classificacao  VARCHAR(20),
    modelo_ia             VARCHAR(100),
    versao_prompt         VARCHAR(20),

    tentativas            INT           NOT NULL DEFAULT 0,
    proxima_tentativa_em  TIMESTAMPTZ,
    ultimo_erro           TEXT,

    criado_em             TIMESTAMPTZ   NOT NULL,
    atualizado_em         TIMESTAMPTZ   NOT NULL,
    respondido_em         TIMESTAMPTZ,
    version               BIGINT        NOT NULL DEFAULT 0
);

-- Índice usado pelo worker para buscar pendentes prontos para (re)tentativa.
CREATE INDEX idx_atendimento_fila ON atendimento (status, proxima_tentativa_em);

CREATE TABLE historico_atendimento (
    id               BIGSERIAL PRIMARY KEY,
    atendimento_id   BIGINT       NOT NULL REFERENCES atendimento (id),
    status_anterior  VARCHAR(40),
    status_novo      VARCHAR(40)  NOT NULL,
    descricao        TEXT,
    criado_em        TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_historico_atendimento ON historico_atendimento (atendimento_id);
