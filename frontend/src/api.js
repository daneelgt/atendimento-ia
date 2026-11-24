async function request(path, options = {}) {
  const resp = await fetch(`/api${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
    body: options.body ? JSON.stringify(options.body) : undefined,
  })
  const data = resp.status === 204 ? null : await resp.json().catch(() => null)
  if (!resp.ok) {
    const campos = data?.campos ? ' ' + Object.entries(data.campos).map(([k, v]) => `${k}: ${v}`).join('; ') : ''
    throw new Error((data?.mensagem ?? `Erro HTTP ${resp.status}`) + campos)
  }
  return data
}

export const api = {
  dashboard: () => request('/dashboard'),
  listar: (status, page = 0) =>
    request(`/atendimentos?page=${page}&size=15${status ? `&status=${status}` : ''}`),
  buscar: (id) => request(`/atendimentos/${id}`),
  historico: (id) => request(`/atendimentos/${id}/historico`),
  acao: (id, acao, body) => request(`/atendimentos/${id}/${acao}`, { method: 'POST', body }),
  enviarMensagem: (body) => request('/mensagens', { method: 'POST', body }),
  avaliacoes: () => request('/avaliacoes'),
  avaliacao: (id) => request(`/avaliacoes/${id}`),
  iniciarAvaliacao: () => request('/avaliacoes', { method: 'POST' }),
}
