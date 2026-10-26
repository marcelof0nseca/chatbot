package com.nxn.chatbot.dominio;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Mensagem pertencente a uma conversa.
 *
 * Não possui setters: o histórico não deve ser alterado pela aplicação.
 * O construtor com argumentos tem visibilidade de pacote para que
 * as mensagens sejam criadas pela Conversa.
 */
@Entity
@Table(name = "mensagem")
public class Mensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "conversa_id", nullable = false)
    private Conversa conversa;

    // Temporário: o mapeamento do autor será concluído na próxima etapa.
    @Transient
    private Remetente autor;

    @Column(name = "conteudo", nullable = false, length = 4000)
    private String conteudo;

    @Column(name = "data_hora", nullable = false)
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

    public Long getId() {
        return id;
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