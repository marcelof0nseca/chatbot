package com.nxn.chatbot.dto;

import com.nxn.chatbot.dominio.StatusTicket;

/**
 * Uma linha da fila de atendimento, como a Tela 6 do painel exibe.
 *
 * O contato vem sempre mascarado: o telefone completo existe no dominio, mas
 * nao atravessa a fronteira da API. E minimizacao de dado pessoal na tela mais
 * exposta do painel.
 */
public record ItemDaFila(
        Long ticketId,
        String contato,
        String motivo,
        long esperaEmSegundos,
        StatusTicket status,
        String ultimaMensagem,
        String atendente) {
}
