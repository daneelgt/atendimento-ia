package br.com.atendimento.controller;

import br.com.atendimento.dto.AtendimentoResponse;
import br.com.atendimento.dto.RevisaoRequests;
import br.com.atendimento.service.RevisaoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/atendimentos/{id}")
public class RevisaoController {

    private final RevisaoService revisao;

    public RevisaoController(RevisaoService revisao) {
        this.revisao = revisao;
    }

    @PostMapping("/aprovar")
    public AtendimentoResponse aprovar(@PathVariable Long id, @Valid @RequestBody RevisaoRequests.Aprovar req) {
        return AtendimentoResponse.de(revisao.aprovar(id, req));
    }

    @PostMapping("/rejeitar")
    public AtendimentoResponse rejeitar(@PathVariable Long id, @Valid @RequestBody RevisaoRequests.Rejeitar req) {
        return AtendimentoResponse.de(revisao.rejeitar(id, req));
    }

    @PostMapping("/reclassificar")
    public AtendimentoResponse reclassificar(@PathVariable Long id, @Valid @RequestBody RevisaoRequests.Reclassificar req) {
        return AtendimentoResponse.de(revisao.reclassificar(id, req));
    }

    @PostMapping("/encaminhar")
    public AtendimentoResponse encaminhar(@PathVariable Long id, @Valid @RequestBody RevisaoRequests.Encaminhar req) {
        return AtendimentoResponse.de(revisao.encaminhar(id, req));
    }

    @PostMapping("/reprocessar")
    public AtendimentoResponse reprocessar(@PathVariable Long id, @Valid @RequestBody RevisaoRequests.Reprocessar req) {
        return AtendimentoResponse.de(revisao.reprocessar(id, req));
    }
}
