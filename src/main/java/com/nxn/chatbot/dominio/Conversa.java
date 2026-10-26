package com.nxn.chatbot.dominio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * Agregado central do dominio. Dona das mensagens (composicao: mensagem nao
 * existe fora de uma conversa) e responsavel pela maquina de estados que
 * decide quem pode falar.
 */
@Entity
@Table(name = "conversa")
public class Conversa {

    private static final int MAX_TENTATIVAS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @OneToMany(
            mappedBy = "conversa",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("dataHora ASC, id ASC")
    private List<Mensagem> mensagens = new ArrayList<>();

    @Column(name = "iniciada_em", nullable = false)
    private LocalDateTime iniciadaEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoConversa estado;

    @Column(name = "encerrada_em")
    private LocalDateTime encerradaEm;

    @OneToOne(mappedBy = "conversa", cascade = CascadeType.ALL)
    private Ticket ticket;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "tentativas_sem_sucesso", nullable = false)
    private int tentativasSemSucesso;

    protected Conversa() {
        // Necessário para o JPA.
    }

    public Conversa(Long id, Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException(
                    "A conversa precisa de um cliente.");
        }
        this.id = id;
        this.cliente = cliente;
        this.iniciadaEm = LocalDateTime.now();
        this.estado = cliente.consentiu()
                ? EstadoConversa.COM_BOT
                : EstadoConversa.AGUARDANDO_CONSENTIMENTO;
    }

    /**
     * Unico caminho para criar uma mensagem no sistema. Conversa encerrada
     * recusa, o que impede reescrever historico de atendimento.
     */
    public Mensagem registrarMensagem(Remetente autor, String conteudo) {
        if (!estado.aceitaNovaMensagem()) {
            throw new IllegalStateException(
                    "Conversa encerrada não aceita novas mensagens.");
        }
        Mensagem mensagem = new Mensagem(this, autor, conteudo);
        mensagens.add(mensagem);
        return mensagem;
    }

    /** Chamado depois que o cliente aceita o aviso de privacidade. */
    public void liberarAposConsentimento() {
        if (estado != EstadoConversa.AGUARDANDO_CONSENTIMENTO) {
            throw new IllegalStateException(
                    "A conversa não está aguardando consentimento.");
        }
        if (!cliente.consentiu()) {
            throw new IllegalStateException(
                    "O cliente ainda não aceitou o aviso de privacidade.");
        }
        this.estado = EstadoConversa.COM_BOT;
    }

    public Ticket escalonar(MotivoEscalonamento motivo) {
        return escalonar(null, motivo);
    }

    /** Tira a conversa do bot e abre o ticket que vai aparecer na fila. */
    public Ticket escalonar(Long idTicket, MotivoEscalonamento motivo) {
        if (estado == EstadoConversa.ENCERRADA) {
            throw new IllegalStateException(
                    "Conversa encerrada não pode ser escalonada.");
        }
        if (ticket != null) {
            return ticket;
        }
        this.ticket = new Ticket(idTicket, this, motivo);
        this.estado = EstadoConversa.AGUARDANDO_ATENDENTE;
        return ticket;
    }

    /** Visibilidade de pacote: so o Ticket muda a conversa para atendimento humano. */
    void atenderPor(Atendente atendente) {
        if (atendente == null) {
            throw new IllegalArgumentException("Atendente é obrigatório.");
        }
        if (estado == EstadoConversa.ENCERRADA) {
            throw new IllegalStateException(
                    "Conversa encerrada não volta para atendimento.");
        }
        this.estado = EstadoConversa.COM_ATENDENTE;
    }

    public void encerrar() {
        if (estado == EstadoConversa.ENCERRADA) {
            return;
        }
        this.estado = EstadoConversa.ENCERRADA;
        this.encerradaEm = LocalDateTime.now();
    }

    public void registrarTentativaSemSucesso() {
        tentativasSemSucesso++;
    }

    public void reiniciarTentativas() {
        tentativasSemSucesso = 0;
    }

    public boolean esgotouTentativas() {
        return tentativasSemSucesso >= MAX_TENTATIVAS;
    }

    /** Conversa fechada sem nunca ter gerado ticket: entra na taxa de contencao. */
    public boolean resolvidaPeloBot() {
        return estado == EstadoConversa.ENCERRADA && ticket == null;
    }

    public boolean botPodeResponder() {
        return estado.botResponde();
    }

    /** Lista somente leitura: ninguem insere mensagem sem passar por registrarMensagem. */
    public List<Mensagem> getMensagens() {
        return Collections.unmodifiableList(mensagens);
    }

    public int getTotalDeMensagens() {
        return mensagens.size();
    }

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public EstadoConversa getEstado() {
        return estado;
    }

    public LocalDateTime getIniciadaEm() {
        return iniciadaEm;
    }

    public LocalDateTime getEncerradaEm() {
        return encerradaEm;
    }

    public int getTentativasSemSucesso() {
        return tentativasSemSucesso;
    }

    public Optional<Ticket> getTicket() {
        return Optional.ofNullable(ticket);
    }

    @Override
    public String toString() {
        return "Conversa{" + cliente.getIdentificacao() + ", " + estado + ", "
                + mensagens.size() + " mensagens}";
    }
}