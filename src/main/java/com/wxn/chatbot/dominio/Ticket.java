package com.wxn.chatbot.dominio;

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
        this.abertoEm = Relogio.agora();
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
        this.assumidoEm = Relogio.agora();
        this.status = StatusTicket.EM_ATENDIMENTO;
    }

    /**
     * Passa o caso em atendimento para outro atendente, sem devolve-lo a fila.
     * O cliente continua com um humano: o status segue EM_ATENDIMENTO, a
     * conversa segue COM_ATENDENTE e o assumidoEm original e preservado, para
     * o tempo de espera do cliente nao ser zerado.
     *
     * Quem pode transferir: o atendente atual do caso ou um gestor.
     *
     * @return o registro da transferencia, que o servico grava junto com o ticket
     */
    public Transferencia transferirPara(Atendente quemTransfere, Atendente destino, String motivo) {
        if (quemTransfere == null) {
            throw new IllegalArgumentException("Quem transfere é obrigatório.");
        }
        if (destino == null) {
            throw new IllegalArgumentException("O atendente de destino é obrigatório.");
        }
        String motivoLimpo = (motivo == null) ? "" : motivo.trim();
        if (motivoLimpo.isEmpty()) {
            throw new IllegalArgumentException("O motivo da transferência é obrigatório.");
        }
        if (motivoLimpo.length() > Transferencia.TAMANHO_MAXIMO_DO_MOTIVO) {
            throw new IllegalArgumentException("O motivo da transferência passa de "
                    + Transferencia.TAMANHO_MAXIMO_DO_MOTIVO + " caracteres.");
        }
        if (status != StatusTicket.EM_ATENDIMENTO) {
            throw new IllegalStateException(
                    "Apenas um ticket em atendimento pode ser transferido.");
        }
        boolean ehOAtual = mesmoAtendente(quemTransfere, atendente);
        if (!ehOAtual && quemTransfere.getPerfil() != PerfilAtendente.GESTOR) {
            throw new IllegalStateException(
                    "Só o atendente do caso ou um gestor pode transferi-lo.");
        }
        if (mesmoAtendente(destino, atendente)) {
            throw new IllegalStateException(
                    "O caso já está com esse atendente.");
        }
        if (!destino.estaDisponivel()) {
            throw new IllegalStateException(
                    "O atendente de destino não está em serviço.");
        }

        Atendente origem = this.atendente;
        conversa.atenderPor(destino);
        this.atendente = destino;
        return new Transferencia(this, origem, destino, quemTransfere, motivoLimpo);
    }

    /**
     * Compara pelo id quando os dois ja foram gravados; antes disso, pela
     * referencia. O Atendente nao define equals, e o JPA pode entregar
     * instancias diferentes da mesma linha.
     */
    private static boolean mesmoAtendente(Atendente a, Atendente b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getId() != null && b.getId() != null) {
            return a.getId().equals(b.getId());
        }
        return a == b;
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
        this.encerradoEm = Relogio.agora();
        this.status = desfecho;
        conversa.encerrar();
    }

    /** Alimenta a coluna Espera da fila no painel. */
    public Duration tempoDeEspera() {
        LocalDateTime fim = (assumidoEm != null)
                ? assumidoEm
                : Relogio.agora();
        return Duration.between(abertoEm, fim);
    }

    public Duration tempoDeAtendimento() {
        if (assumidoEm == null) {
            return Duration.ZERO;
        }
        LocalDateTime fim = (encerradoEm != null)
                ? encerradoEm
                : Relogio.agora();
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