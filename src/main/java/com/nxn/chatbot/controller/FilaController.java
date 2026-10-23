package com.nxn.chatbot.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nxn.chatbot.dto.ItemDaFila;
import com.nxn.chatbot.dto.ResumoDaFila;
import com.nxn.chatbot.servico.FilaDeConversasService;

/** API que a Tela 6 do painel consome. */
@RestController
@RequestMapping("/api/fila")
public class FilaController {

    private final FilaDeConversasService servico;

    public FilaController(FilaDeConversasService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<ItemDaFila> fila() {
        return servico.listarFila();
    }

    @GetMapping("/resumo")
    public ResumoDaFila resumo() {
        return servico.resumoDoDia();
    }

    @PostMapping("/{ticketId}/assumir")
    public ItemDaFila assumir(@PathVariable Long ticketId, @RequestParam Long atendenteId) {
        return servico.assumir(ticketId, atendenteId);
    }

    /** Ticket ou atendente que nao existe. */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> naoEncontrado(IllegalArgumentException erro) {
        return Map.of("erro", erro.getMessage());
    }

    /** Regra do dominio recusou: ticket ja assumido, atendente fora de servico. */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> conflito(IllegalStateException erro) {
        return Map.of("erro", erro.getMessage());
    }
}
