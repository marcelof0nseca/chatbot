package com.wxn.chatbot.dto;

import java.time.LocalDateTime;

/**
 * Uma transferencia do caso, como a Tela 7 exibe: de quem para quem, quem
 * decidiu e por que. Os atendentes saem pelo nome.
 */
public record TransferenciaDoHistorico(
        String de,
        String para,
        String realizadaPor,
        String motivo,
        LocalDateTime realizadaEm) {
}
