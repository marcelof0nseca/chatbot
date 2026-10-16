package com.nxn.chatbot.dominio;

import java.time.LocalDateTime;

/** Imutável: classe final, atributos final e sem setters. */
public final class Consentimento {

    private final LocalDateTime data;
    private final String versaoPolitica;

    public Consentimento(String versaoPolitica) {
        this(LocalDateTime.now(), versaoPolitica);
    }

    public Consentimento(LocalDateTime data, String versaoPolitica) {
        if (data == null) {
            throw new IllegalArgumentException("A data do consentimento é obrigatória.");
        }
        if (versaoPolitica == null || versaoPolitica.isBlank()) {
            throw new IllegalArgumentException("A versão da política é obrigatória.");
        }
        this.data = data;
        this.versaoPolitica = versaoPolitica.trim();
    }

    public LocalDateTime getData() {
        return data;
    }

    public String getVersaoPolitica() {
        return versaoPolitica;
    }
}