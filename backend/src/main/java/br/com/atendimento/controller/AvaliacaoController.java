package br.com.atendimento.controller;

import br.com.atendimento.avaliacao.AvaliacaoService;
import br.com.atendimento.dto.AvaliacaoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/avaliacoes")
public class AvaliacaoController {

    private final AvaliacaoService avaliacao;

    public AvaliacaoController(AvaliacaoService avaliacao) {
        this.avaliacao = avaliacao;
    }

    @PostMapping
    public ResponseEntity<AvaliacaoResponse> iniciar() {
        return ResponseEntity.accepted().body(AvaliacaoResponse.resumo(avaliacao.iniciar()));
    }

    @GetMapping
    public List<AvaliacaoResponse> listar() {
        return avaliacao.listar().stream().map(AvaliacaoResponse::resumo).toList();
    }

    @GetMapping("/{id}")
    public AvaliacaoResponse buscar(@PathVariable Long id) {
        return AvaliacaoResponse.completo(avaliacao.buscar(id));
    }
}
