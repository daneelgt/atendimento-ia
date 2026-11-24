import { useCallback, useEffect, useState } from 'react'
import { api } from '../api.js'
import { STATUS, DEPARTAMENTOS, fmtData, fmtPct } from '../dominio.js'
import DetalheAtendimento from './DetalheAtendimento.jsx'

export default function Atendimentos({ filtroInicial }) {
  const [status, setStatus] = useState(filtroInicial)
  const [pagina, setPagina] = useState(0)
  const [dados, setDados] = useState(null)
  const [selecionado, setSelecionado] = useState(null)
  const [erro, setErro] = useState('')

  const carregar = useCallback(() => {
    api.listar(status, pagina).then(setDados).catch((e) => setErro(e.message))
  }, [status, pagina])

  useEffect(() => { carregar() }, [carregar])

  return (
    <div className={selecionado ? 'layout-detalhe' : ''}>
      <section className="cartao">
        <div className="filtros">
          <label>
            Status{' '}
            <select value={status} onChange={(e) => { setStatus(e.target.value); setPagina(0) }}>
              <option value="">Todos</option>
              {Object.entries(STATUS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </select>
          </label>
          <button onClick={carregar}>Atualizar</button>
        </div>
        {erro && <p className="erro">{erro}</p>}
        <table>
          <thead>
            <tr><th>#</th><th>Mensagem</th><th>Categoria</th><th>Prioridade</th><th>Departamento</th><th>Confiança</th><th>Status</th><th>Recebido</th></tr>
          </thead>
          <tbody>
            {dados?.content.map((a) => (
              <tr key={a.id} onClick={() => setSelecionado(a.id)} className={selecionado === a.id ? 'sel' : ''}>
                <td>{a.id}</td>
                <td className="trunc">{a.mensagem}</td>
                <td>{a.categoria ?? '—'}</td>
                <td>{a.prioridade ? <span className={`prio ${a.prioridade}`}>{a.prioridade}</span> : '—'}</td>
                <td>{DEPARTAMENTOS[a.departamento] ?? '—'}</td>
                <td>{fmtPct(a.confianca)}</td>
                <td><span className={`status ${a.status}`}>{STATUS[a.status]}</span></td>
                <td>{fmtData(a.criadoEm)}</td>
              </tr>
            ))}
            {dados?.content.length === 0 && <tr><td colSpan="8">Nenhum atendimento.</td></tr>}
          </tbody>
        </table>
        {dados && (
          <div className="paginacao">
            <button disabled={pagina === 0} onClick={() => setPagina(pagina - 1)}>Anterior</button>
            <span>Página {pagina + 1} de {Math.max(1, dados.page.totalPages)} ({dados.page.totalElements} itens)</span>
            <button disabled={pagina + 1 >= dados.page.totalPages} onClick={() => setPagina(pagina + 1)}>Próxima</button>
          </div>
        )}
      </section>
      {selecionado && (
        <DetalheAtendimento id={selecionado} onFechar={() => setSelecionado(null)} onAlterado={carregar} />
      )}
    </div>
  )
}
