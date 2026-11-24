package br.com.atendimento.dto;

import br.com.atendimento.entity.Assunto;
import br.com.atendimento.entity.Categoria;
import br.com.atendimento.entity.Departamento;
import br.com.atendimento.entity.Prioridade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Requisições das ações do funcionário. Todas levam a "version" que o funcionário estava vendo:
 * se o atendimento mudou nesse meio tempo, a ação é recusada com 409.
 */
public final class RevisaoRequests {

    private RevisaoRequests() {
    }

    public record Aprovar(@Size(max = 5000) String resposta, @NotNull Long version) {
    }

    public record Rejeitar(@NotBlank @Size(max = 500) String motivo, @NotNull Long version) {
    }

    /** departamento é opcional: se ausente, usa o departamento padrão da categoria. */
    public record Reclassificar(@NotNull Categoria categoria, @NotNull Prioridade prioridade,
                                @NotNull Assunto assunto, Departamento departamento, @NotNull Long version) {
    }

    public record Encaminhar(@NotNull Departamento departamento, @NotNull Long version) {
    }

    public record Reprocessar(@NotNull Long version) {
    }
}
