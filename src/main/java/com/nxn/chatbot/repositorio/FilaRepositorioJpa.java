package com.nxn.chatbot.repositorio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.nxn.chatbot.dominio.Atendente;
import com.nxn.chatbot.dominio.EstadoConversa;
import com.nxn.chatbot.dominio.StatusTicket;
import com.nxn.chatbot.dominio.Ticket;
import com.nxn.chatbot.servico.FilaRepositorio;

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

    public FilaRepositorioJpa(TicketRepository tickets, ConversaRepository conversas,
                              AtendenteRepository atendentes) {
        this.tickets = tickets;
        this.conversas = conversas;
        this.atendentes = atendentes;
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
    public void salvar(Ticket ticket) {
        tickets.save(ticket);
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
