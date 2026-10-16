package com.nxn.chatbot.dominio;

import java.time.LocalDateTime;
import java.util.Objects;

public final class Mensagem {

    private final String conteudo;
    private final TipoRemetente remetente;
    private final LocalDateTime dataHora;

    public Mensagem(String conteudo, TipoRemetente remetente) {
        if (conteudo == null || conteudo.isBlank()) {
            throw new IllegalArgumentException(
                "O conteúdo da mensagem não pode estar vazio."
            );
        }

        this.conteudo = conteudo;
        this.remetente = Objects.requireNonNull(
            remetente, "O remetente é obrigatório."
        );
        this.dataHora = LocalDateTime.now();
    }

    public String getConteudo() {
        return conteudo;
    }

    public TipoRemetente getRemetente() {
        return remetente;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
}