package br.com.atendimento.controller;

import br.com.atendimento.dto.AtendimentoResponse;
import br.com.atendimento.dto.HistoricoResponse;
import br.com.atendimento.entity.StatusAtendimento;
import br.com.atendimento.service.ConsultaAtendimentoService;
import br.com.atendimento.service.HistoricoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/atendimentos")
public class AtendimentoController {

    private final ConsultaAtendimentoService consulta;
    private final HistoricoService historico;

    public AtendimentoController(ConsultaAtendimentoService consulta, HistoricoService historico) {
        this.consulta = consulta;
        this.historico = historico;
    }

    @GetMapping
    public Page<AtendimentoResponse> listar(
            @RequestParam(required = false) StatusAtendimento status,
            @PageableDefault(size = 20, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return consulta.listar(status, pageable).map(AtendimentoResponse::de);
    }

    @GetMapping("/{id}")
    public AtendimentoResponse buscar(@PathVariable Long id) {
        return AtendimentoResponse.de(consulta.buscar(id));
    }

    @GetMapping("/{id}/historico")
    public List<HistoricoResponse> historico(@PathVariable Long id) {
        consulta.buscar(id);
        return historico.listar(id).stream().map(HistoricoResponse::de).toList();
    }
}
