import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { fmtData, fmtPct } from '../dominio.js'

export default function Avaliacoes() {
  const [lista, setLista] = useState([])
  const [detalhe, setDetalhe] = useState(null)
  const [erro, setErro] = useState('')

  const carregar = () => api.avaliacoes().then(setLista).catch((e) => setErro(e.message))

  useEffect(() => {
    carregar()
    const t = setInterval(carregar, 3000)
    return () => clearInterval(t)
  }, [])

  const iniciar = () => { setErro(''); api.iniciarAvaliacao().then(carregar).catch((e) => setErro(e.message)) }

  return (
    <>
      <section className="cartao">
        <div className="filtros">
          <h3>Execuções (mesmo dataset, para comparar prompts e modelos)</h3>
          <button className="primario" onClick={iniciar}>Executar avaliação</button>
        </div>
        {erro && <p className="erro">{erro}</p>}
        <table>
          <thead>
            <tr><th>#</th><th>Início</th><th>Modelo</th><th>Prompt</th><th>Status</th><th>Categoria</th><th>Prioridade</th><th>Assunto</th><th>Inválidas</th><th></th></tr>
          </thead>
          <tbody>
            {lista.map((e) => (
              <tr key={e.id}>
                <td>{e.id}</td><td>{fmtData(e.iniciadoEm)}</td><td>{e.modelo}</td><td>{e.versaoPrompt}</td>
                <td>{e.status}{e.erro && <div className="erro-texto">{e.erro}</div>}</td>
                <td>{fmtPct(e.acuraciaCategoria)}</td><td>{fmtPct(e.acuraciaPrioridade)}</td><td>{fmtPct(e.acuraciaAssunto)}</td>
                <td>{e.respostasInvalidas ?? '—'}</td>
                <td>{e.status === 'CONCLUIDA' && <button onClick={() => api.avaliacao(e.id).then(setDetalhe)}>Detalhes</button>}</td>
              </tr>
            ))}
            {lista.length === 0 && <tr><td colSpan="10">Nenhuma avaliação executada.</td></tr>}
          </tbody>
        </table>
      </section>
      {detalhe && <DetalheAvaliacao e={detalhe} />}
    </>
  )
}

function DetalheAvaliacao({ e }) {
  const m = e.metricas
  const colunas = Object.keys(Object.values(m.matrizConfusao)[0])
  const erros = m.casos.filter((c) => !c.acertoCompleto)
  return (
    <>
      <div className="kpis">
        <div className="kpi"><span>Taxa de acerto (3 campos)</span><b>{fmtPct(m.taxaAcerto)}</b></div>
        <div className="kpi"><span>Taxa de erro</span><b>{fmtPct(m.taxaErro)}</b></div>
        <div className="kpi"><span>Acurácia categoria</span><b>{fmtPct(m.acuraciaCategoria)}</b></div>
        <div className="kpi"><span>Acurácia prioridade</span><b>{fmtPct(m.acuraciaPrioridade)}</b></div>
        <div className="kpi"><span>Acurácia assunto</span><b>{fmtPct(m.acuraciaAssunto)}</b></div>
      </div>
      <div className="grade-2">
        <section className="cartao">
          <h3>Precisão e recall por categoria</h3>
          <table>
            <thead><tr><th>Categoria</th><th>Esperados</th><th>Previstos</th><th>Acertos</th><th>Precisão</th><th>Recall</th></tr></thead>
            <tbody>
              {Object.entries(m.porCategoria).map(([c, v]) => (
                <tr key={c}><td>{c}</td><td>{v.esperados}</td><td>{v.previstos}</td><td>{v.acertos}</td><td>{fmtPct(v.precisao)}</td><td>{fmtPct(v.recall)}</td></tr>
              ))}
            </tbody>
          </table>
        </section>
        <section className="cartao">
          <h3>Matriz de confusão (linha = esperado, coluna = retornado)</h3>
          <table className="matriz">
            <thead><tr><th></th>{colunas.map((c) => <th key={c}>{c.slice(0, 6)}</th>)}</tr></thead>
            <tbody>
              {Object.entries(m.matrizConfusao).map(([linha, cols]) => (
                <tr key={linha}>
                  <th>{linha}</th>
                  {colunas.map((c) => <td key={c} className={cols[c] ? (c === linha ? 'certo' : 'errado') : ''}>{cols[c]}</td>)}
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      </div>
      <section className="cartao">
        <h3>Casos com erro ({erros.length})</h3>
        <table>
          <thead><tr><th>Mensagem</th><th>Categoria (esp. / ret.)</th><th>Prioridade</th><th>Assunto</th></tr></thead>
          <tbody>
            {erros.map((c, i) => (
              <tr key={i}>
                <td className="trunc">{c.mensagem}{c.erro && <div className="erro-texto">{c.erro}</div>}</td>
                <td className={c.categoriaEsperada === c.categoriaRetornada ? '' : 'errado'}>{c.categoriaEsperada} / {c.categoriaRetornada ?? '—'}</td>
                <td className={c.prioridadeEsperada === c.prioridadeRetornada ? '' : 'errado'}>{c.prioridadeEsperada} / {c.prioridadeRetornada ?? '—'}</td>
                <td className={c.assuntoEsperado === c.assuntoRetornado ? '' : 'errado'}>{c.assuntoEsperado} / {c.assuntoRetornado ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </>
  )
}
