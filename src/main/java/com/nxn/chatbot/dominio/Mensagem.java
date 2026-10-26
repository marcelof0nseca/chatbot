package com.nxn.chatbot.dominio;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Mensagem pertencente a uma conversa.
 *
 * Não possui setters: o histórico não deve ser alterado pela aplicação.
 * A classe e os campos não são final para permitir a persistência com JPA.
 *
 * O construtor com argumentos tem visibilidade de pacote:
 * a criação das mensagens acontece pela Conversa.
 */
public class Mensagem {

    private Conversa conversa;
    private Remetente autor;
    private String conteudo;
    private LocalDateTime dataHora;

    protected Mensagem() {
        // Necessário para o JPA.
    }

    Mensagem(Conversa conversa, Remetente autor, String conteudo) {
        if (conteudo == null || conteudo.isBlank()) {
            throw new IllegalArgumentException(
                    "O conteúdo da mensagem não pode estar vazio."
            );
        }

        this.conversa = Objects.requireNonNull(
                conversa, "A mensagem precisa pertencer a uma conversa."
        );
        this.autor = Objects.requireNonNull(
                autor, "O remetente é obrigatório."
        );
        this.conteudo = conteudo;
        this.dataHora = LocalDateTime.now();
    }

    public Conversa getConversa() {
        return conversa;
    }

    /** O autor concreto: qual cliente, qual atendente, ou o próprio bot. */
    public Remetente getAutor() {
        return autor;
    }

    public TipoRemetente getRemetente() {
        return autor.getTipoRemetente();
    }

    public String getConteudo() {
        return conteudo;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public boolean ehDoCliente() {
        return autor.getTipoRemetente() == TipoRemetente.CLIENTE;
    }

    @Override
    public String toString() {
        return "[" + autor.getTipoRemetente() + "] " + conteudo;
    }
}