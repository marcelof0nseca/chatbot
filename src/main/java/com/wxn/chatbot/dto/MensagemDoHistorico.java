package com.wxn.chatbot.dto;

import java.time.LocalDateTime;

/**
 * Uma fala do historico, como a Tela 7 exibe.
 *
 * autor e CLIENTE, CHATBOT ou ATENDENTE. identificacao e o que aparece na
 * tela: telefone mascarado para o cliente, nome para o atendente, "Tino" para
 * o bot.
 */
public record MensagemDoHistorico(
        String autor,
        String identificacao,
        String conteudo,
        LocalDateTime dataHora) {
}
