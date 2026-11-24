import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { DEPARTAMENTOS } from '../dominio.js'

function Barras({ titulo, dados, rotulos = {} }) {
  const max = Math.max(1, ...Object.values(dados))
  return (
    <section className="cartao">
      <h3>{titulo}</h3>
      {Object.entries(dados).map(([k, v]) => (
        <div key={k} className="barra-linha">
          <span>{rotulos[k] ?? k}</span>
          <div className="barra"><div style={{ width: `${(v / max) * 100}%` }} /></div>
          <b>{v}</b>
        </div>
      ))}
    </section>
  )
}

export default function Dashboard({ onFiltrar }) {
  const [d, setD] = useState(null)
  const [erro, setErro] = useState('')

  useEffect(() => {
    const carregar = () => api.dashboard().then(setD).catch((e) => setErro(e.message))
    carregar()
    const t = setInterval(carregar, 5000)
    return () => clearInterval(t)
  }, [])

  if (erro) return <p className="erro">{erro}</p>
  if (!d) return <p>Carregando…</p>

  const kpis = [
    ['Total recebidas', d.total, ''],
    ['Pendentes (fila da IA)', d.pendentes, 'AGUARDANDO_PROCESSAMENTO'],
    ['Aguardando revisão', d.aguardandoRevisao, 'AGUARDANDO_REVISAO'],
    ['Encaminhados (resposta a revisar)', d.encaminhados, 'CLASSIFICADO'],
    ['Respondidos', d.respondidos, 'RESPONDIDO'],
    ['Erro de processamento', d.erroProcessamento, 'ERRO_PROCESSAMENTO'],
    ['Classificação manual', d.classificacaoManual, 'CLASSIFICACAO_MANUAL'],
  ]

  return (
    <>
      <div className="kpis">
        {kpis.map(([rotulo, valor, status]) => (
          <button key={rotulo} className="kpi" onClick={() => onFiltrar(status)}>
            <span>{rotulo}</span>
            <b>{valor}</b>
          </button>
        ))}
      </div>
      <div className="grade-2">
        <Barras titulo="Por departamento" dados={d.porDepartamento} rotulos={DEPARTAMENTOS} />
        <Barras titulo="Por prioridade" dados={d.porPrioridade} />
      </div>
      <p className="dica">Atualiza a cada 5 segundos. Clique em um indicador para ver a lista filtrada.</p>
    </>
  )
}
