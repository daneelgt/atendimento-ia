export const STATUS = {
  RECEBIDO: 'Recebido',
  AGUARDANDO_PROCESSAMENTO: 'Aguardando processamento',
  PROCESSANDO: 'Processando',
  CLASSIFICADO: 'Encaminhado',
  AGUARDANDO_REVISAO: 'Aguardando revisão',
  APROVADO: 'Aprovado',
  RESPONDIDO: 'Respondido',
  ERRO_PROCESSAMENTO: 'Erro de processamento',
  CLASSIFICACAO_MANUAL: 'Classificação manual',
}

export const DEPARTAMENTOS = {
  FINANCEIRO: 'Financeiro',
  SUPORTE_TECNICO: 'Suporte técnico',
  RETENCAO: 'Retenção/Cancelamento',
  COMERCIAL: 'Comercial',
  ATENDIMENTO_GERAL: 'Atendimento geral',
}

export const PRIORIDADES = ['BAIXA', 'MEDIA', 'ALTA', 'URGENTE']

export const ASSUNTOS = {
  FINANCEIRO: ['COBRANCA_DUPLICADA', 'COBRANCA_INDEVIDA', 'REEMBOLSO', 'SEGUNDA_VIA', 'ALTERACAO_PAGAMENTO'],
  SUPORTE_TECNICO: ['ACESSO_CONTA', 'CONTA_INVADIDA', 'ERRO_SISTEMA', 'LENTIDAO', 'SERVICO_INDISPONIVEL'],
  CANCELAMENTO: ['CANCELAMENTO_ASSINATURA'],
  COMERCIAL: ['INFORMACAO_PLANOS', 'UPGRADE_PLANO', 'PROPOSTA_EMPRESARIAL'],
  OUTROS: ['ELOGIO', 'SUGESTAO', 'DUVIDA_GERAL'],
}

export const fmtData = (iso) => (iso ? new Date(iso).toLocaleString('pt-BR') : '—')
export const fmtPct = (v) => (v == null ? '—' : `${(Number(v) * 100).toFixed(1)}%`)
