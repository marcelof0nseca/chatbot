package com.wxn.chatbot.dominio;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Registro de que um caso mudou de maos: de quem saiu, para quem foi, quem
 * decidiu, por que e quando. O ticket guarda so o atendente atual; o caminho
 * percorrido fica aqui, uma linha por transferencia.
 *
 * Imutavel e com construtor de pacote: so Ticket.transferirPara() cria, depois
 * de validar as regras.
 */
@Entity
@Table(name = "transferencia")
public class Transferencia {

    /** Limite do motivo, para caber numa linha do historico do painel. */
    public static final int TAMANHO_MAXIMO_DO_MOTIVO = 280;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(optional = false)
    @JoinColumn(name = "origem_id", nullable = false)
    private Atendente origem;

    @ManyToOne(optional = false)
    @JoinColumn(name = "destino_id", nullable = false)
    private Atendente destino;

    /** Quem fez a transferencia: o proprio atendente do caso ou um gestor. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "realizada_por_id", nullable = false)
    private Atendente realizadaPor;

    @Column(name = "motivo", nullable = false, length = TAMANHO_MAXIMO_DO_MOTIVO)
    private String motivo;

    @Column(name = "realizada_em", nullable = false)
    private LocalDateTime realizadaEm;

    protected Transferencia() {
        // Necessário para o JPA.
    }

    Transferencia(Ticket ticket, Atendente origem, Atendente destino, Atendente realizadaPor, String motivo) {
        this.ticket = ticket;
        this.origem = origem;
        this.destino = destino;
        this.realizadaPor = realizadaPor;
        this.motivo = motivo;
        this.realizadaEm = Relogio.agora();
    }

    public Long getId() {
        return id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public Atendente getOrigem() {
        return origem;
    }

    public Atendente getDestino() {
        return destino;
    }

    public Atendente getRealizadaPor() {
        return realizadaPor;
    }

    public String getMotivo() {
        return motivo;
    }

    public LocalDateTime getRealizadaEm() {
        return realizadaEm;
    }

    @Override
    public String toString() {
        return "Transferencia{" + origem.getNome() + " -> " + destino.getNome() + "}";
    }
}
