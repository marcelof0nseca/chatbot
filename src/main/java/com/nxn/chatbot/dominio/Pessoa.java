package com.nxn.chatbot.dominio;

import java.time.LocalDateTime;
import java.util.Objects;

public abstract class Pessoa {

    private final Long id;
    private String nome;
    private final LocalDateTime dataCadastro;

    protected Pessoa(Long id, String nome) {
        this.id = id;
        this.nome = validarNome(nome);
        this.dataCadastro = LocalDateTime.now();
    }

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

    /** Identidade pela chave, e não pelos atributos, que podem mudar. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Pessoa outra) || id == null) {
            return false;
        }
        return getClass() == o.getClass() && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + nome + "}";
    }
}
