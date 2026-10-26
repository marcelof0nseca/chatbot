package com.nxn.chatbot.dominio;

import java.time.LocalDateTime;
import java.util.Objects;

import org.hibernate.annotations.JoinFormula;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Mensagem pertencente a uma conversa.
 *
 * Não possui setters: o histórico não deve ser alterado pela aplicação.
 * O autor é armazenado pelo tipo e pelo identificador do remetente.
 */
@Entity
@Table(name = "mensagem")
public class Mensagem {

    /*
     * O esquema identifica mensagens do bot pelo tipo CHATBOT.
     * Não armazena o nome, a versão ou a configuração do bot.
     */
    private static final Remetente BOT_DO_HISTORICO = new Remetente() {

        @Override
        public String getIdentificacao() {
            return "Chatbot";
        }

        @Override
        public TipoRemetente getTipoRemetente() {
            return TipoRemetente.CHATBOT;
        }
    };

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "conversa_id", nullable = false)
    private Conversa conversa;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_remetente", nullable = false, length = 10)
    private TipoRemetente tipoRemetente;

    @Column(name = "remetente_id")
    private Long remetenteId;

    /*
     * Estas associações consultam as mesmas colunas existentes.
     * Cada expressão só retorna um ID para o tipo correspondente.
     */
    @ManyToOne(optional = true)
    @JoinFormula(
            value = "case when tipo_remetente = 'CLIENTE' "
                    + "then remetente_id else null end",
            referencedColumnName = "id"
    )
    private Cliente autorCliente;

    @ManyToOne(optional = true)
    @JoinFormula(
            value = "case when tipo_remetente = 'ATENDENTE' "
                    + "then remetente_id else null end",
            referencedColumnName = "id"
    )
    private Atendente autorAtendente;

    /*
     * Preserva o objeto original durante o uso em memória.
     * Após carregar do banco, getAutor() usa as associações acima.
     */
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

        this.tipoRemetente = Objects.requireNonNull(
                autor.getTipoRemetente(),
                "O tipo do remetente é obrigatório."
        );

        if (autor instanceof Cliente cliente) {
            this.autorCliente = cliente;
            this.remetenteId = cliente.getId();
        } else if (autor instanceof Atendente atendente) {
            this.autorAtendente = atendente;
            this.remetenteId = atendente.getId();
        }

        this.conteudo = conteudo;
        this.dataHora = LocalDateTime.now();
    }

    @PrePersist
    private void prepararRemetenteParaPersistencia() {
        if (tipoRemetente == TipoRemetente.CHATBOT) {
            remetenteId = null;
            return;
        }

        if (tipoRemetente == TipoRemetente.CLIENTE
                && autorCliente != null) {
            remetenteId = autorCliente.getId();
        } else if (tipoRemetente == TipoRemetente.ATENDENTE
                && autorAtendente != null) {
            remetenteId = autorAtendente.getId();
        } else {
            throw new IllegalStateException(
                    "O autor não corresponde ao tipo do remetente."
            );
        }

        if (remetenteId == null) {
            throw new IllegalStateException(
                    "Cliente ou atendente deve ter um ID "
                            + "antes de salvar a mensagem."
            );
        }
    }

    public Long getId() {
        return id;
    }

    public Conversa getConversa() {
        return conversa;
    }

    public Remetente getAutor() {
        if (autor != null) {
            return autor;
        }

        Remetente remetente = switch (tipoRemetente) {
            case CLIENTE -> autorCliente;
            case ATENDENTE -> autorAtendente;
            case CHATBOT -> BOT_DO_HISTORICO;
        };

        if (remetente == null) {
            throw new IllegalStateException(
                    "O remetente registrado na mensagem não foi encontrado."
            );
        }

        return remetente;
    }

    public TipoRemetente getRemetente() {
        return tipoRemetente;
    }

    public Long getRemetenteId() {
        return remetenteId;
    }

    public String getConteudo() {
        return conteudo;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public boolean ehDoCliente() {
        return tipoRemetente == TipoRemetente.CLIENTE;
    }

    @Override
    public String toString() {
        return "[" + tipoRemetente + "] " + conteudo;
    }
}