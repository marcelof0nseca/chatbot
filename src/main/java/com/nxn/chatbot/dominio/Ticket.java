package com.nxn.chatbot.dominio;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * O atendimento humano aberto quando o bot escalona. Guarda quem assumiu,
 * quando, por que, e quanto tempo levou.
 *
 * Construtor de pacote: so a Conversa abre ticket, pela operacao escalonar().
 */
public class Ticket {

    private final Long id;
    private final Conversa conversa;
    private final MotivoEscalonamento motivo;
    private final LocalDateTime abertoEm;
    private Atendente atendente;
    private LocalDateTime assumidoEm;
    private LocalDateTime encerradoEm;
    private StatusTicket status;

    Ticket(Long id, Conversa conversa, MotivoEscalonamento motivo) {
        if (conversa == null) {
            throw new IllegalArgumentException("O ticket precisa de uma conversa.");
        }
        if (motivo == null) {
            throw new IllegalArgumentException("O motivo do escalonamento é obrigatório.");
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
            throw new IllegalStateException("Apenas um ticket aberto pode ser assumido.");
        }
        if (atendente == null) {
            throw new IllegalArgumentException("Atendente é obrigatório.");
        }
        if (!atendente.estaDisponivel()) {
            throw new IllegalStateException("O atendente não está em serviço.");
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
        if (status == StatusTicket.RESOLVIDO || status == StatusTicket.CANCELADO) {
            throw new IllegalStateException("Ticket já encerrado.");
        }
        this.encerradoEm = LocalDateTime.now();
        this.status = desfecho;
        conversa.encerrar();
    }

    /** Alimenta a coluna Espera da fila no painel. */
    public Duration tempoDeEspera() {
        LocalDateTime fim = (assumidoEm != null) ? assumidoEm : LocalDateTime.now();
        return Duration.between(abertoEm, fim);
    }

    public Duration tempoDeAtendimento() {
        if (assumidoEm == null) {
            return Duration.ZERO;
        }
        LocalDateTime fim = (encerradoEm != null) ? encerradoEm : LocalDateTime.now();
        return Duration.between(assumidoEm, fim);
    }

    public boolean estaEncerrado() {
        return status == StatusTicket.RESOLVIDO || status == StatusTicket.CANCELADO;
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
