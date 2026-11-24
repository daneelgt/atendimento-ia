import { useState } from 'react'
import Dashboard from './components/Dashboard.jsx'
import Atendimentos from './components/Atendimentos.jsx'
import Avaliacoes from './components/Avaliacoes.jsx'
import NovaMensagem from './components/NovaMensagem.jsx'

const ABAS = { dashboard: 'Painel', atendimentos: 'Atendimentos', avaliacoes: 'Avaliação da IA', nova: 'Simular mensagem' }

export default function App() {
  const [aba, setAba] = useState('dashboard')
  const [filtroInicial, setFiltroInicial] = useState('')

  const abrirAtendimentos = (status) => {
    setFiltroInicial(status ?? '')
    setAba('atendimentos')
  }

  return (
    <>
      <header className="topo">
        <strong>Atendimento IA</strong>
        <nav>
          {Object.entries(ABAS).map(([k, v]) => (
            <button key={k} className={aba === k ? 'ativa' : ''} onClick={() => setAba(k)}>{v}</button>
          ))}
        </nav>
      </header>
      <main>
        {aba === 'dashboard' && <Dashboard onFiltrar={abrirAtendimentos} />}
        {aba === 'atendimentos' && <Atendimentos key={filtroInicial} filtroInicial={filtroInicial} />}
        {aba === 'avaliacoes' && <Avaliacoes />}
        {aba === 'nova' && <NovaMensagem onEnviada={() => abrirAtendimentos('')} />}
      </main>
    </>
  )
}
