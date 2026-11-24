package br.com.atendimento.service;

import br.com.atendimento.ai.ClassificacaoIA;
import br.com.atendimento.ai.ClienteIA;
import br.com.atendimento.ai.PromptBuilder;
import br.com.atendimento.validation.ClassificacaoValidator;
import br.com.atendimento.validation.RespostaIaInvalidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Pede a classificação à IA e valida. Se a resposta for inválida, faz UMA nova tentativa informando
 * os erros à IA (auto-correção controlada). Se continuar inválida, propaga RespostaIaInvalidaException.
 * IaIndisponivelException não é tratada aqui: quem decide o backoff é o processamento.
 */
@Service
public class ClassificacaoIaService {

    private static final Logger log = LoggerFactory.getLogger(ClassificacaoIaService.class);

    private final ClienteIA cliente;
    private final PromptBuilder prompt;
    private final ClassificacaoValidator validator;

    public ClassificacaoIaService(ClienteIA cliente, PromptBuilder prompt, ClassificacaoValidator validator) {
        this.cliente = cliente;
        this.prompt = prompt;
        this.validator = validator;
    }

    public ClassificacaoIA classificar(String mensagem) {
        String bruto = cliente.completar(prompt.instrucoesSistema(), prompt.conteudoUsuario(mensagem, null));
        try {
            return validator.validar(bruto, mensagem);
        } catch (RespostaIaInvalidaException e) {
            log.warn("Resposta inválida da IA, tentando correção. Erros: {} | Resposta: {}", e.getErros(), abreviar(bruto));
            String corrigido = cliente.completar(prompt.instrucoesSistema(), prompt.conteudoUsuario(mensagem, e.getErros()));
            try {
                return validator.validar(corrigido, mensagem);
            } catch (RespostaIaInvalidaException e2) {
                log.warn("Resposta da IA continua inválida após correção. Erros: {} | Resposta: {}",
                        e2.getErros(), abreviar(corrigido));
                throw e2;
            }
        }
    }

    public String modelo() {
        return cliente.modelo();
    }

    public String versaoPrompt() {
        return prompt.versao();
    }

    private static String abreviar(String s) {
        return s == null ? "null" : s.substring(0, Math.min(300, s.length()));
    }
}
