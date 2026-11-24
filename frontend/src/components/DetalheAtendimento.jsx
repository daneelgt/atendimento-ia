import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { STATUS, DEPARTAMENTOS, PRIORIDADES, ASSUNTOS, fmtData, fmtPct } from '../dominio.js'

const EDITAVEIS = ['CLASSIFICADO', 'AGUARDANDO_REVISAO', 'CLASSIFICACAO_MANUAL', 'ERRO_PROCESSAMENTO']

export default function DetalheAtendimento({ id, onFechar, onAlterado }) {
  const [a, setA] = useState(null)
  const [historico, setHistorico] = useState([])
  const [resposta, setResposta] = useState('')
  const [classif, setClassif] = useState({ categoria: '', prioridade: '', assunto: '', departamento: '' })
  const [departamento, setDepartamento] = useState('')
  const [msg, setMsg] = useState(null)

  const carregar = () =>
    Promise.all([api.buscar(id), api.historico(id)]).then(([at, h]) => {
      setA(at)
      setHistorico(h)
      setResposta(at.respostaSugerida ?? '')
      setClassif({ categoria: at.categoria ?? '', prioridade: at.prioridade ?? '', assunto: at.assunto ?? '', departamento: '' })
      setDepartamento(at.departamento ?? '')
    })

  useEffect(() => { setMsg(null); carregar() }, [id])

  const executar = async (acao, body, sucesso) => {
    setMsg(null)
    try {
      await api.acao(id, acao, { ...body, version: a.version })
      setMsg({ ok: true, texto: sucesso })
      await carregar()
      onAlterado()
    } catch (e) {
      setMsg({ ok: false, texto: e.message })
    }
  }

  if (!a) return <aside className="cartao detalhe">Carregando…</aside>

  const podeEditar = EDITAVEIS.includes(a.status)
  const podeAprovar = a.status === 'CLASSIFICADO'
  const respostaEditada = resposta.trim() !== (a.respostaSugerida ?? '').trim()

  return (
    <aside className="cartao detalhe">
      <div className="detalhe-topo">
        <h3>Atendimento #{a.id}</h3>
        <button onClick={onFechar}>Fechar</button>
      </div>
      {msg && <p className={msg.ok ? 'sucesso' : 'erro'}>{msg.texto}</p>}

      <dl>
        <dt>Status</dt><dd><span className={`status ${a.status}`}>{STATUS[a.status]}</span></dd>
        <dt>Cliente</dt><dd>{a.clienteEmail}</dd>
        <dt>Categoria</dt><dd>{a.categoria ?? '—'} {a.origemClassificacao && <small>({a.origemClassificacao})</small>}</dd>
        <dt>Assunto</dt><dd>{a.assunto ?? '—'}</dd>
        <dt>Prioridade</dt><dd>{a.prioridade ?? '—'}</dd>
        <dt>Departamento</dt><dd>{DEPARTAMENTOS[a.departamento] ?? '—'}</dd>
        <dt>Confiança</dt><dd>{fmtPct(a.confianca)}</dd>
        <dt>Tentativas</dt><dd>{a.tentativas}</dd>
        {a.ultimoErro && <><dt>Último erro</dt><dd className="erro-texto">{a.ultimoErro}</dd></>}
      </dl>

      <h4>Mensagem do cliente</h4>
      <blockquote>{a.mensagem}</blockquote>

      {a.respostaFinal ? (
        <><h4>Resposta enviada</h4><blockquote>{a.respostaFinal}</blockquote></>
      ) : (
        <>
          <h4>Resposta {a.respostaSugerida ? 'sugerida pela IA' : '(escreva a resposta)'}</h4>
          <textarea rows="5" value={resposta} onChange={(e) => setResposta(e.target.value)} disabled={!podeAprovar} />
          <div className="acoes">
            <button className="primario" disabled={!podeAprovar}
              onClick={() => executar('aprovar', { resposta: respostaEditada ? resposta : null },
                respostaEditada ? 'Resposta editada, aprovada e enviada.' : 'Resposta aprovada e enviada.')}>
              {respostaEditada ? 'Aprovar edição e enviar' : 'Aprovar e enviar'}
            </button>
            <button disabled={!a.respostaSugerida || !['CLASSIFICADO', 'AGUARDANDO_REVISAO'].includes(a.status)}
              onClick={() => {
                const motivo = prompt('Motivo da rejeição:')
                if (motivo) executar('rejeitar', { motivo }, 'Sugestão rejeitada.')
              }}>Rejeitar sugestão</button>
          </div>
          {!podeAprovar && <p className="dica">A aprovação fica liberada quando o atendimento está encaminhado a um departamento.</p>}
        </>
      )}

      {podeEditar && (
        <>
          <h4>{a.status === 'CLASSIFICADO' ? 'Alterar classificação' : 'Classificar manualmente'}</h4>
          <div className="form-linha">
            <select value={classif.categoria} onChange={(e) => setClassif({ ...classif, categoria: e.target.value, assunto: '' })}>
              <option value="">Categoria</option>
              {Object.keys(ASSUNTOS).map((c) => <option key={c}>{c}</option>)}
            </select>
            <select value={classif.assunto} onChange={(e) => setClassif({ ...classif, assunto: e.target.value })}>
              <option value="">Assunto</option>
              {(ASSUNTOS[classif.categoria] ?? []).map((s) => <option key={s}>{s}</option>)}
            </select>
            <select value={classif.prioridade} onChange={(e) => setClassif({ ...classif, prioridade: e.target.value })}>
              <option value="">Prioridade</option>
              {PRIORIDADES.map((p) => <option key={p}>{p}</option>)}
            </select>
            <select value={classif.departamento} onChange={(e) => setClassif({ ...classif, departamento: e.target.value })}>
              <option value="">Departamento padrão</option>
              {Object.entries(DEPARTAMENTOS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </select>
            <button disabled={!classif.categoria || !classif.assunto || !classif.prioridade}
              onClick={() => executar('reclassificar', { ...classif, departamento: classif.departamento || null },
                'Classificação salva e atendimento encaminhado.')}>Salvar</button>
          </div>

          <h4>Encaminhar manualmente</h4>
          <div className="form-linha">
            <select value={departamento} onChange={(e) => setDepartamento(e.target.value)}>
              <option value="">Departamento</option>
              {Object.entries(DEPARTAMENTOS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </select>
            <button disabled={!departamento}
              onClick={() => executar('encaminhar', { departamento }, 'Atendimento encaminhado.')}>Encaminhar</button>
            <button onClick={() => executar('reprocessar', {}, 'Atendimento devolvido à fila da IA.')}>Reprocessar com IA</button>
          </div>
        </>
      )}

      <h4>Histórico</h4>
      <ul className="historico">
        {historico.map((h, i) => (
          <li key={i}><small>{fmtData(h.criadoEm)}</small> <b>{STATUS[h.statusNovo]}</b> — {h.descricao}</li>
        ))}
      </ul>
    </aside>
  )
}
