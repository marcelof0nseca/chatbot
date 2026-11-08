package com.wxn.chatbot.repositorio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.wxn.chatbot.dominio.Atendente;
import com.wxn.chatbot.dominio.EstadoConversa;
import com.wxn.chatbot.dominio.StatusTicket;
import com.wxn.chatbot.dominio.Ticket;
import com.wxn.chatbot.dominio.Transferencia;
import com.wxn.chatbot.servico.FilaRepositorio;

/**
 * Implementacao da porta FilaRepositorio sobre os repositorios Spring Data.
 * O servico da fila depende so da porta, entao trocar a implementacao em
 * memoria por esta nao mudou nenhuma linha das regras de negocio.
 */
@Repository
public class FilaRepositorioJpa implements FilaRepositorio {

    private final TicketRepository tickets;
    private final ConversaRepository conversas;
    private final AtendenteRepository atendentes;
    private final TransferenciaRepository transferencias;

    public FilaRepositorioJpa(TicketRepository tickets, ConversaRepository conversas,
                              AtendenteRepository atendentes, TransferenciaRepository transferencias) {
        this.tickets = tickets;
        this.conversas = conversas;
        this.atendentes = atendentes;
        this.transferencias = transferencias;
    }

    @Override
    public List<Ticket> buscarPorStatus(StatusTicket status) {
        return tickets.findByStatusOrderByAbertoEmAsc(status);
    }

    @Override
    public Optional<Ticket> buscarTicket(Long id) {
        return tickets.findById(id);
    }

    @Override
    public Optional<Atendente> buscarAtendente(Long id) {
        return atendentes.findById(id);
    }

    @Override
    public List<Atendente> listarAtendentes() {
        return atendentes.findAll(Sort.by("nome"));
    }

    @Override
    public void salvar(Ticket ticket) {
        tickets.save(ticket);
    }

    @Override
    public void salvarTransferencia(Transferencia transferencia) {
        transferencias.save(transferencia);
    }

    @Override
    public List<Transferencia> listarTransferencias(Long ticketId) {
        return transferencias.findByTicketIdOrderByRealizadaEmAscIdAsc(ticketId);
    }

    @Override
    public long contarConversasDesde(LocalDateTime inicio) {
        return conversas.countByIniciadaEmAfter(inicio);
    }

    @Override
    public long contarConversasResolvidasPeloBotDesde(LocalDateTime inicio) {
        return conversas.countByIniciadaEmAfterAndEstadoAndTicketIsNull(inicio, EstadoConversa.ENCERRADA);
    }
}
