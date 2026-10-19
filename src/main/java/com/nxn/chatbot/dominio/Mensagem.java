package com.nxn.chatbot.dominio;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Imutável: classe final, atributos final e sem setters, porque histórico de
 * atendimento não se reescreve.
 *
 * O construtor é de pacote de propósito: só a Conversa cria mensagens, o que
 * garante que nenhuma mensagem exista solta fora de uma conversa (composição).
 */
public final class Mensagem {

    private final Conversa conversa;
    private final Remetente autor;
    private final String conteudo;
    private final LocalDateTime dataHora;

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
