import { useState } from 'react'
import { api } from '../api.js'

const EXEMPLOS = [
  'Fui cobrado duas vezes na minha fatura.',
  'Não consigo entrar na minha conta.',
  'Quero cancelar minha assinatura.',
  'Gostaria de solicitar um reembolso.',
  'Estou com problemas para acessar o serviço.',
]

/** Simula o canal do cliente (em produção seria um formulário do site, e-mail, etc.). */
export default function NovaMensagem({ onEnviada }) {
  const [email, setEmail] = useState('cliente@exemplo.com')
  const [mensagem, setMensagem] = useState('')
  const [msg, setMsg] = useState(null)

  const enviar = async (e) => {
    e.preventDefault()
    try {
      const r = await api.enviarMensagem({ clienteEmail: email, mensagem })
      setMsg({ ok: true, texto: `Mensagem #${r.id} recebida e salva. A IA vai classificá-la em segundos.` })
      setMensagem('')
    } catch (err) {
      setMsg({ ok: false, texto: err.message })
    }
  }

  return (
    <section className="cartao estreito">
      <h3>Simular mensagem de cliente</h3>
      {msg && <p className={msg.ok ? 'sucesso' : 'erro'}>{msg.texto}</p>}
      <form onSubmit={enviar}>
        <label>E-mail<input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required /></label>
        <label>Mensagem<textarea rows="4" value={mensagem} onChange={(e) => setMensagem(e.target.value)} required /></label>
        <div className="acoes">
          <button className="primario" type="submit">Enviar</button>
          <button type="button" onClick={onEnviada}>Ver atendimentos</button>
        </div>
      </form>
      <p className="dica">Exemplos:</p>
      <div className="exemplos">
        {EXEMPLOS.map((x) => <button key={x} onClick={() => setMensagem(x)}>{x}</button>)}
      </div>
    </section>
  )
}
