package com.wxn.chatbot.dto;

/**
 * Corpo do POST /api/simulador/mensagens: o que chega quando um cliente
 * escreve. nome so e usado no primeiro contato, para cadastrar o cliente.
 */
public record MensagemDoCliente(
        String telefone,
        String nome,
        String texto) {
}
