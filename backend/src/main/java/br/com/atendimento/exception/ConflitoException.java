package br.com.atendimento.exception;

/** 409: transição de status não permitida, versão desatualizada ou operação já em andamento. */
public class ConflitoException extends RuntimeException {
    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
