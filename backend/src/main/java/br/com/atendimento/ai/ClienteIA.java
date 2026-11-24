package br.com.atendimento.ai;

/**
 * Fronteira com o provedor de IA. Devolve o texto bruto: quem interpreta e valida é o Java.
 * Lança IaIndisponivelException para falhas transitórias (timeout, rede, 429, 5xx).
 */
public interface ClienteIA {

    String completar(String instrucoesSistema, String conteudoUsuario);

    String modelo();
}
