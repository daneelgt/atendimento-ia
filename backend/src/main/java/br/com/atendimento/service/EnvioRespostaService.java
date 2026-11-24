package br.com.atendimento.service;

import br.com.atendimento.entity.Atendimento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Ponto de extensão para o envio real (e-mail, WhatsApp...). Neste projeto o envio é simulado.
 * Só é chamado pelo RevisaoService.aprovar, ou seja, apenas após aprovação humana.
 */
@Service
public class EnvioRespostaService {

    private static final Logger log = LoggerFactory.getLogger(EnvioRespostaService.class);

    public void enviar(Atendimento atendimento) {
        log.info("[ENVIO SIMULADO] Para: {} | Atendimento {} | Resposta: {}",
                atendimento.getClienteEmail(), atendimento.getId(), atendimento.getRespostaFinal());
    }
}
