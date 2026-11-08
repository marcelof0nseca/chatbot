package com.wxn.chatbot.dto;

/**
 * Corpo do POST /api/fila/{ticketId}/transferir.
 *
 * quemTransfereId e o atendente logado no painel: o proprio atendente do
 * caso ou um gestor. Ate existir login, o painel informa esse id, como ja
 * faz no assumir.
 */
public record PedidoDeTransferencia(
        Long quemTransfereId,
        Long destinoId,
        String motivo) {
}
