package com.wxn.chatbot.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wxn.chatbot.dto.ItemDaFila;
import com.wxn.chatbot.dto.PedidoDeTransferencia;
import com.wxn.chatbot.dto.ResumoDaFila;
import com.wxn.chatbot.servico.FilaDeConversasService;

/**
 * API da fila: a Tela 6 lista e assume; a Tela 7 transfere. Os erros (404,
 * 400 e 409) sao traduzidos em ErrosDaApi, que vale para todos os controllers.
 */
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

    /**
     * Passa o caso a outro atendente, com o motivo. Responde o item da fila
     * ja com o novo atendente.
     */
    @PostMapping("/{ticketId}/transferir")
    public ItemDaFila transferir(@PathVariable Long ticketId, @RequestBody PedidoDeTransferencia pedido) {
        return servico.transferir(ticketId, pedido.quemTransfereId(), pedido.destinoId(), pedido.motivo());
    }
}
