package br.com.atendimento.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NovaMensagemRequest(
        @NotBlank @Email @Size(max = 255) String clienteEmail,
        @NotBlank @Size(max = 5000) String mensagem) {
}
