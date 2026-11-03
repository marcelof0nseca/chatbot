package com.wxn.chatbot.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wxn.chatbot.dto.AtendenteResumo;
import com.wxn.chatbot.dto.ConversaDoAtendimento;
import com.wxn.chatbot.servico.AtendimentoService;

/** API que a Tela 7 consome, mais a lista da equipe para o painel. */
@RestController
@RequestMapping("/api")
public class AtendimentoController {

    private final AtendimentoService servico;

    public AtendimentoController(AtendimentoService servico) {
        this.servico = servico;
    }

    @GetMapping("/fila/{ticketId}/conversa")
    public ConversaDoAtendimento conversa(@PathVariable Long ticketId) {
        return servico.conversaDoTicket(ticketId);
    }

    @GetMapping("/atendentes")
    public List<AtendenteResumo> atendentes() {
        return servico.atendentes();
    }
}
