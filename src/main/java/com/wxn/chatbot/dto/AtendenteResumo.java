package com.wxn.chatbot.dto;

/** Quem pode assumir casos no painel. */
public record AtendenteResumo(
        Long id,
        String nome,
        String perfil,
        boolean disponivel) {
}
