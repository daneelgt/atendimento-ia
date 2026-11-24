# Atendimento IA

Sistema de atendimento ao cliente que usa IA para **classificar, priorizar e encaminhar** mensagens e **sugerir respostas**, sempre com **aprovação humana** antes do envio.

> Princípio central: a IA é só um serviço externo que *opina*. Quem valida, salva, decide o encaminhamento e controla o fluxo é o Java. Se a IA cair, o sistema continua recebendo mensagens e nenhuma se perde.

**Stack:** Java 21 · Spring Boot 3.5 (Web, Data JPA, Validation) · PostgreSQL 16 · Flyway · Google Gemini (API compatível com OpenAI) · React 18 + Vite · JUnit 5 · Mockito · Testcontainers

---

## Como executar

Pré-requisitos: Java 21, Maven, Node 18+ e Docker.

```bash
# 1. Banco
docker compose up -d

# 2. Backend (porta 8080) — sem chave, usa o classificador local "fake"
cd backend
mvn spring-boot:run

# 3. Painel (porta 5173)
cd frontend
npm install
npm run dev
```

Abra http://localhost:5173 e use a aba **Simular mensagem**.

### Usando o Gemini (plano gratuito)

1. Gere uma chave em https://aistudio.google.com → *Get API key*.
2. Rode o backend com:

```bash
export IA_PROVEDOR=api
export IA_API_KEY=sua-chave
export IA_MODELO=gemini-2.5-flash        # confira os modelos Flash disponíveis no AI Studio
export AVALIACAO_INTERVALO_MS=6000       # respeita o limite de requisições/minuto do plano gratuito
mvn spring-boot:run
```

Outros provedores no mesmo formato funcionam trocando só `IA_URL`, `IA_MODELO` e `IA_API_KEY`:

| Provedor | IA_URL |
|---|---|
| Gemini (padrão) | `https://generativelanguage.googleapis.com/v1beta/openai/chat/completions` |
| Groq | `https://api.groq.com/openai/v1/chat/completions` |
| Ollama (local, offline) | `http://localhost:11434/v1/chat/completions` (chave qualquer) |
| DeepSeek | `https://api.deepseek.com/chat/completions` |

> No plano gratuito do Gemini, o Google pode usar os dados enviados para melhorar os modelos. Com dados reais de clientes, use o plano pago ou o Ollama.

### Testes

```bash
cd backend
mvn test     # 58 testes; os de integração sobem um PostgreSQL real via Docker (Testcontainers)
```

---

## Arquitetura

```
 Cliente ──POST /api/mensagens──► MensagemController ──► RecebimentoService ──► PostgreSQL (status RECEBIDO)
                                        │ 202 Accepted (não espera a IA)
                                        ▼
 ProcessamentoScheduler (a cada 5s) ──► ProcessamentoService
        1. reivindica a mensagem (UPDATE atômico → PROCESSANDO)
        2. ClassificacaoIaService ──► ClienteIA (Gemini) ──► texto bruto
                                 └─► ClassificacaoValidator ──► ClassificacaoIA (válida) ou erro
        3. ResultadoProcessamentoService grava o desfecho:
             válida + confiança ≥ 0,70 → EncaminhamentoService → CLASSIFICADO (departamento definido)
             válida + confiança < 0,70 → AGUARDANDO_REVISAO
             inválida após correção    → CLASSIFICACAO_MANUAL
             IA fora do ar             → AGUARDANDO_PROCESSAMENTO + backoff (ou ERRO_PROCESSAMENTO no limite)

 Painel React ──► RevisaoController ──► RevisaoService (aprovar / editar / rejeitar / reclassificar / encaminhar / reprocessar)
                                            └─► EnvioRespostaService (único ponto de envio; só após aprovação)
```

**Por que um monólito com o banco como fila?** Para cerca de 500 mensagens por dia, Kafka ou RabbitMQ seriam complexidade sem benefício. A tabela `atendimento` com `status` e `proxima_tentativa_em` já é uma fila durável, que sobrevive a reinícios e permite consultas e auditoria.

### Estrutura de pastas

```
backend/src/main/java/br/com/atendimento/
├── controller/   Recebem HTTP e delegam. Nenhuma regra de negócio.
├── service/      Regras: recebimento, processamento, encaminhamento, revisão, dashboard, backoff.
├── scheduler/    Worker que consome a fila.
├── ai/           Fronteira com a IA: ClienteIA (interface), cliente HTTP, fake, prompt.
├── validation/   ClassificacaoValidator: única porta de entrada da resposta da IA.
├── avaliacao/    Dataset, execução e cálculo de métricas de qualidade.
├── entity/       Atendimento, HistoricoAtendimento, AvaliacaoExecucao e enums.
├── repository/   Spring Data JPA, incluindo as queries atômicas da fila.
├── dto/          Contratos da API (a entidade nunca é exposta diretamente).
├── exception/    Exceções de negócio e GlobalExceptionHandler (respostas de erro padronizadas).
└── config/       Propriedades tipadas (app.ia, app.processamento, app.avaliacao).
backend/src/main/resources/
├── db/migration/          Schema versionado (Flyway)
├── prompts/               Prompt versionado (classificacao-v1.txt)
└── avaliacao/             Dataset de avaliação (55 mensagens com gabarito)
frontend/src/              Painel React (sem bibliotecas de UI)
```

---

## Modelo de dados

**atendimento**: a mensagem e todo o seu ciclo de vida.

| Grupo | Colunas |
|---|---|
| Entrada | `id`, `cliente_email`, `mensagem`, `criado_em` |
| Classificação | `categoria`, `prioridade`, `assunto`, `departamento`, `confianca` (CHECK 0–1), `resposta_sugerida`, `origem_classificacao` (IA/MANUAL), `modelo_ia`, `versao_prompt` |
| Resposta | `resposta_final`, `respondido_em` |
| Controle da fila | `status`, `tentativas`, `proxima_tentativa_em`, `ultimo_erro` (índice em `status, proxima_tentativa_em`) |
| Concorrência | `version` (lock otimista) |

**historico_atendimento**: trilha de auditoria (status anterior → novo, descrição, data).
**avaliacao_execucao**: cada execução da avaliação, com modelo, versão do prompt, acurácias e métricas completas em JSON.

### Ciclo de status

```
RECEBIDO ─► PROCESSANDO ─┬─► CLASSIFICADO ─(funcionário aprova)─► APROVADO ─► RESPONDIDO
                         ├─► AGUARDANDO_REVISAO ─(funcionário classifica)─► CLASSIFICADO
                         ├─► CLASSIFICACAO_MANUAL ─(classifica/encaminha)─► CLASSIFICADO
                         └─► AGUARDANDO_PROCESSAMENTO ─(backoff)─► PROCESSANDO ...
                                       └─(limite de tentativas)─► ERRO_PROCESSAMENTO
```

- `RECEBIDO` = nunca passou pela IA. `AGUARDANDO_PROCESSAMENTO` = já falhou e espera a próxima tentativa.
- `CLASSIFICADO` = encaminhado a um departamento, com a resposta aguardando revisão.
- `AGUARDANDO_REVISAO` = a IA classificou com baixa confiança; a classificação precisa de um humano.

---

## Componentes principais

### Recebimento: `RecebimentoService`
Só valida e grava, **sem chamar a IA**. A resposta HTTP é `202 Accepted`: a mensagem foi aceita e o processamento vem depois. É isso que garante que nada se perde com a IA fora do ar.

### Integração com IA: pacote `ai`
- **`ClienteIA`** é uma interface com um único método: recebe o prompt e devolve **texto bruto**. A IA não devolve objetos de domínio; quem interpreta é o Java.
- **`OpenAiCompatibleClienteIA`** usa o `HttpClient` nativo do Java (sem SDK), com `temperature: 0` e `response_format: json_object`. Ele traduz falhas de transporte (timeout, conexão recusada, HTTP 429 e 5xx) para `IaIndisponivelException`.
- **`FakeClienteIA`** é um classificador por palavras-chave. Permite rodar sem chave e serve de linha de base na avaliação. Ele devolve JSON, então passa pelo mesmo validador da IA real.
- **`PromptBuilder`** carrega o prompt versionado e envolve a mensagem em `<mensagem>…</mensagem>`. O prompt manda tratar esse conteúdo como dado e ignorar instruções dentro dele, o que mitiga *prompt injection* (há um caso assim no dataset).

O prompt define um **catálogo fechado** de categorias, assuntos e prioridades, com regras explícitas de prioridade, e **proíbe** inventar valores, prazos, protocolos ou prometer reembolso.

### Validação: `ClassificacaoValidator`
Nada vira classificação sem passar por aqui. O validador verifica:
- se é JSON válido e se é um objeto;
- se os campos obrigatórios existem e têm o tipo certo (texto ou número);
- se categoria, prioridade e assunto pertencem aos enums;
- se o **assunto pertence à categoria** (ex.: `COBRANCA_DUPLICADA` só em `FINANCEIRO`);
- se a confiança está entre 0 e 1;
- se a resposta sugerida não está vazia e tem no máximo 1000 caracteres;
- uma **heurística anti-invenção**: rejeita respostas com valores em R$ ou números longos (protocolos) que não aparecem na mensagem do cliente.

Ele coleta **todos** os erros de uma vez, em vez de parar no primeiro. Correções toleradas: remover a cerca ```` ```json ```` e normalizar maiúsculas e espaços nos enums.

### Tratamento de falhas

| Falha | Onde é tratada | O que acontece |
|---|---|---|
| JSON inválido, campo ausente, categoria inexistente | `ClassificacaoIaService` | 1 nova tentativa, enviando os erros para a IA corrigir. Se falhar de novo: `CLASSIFICACAO_MANUAL`, com o erro registrado em `ultimo_erro` e no log. |
| Timeout, conexão recusada, 429, 5xx | `ResultadoProcessamentoService` | `AGUARDANDO_PROCESSAMENTO` com backoff exponencial (30s, 1m, 2m, 4m, 8m, 10m, 10m…). Após 8 tentativas (cerca de 35 minutos): `ERRO_PROCESSAMENTO`. |
| IA fora do ar durante um ciclo | `ProcessamentoScheduler` | Encerra o ciclo na primeira falha, para não sobrecarregar a API com o resto do lote. As demais mensagens seguem na fila. |
| Aplicação cai no meio de uma chamada | `ProcessamentoScheduler` | Mensagens presas em `PROCESSANDO` há mais de 5 minutos voltam para a fila. |
| Erro inesperado | `ProcessamentoService` | É logado com stack trace e tratado como falha temporária, para não perder a mensagem. |

### Idempotência: não processar duas vezes
`AtendimentoRepository.reivindicar` executa um único `UPDATE … SET status='PROCESSANDO' WHERE id=? AND status IN ('RECEBIDO','AGUARDANDO_PROCESSAMENTO')`. O banco garante que só uma transação recebe "1 linha alterada"; as outras recebem 0 e desistem. Isso vale entre threads e também entre **várias instâncias** da aplicação. A chamada à IA acontece **fora de transação**, para não segurar uma conexão do banco durante segundos de espera por HTTP.

### Encaminhamento: `EncaminhamentoService`
Com confiança maior ou igual a `app.processamento.confianca-minima` (0,70), o atendimento vai para o departamento padrão da categoria. Abaixo disso, fica sem departamento, aguardando revisão. A tabela categoria → departamento está no enum `Categoria`.

### Revisão humana: `RevisaoService`
Cada ação confere o status atual (transição inválida retorna `409`) e a `version` que o funcionário estava vendo (alteração concorrente também retorna `409`). Tudo vai para o histórico.
- **Aprovar**: só a partir de `CLASSIFICADO`. Usa a resposta editada, se houver, ou a sugerida. Chama `EnvioRespostaService` e marca `RESPONDIDO`.
- **Rejeitar**: descarta a sugestão. A partir daí, aprovar exige uma resposta escrita pelo funcionário.
- **Reclassificar**: categoria, assunto e prioridade definidos pelo funcionário, com departamento opcional (o padrão é o da categoria).
- **Encaminhar** manualmente e **Reprocessar** (devolve à fila com o contador de tentativas zerado).

### Avaliação da qualidade: pacote `avaliacao`
- O dataset fixo (`dataset-v1.json`) tem **55 mensagens com gabarito**, incluindo casos difíceis: assunto misto, *prompt injection* e mensagem vaga.
- `AvaliacaoExecutor` roda em segundo plano usando **o mesmo** `ClassificacaoIaService` da produção, ou seja, avalia prompt e validação reais.
- `MetricasCalculator` calcula:
  - acurácia de categoria, prioridade e assunto;
  - taxa de acerto completo (os três campos certos) e taxa de erro;
  - precisão e recall por categoria;
  - matriz de confusão, com uma coluna `INVALIDA` para respostas rejeitadas.
- Cada execução registra modelo, versão do prompt e dataset. Para comparar, crie `classificacao-v2.txt`, mude `app.ia.versao-prompt` (ou troque `IA_MODELO`) e rode de novo: o painel lista as execuções lado a lado.
- Linha de base do classificador `fake`: **78% de acurácia de categoria** e 75% de assunto.

---

## API

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/mensagens` | Recebe mensagem `{clienteEmail, mensagem}` → 202 |
| GET | `/api/atendimentos?status=&page=&size=` | Lista paginada |
| GET | `/api/atendimentos/{id}` / `/historico` | Detalhe e auditoria |
| POST | `/api/atendimentos/{id}/aprovar` | `{resposta?, version}` |
| POST | `/api/atendimentos/{id}/rejeitar` | `{motivo, version}` |
| POST | `/api/atendimentos/{id}/reclassificar` | `{categoria, prioridade, assunto, departamento?, version}` |
| POST | `/api/atendimentos/{id}/encaminhar` | `{departamento, version}` |
| POST | `/api/atendimentos/{id}/reprocessar` | `{version}` |
| GET | `/api/dashboard` | Contadores e agrupamentos |
| POST / GET | `/api/avaliacoes`, `/api/avaliacoes/{id}` | Executa e consulta avaliações |

---

## Testes (cenários obrigatórios)

| # | Cenário | Teste |
|---|---|---|
| 1 | Mensagem classificada corretamente | `ProcessamentoIntegrationTest.cenario1_…` |
| 2 | IA retorna JSON inválido | `ClassificacaoValidatorTest.rejeitaJsonInvalido`, `ProcessamentoIntegrationTest.cenarios2e3_…` |
| 3 | Categoria inexistente | `ClassificacaoValidatorTest.rejeitaCategoriaInexistente`, `cenarios2e3_…` |
| 4 | IA não responde (timeout) | `OpenAiCompatibleClienteIATest.iaNaoRespondeDentroDoTimeout`, `cenarios4a6_…` |
| 5 | API indisponível | `OpenAiCompatibleClienteIATest.apiIndisponivelRetornando503`, `cenario5_…` |
| 6 | Mensagem permanece salva | `cenarios4a6_…`, `cenario6_recebimentoFuncionaComIaForaDoAr` |
| 7 | Reprocessamento posterior | `cenario7_mensagemReprocessadaQuandoIaVolta` |
| 8 | Sem processamento duplicado | `cenario8_…` (duas threads simultâneas) |
| 9 | Funcionário altera classificação | `RevisaoIntegrationTest.cenario9_…` |
| 10 | Funcionário aprova e edita | `RevisaoIntegrationTest.cenario10_…`, `funcionarioAprovaSugestaoSemEditar` |
| 11 | Departamento correto | `EncaminhamentoServiceTest.encaminhaParaODepartamentoCorreto` (5 categorias) |
| 12 | Baixa confiança | `EncaminhamentoServiceTest.baixaConfianca…`, `cenario12_…` |

Também há testes para: limite de tentativas, recuperação de mensagens presas, conflito de versão, transição inválida, rejeição, dashboard, métricas e avaliação de ponta a ponta.

---

## Limitações conhecidas e próximos passos

- **Envio simulado**: `EnvioRespostaService` só registra no log. Num envio real (e-mail), o ideal é usar uma *outbox*: gravar `APROVADO` e enviar de forma assíncrona com retry, em vez de enviar dentro da transação.
- **Sem autenticação** no painel nem identificação de qual funcionário aprovou. O próximo passo seria Spring Security e uma coluna `revisado_por`.
- **A heurística anti-invenção é limitada**: pega valores e números, mas não afirmações falsas em texto ("seu reembolso foi aprovado"). A proteção principal continua sendo o prompt somado à revisão humana obrigatória.
- **Reprodutibilidade da avaliação**: o dataset e o prompt são fixos e a temperatura é 0, mas LLMs não são 100% determinísticos, e o provedor pode atualizar o modelo por trás do mesmo nome. Por isso cada execução registra modelo e versão do prompt.
- **Comparação exata de assunto**: o catálogo fechado torna a métrica justa, mas um assunto novo exige atualizar o enum, o prompt e o dataset.
- **Erros 401 e 404 do provedor** (chave ou modelo errados) entram no mesmo backoff das falhas temporárias e aparecem como erro no log. Um *circuit breaker* poderia pausar o worker até a configuração ser corrigida.
- **Escala**: com várias instâncias, a reivindicação atômica já impede processamento duplicado. Para volumes bem maiores, o próximo passo seria `SELECT … FOR UPDATE SKIP LOCKED` ou uma fila dedicada.
