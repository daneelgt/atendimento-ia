package br.com.atendimento.validation;

import java.util.List;

public class RespostaIaInvalidaException extends RuntimeException {

    private final List<String> erros;

    public RespostaIaInvalidaException(List<String> erros) {
        super("Resposta da IA inválida: " + String.join("; ", erros));
        this.erros = List.copyOf(erros);
    }

    public List<String> getErros() {
        return erros;
    }
}
