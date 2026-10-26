package com.nxn.chatbot.dominio;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Pessoa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    /** Exigido pelo JPA; não usar diretamente. */
    protected Pessoa() {
    }

    protected Pessoa(Long id, String nome) {
        this.id = id; // null para registros novos: o banco gera
        this.nome = validarNome(nome);
        this.dataCadastro = LocalDateTime.now();
    }

    /** Texto que identifica a pessoa na interface (sem expor dados sensíveis). */
    public abstract String getIdentificacao();

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = validarNome(nome);
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    // ---- validações compartilhadas pelas subclasses ----

    protected static String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome não pode ser vazio.");
        }
        return nome.trim();
    }

    protected static String validarEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("O e-mail é obrigatório.");
        }
        String e = email.trim();
        int arroba = e.indexOf('@');
        if (arroba <= 0 || arroba != e.lastIndexOf('@') || arroba == e.length() - 1) {
            throw new IllegalArgumentException("E-mail inválido: deve conter um único '@' com texto antes e depois.");
        }
        return e;
    }
}