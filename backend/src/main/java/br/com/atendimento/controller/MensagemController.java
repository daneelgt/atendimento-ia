package br.com.atendimento.controller;

import br.com.atendimento.dto.MensagemRecebidaResponse;
import br.com.atendimento.dto.NovaMensagemRequest;
import br.com.atendimento.entity.Atendimento;
import br.com.atendimento.service.RecebimentoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/mensagens")
public class MensagemController {

    private final RecebimentoService recebimentoService;

    public MensagemController(RecebimentoService recebimentoService) {
        this.recebimentoService = recebimentoService;
    }

    /** 202 Accepted: a mensagem foi aceita e salva; a classificação acontece depois. */
    @PostMapping
    public ResponseEntity<MensagemRecebidaResponse> receber(@Valid @RequestBody NovaMensagemRequest request) {
        Atendimento atendimento = recebimentoService.receber(request);
        return ResponseEntity.accepted()
                .location(URI.create("/api/atendimentos/" + atendimento.getId()))
                .body(MensagemRecebidaResponse.de(atendimento));
    }
}
