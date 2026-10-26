package com.nxn.chatbot.dominio;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * O atendimento humano aberto quando o bot escalona. Guarda quem assumiu,
 * quando, por que, e quanto tempo levou.
 *
 * Construtor de pacote: so a Conversa abre ticket, pela operacao escalonar().
 */
@Entity
@Table(name = "ticket")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "conversa_id", nullable = false, unique = true)
    private Conversa conversa;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo", nullable = false, length = 25)
    private MotivoEscalonamento motivo;

    @Column(name = "aberto_em", nullable = false)
    private LocalDateTime abertoEm;

    @ManyToOne(optional = true)
    @JoinColumn(name = "atendente_id", nullable = true)
    private Atendente atendente;

    @Column(name = "assumido_em")
    private LocalDateTime assumidoEm;

    @Column(name = "encerrado_em")
    private LocalDateTime encerradoEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private StatusTicket status;

    protected Ticket() {
        // Necessário para o JPA.
    }

    Ticket(Long id, Conversa conversa, MotivoEscalonamento motivo) {
        if (conversa == null) {
            throw new IllegalArgumentException(
                    "O ticket precisa de uma conversa.");
        }
        if (motivo == null) {
            throw new IllegalArgumentException(
                    "O motivo do escalonamento é obrigatório.");
        }
        this.id = id;
        this.conversa = conversa;
        this.motivo = motivo;
        this.abertoEm = LocalDateTime.now();
        this.status = StatusTicket.ABERTO;
    }

    /** Assumir o ticket tambem muda o estado da conversa: o bot para de responder. */
    public void assumir(Atendente atendente) {
        if (status != StatusTicket.ABERTO) {
            throw new IllegalStateException(
                    "Apenas um ticket aberto pode ser assumido.");
        }
        if (atendente == null) {
            throw new IllegalArgumentException("Atendente é obrigatório.");
        }
        if (!atendente.estaDisponivel()) {
            throw new IllegalStateException(
                    "O atendente não está em serviço.");
        }

        // A conversa muda primeiro: se ela recusar, o ticket nao fica num
        // estado intermediario invalido.
        conversa.atenderPor(atendente);
        this.atendente = atendente;
        this.assumidoEm = LocalDateTime.now();
        this.status = StatusTicket.EM_ATENDIMENTO;
    }

    public void resolver() {
        encerrar(StatusTicket.RESOLVIDO);
    }

    public void cancelar() {
        encerrar(StatusTicket.CANCELADO);
    }

    private void encerrar(StatusTicket desfecho) {
        if (status == StatusTicket.RESOLVIDO
                || status == StatusTicket.CANCELADO) {
            throw new IllegalStateException("Ticket já encerrado.");
        }
        this.encerradoEm = LocalDateTime.now();
        this.status = desfecho;
        conversa.encerrar();
    }

    /** Alimenta a coluna Espera da fila no painel. */
    public Duration tempoDeEspera() {
        LocalDateTime fim = (assumidoEm != null)
                ? assumidoEm
                : LocalDateTime.now();
        return Duration.between(abertoEm, fim);
    }

    public Duration tempoDeAtendimento() {
        if (assumidoEm == null) {
            return Duration.ZERO;
        }
        LocalDateTime fim = (encerradoEm != null)
                ? encerradoEm
                : LocalDateTime.now();
        return Duration.between(assumidoEm, fim);
    }

    public boolean estaEncerrado() {
        return status == StatusTicket.RESOLVIDO
                || status == StatusTicket.CANCELADO;
    }

    public Long getId() {
        return id;
    }

    public Conversa getConversa() {
        return conversa;
    }

    public MotivoEscalonamento getMotivo() {
        return motivo;
    }

    public LocalDateTime getAbertoEm() {
        return abertoEm;
    }

    public LocalDateTime getAssumidoEm() {
        return assumidoEm;
    }

    public LocalDateTime getEncerradoEm() {
        return encerradoEm;
    }

    /** Optional deixa explicito que um ticket na fila ainda nao tem atendente. */
    public Optional<Atendente> getAtendente() {
        return Optional.ofNullable(atendente);
    }

    public StatusTicket getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "Ticket{" + status + ", " + motivo + "}";
    }
}