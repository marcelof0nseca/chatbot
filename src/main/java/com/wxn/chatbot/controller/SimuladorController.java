package com.wxn.chatbot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wxn.chatbot.dto.ConversaDoCliente;
import com.wxn.chatbot.dto.MensagemDoCliente;
import com.wxn.chatbot.servico.CanalDoClienteService;

/**
 * O lado do cliente, enquanto o WhatsApp nao esta integrado: a pagina
 * simulador.html do painel faz o papel do celular. O webhook do WhatsApp,
 * quando vier, sera outro controller chamando o mesmo CanalDoClienteService.
 */
@RestController
@RequestMapping("/api/simulador")
public class SimuladorController {

    private final CanalDoClienteService canal;

    public SimuladorController(CanalDoClienteService canal) {
        this.canal = canal;
    }

    @PostMapping("/mensagens")
    public ConversaDoCliente enviar(@RequestBody MensagemDoCliente mensagem) {
        return canal.receberMensagem(mensagem.telefone(), mensagem.nome(), mensagem.texto());
    }

    @GetMapping("/conversa")
    public ConversaDoCliente conversa(@RequestParam String telefone) {
        return canal.conversaAtual(telefone);
    }
}
