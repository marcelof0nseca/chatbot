package com.nxn.chatbot.dominio;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Valor embutido em Cliente. Sem setters: a imutabilidade vem da ausência
 * deles. Não é final porque o JPA precisa de construtor vazio.
 */
@Embeddable
public class Consentimento {

    @Column(name = "aceito_em")
    private LocalDateTime data;

    @Column(name = "versao_politica", length = 20)
    private String versaoPolitica;

    /** Exigido pelo JPA; não usar diretamente. */
    protected Consentimento() {
    }

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