package com.nxn.chatbot.repositorio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.nxn.chatbot.dominio.Atendente;
import com.nxn.chatbot.dominio.Conversa;
import com.nxn.chatbot.dominio.StatusTicket;
import com.nxn.chatbot.dominio.Ticket;
import com.nxn.chatbot.servico.FilaRepositorio;

/**
 * Duble de teste da porta FilaRepositorio: guarda tudo em memoria, para as
 * regras do servico serem testadas sem subir banco. Em producao quem
 * implementa a porta e o FilaRepositorioJpa.
 */
public class FilaRepositorioEmMemoria implements FilaRepositorio {

    private final Map<Long, Ticket> tickets = new LinkedHashMap<>();
    private final Map<Long, Atendente> atendentes = new LinkedHashMap<>();
    private final List<Conversa> conversas = new ArrayList<>();

    public void registrar(Conversa conversa) {
        conversas.add(conversa);
        conversa.getTicket().ifPresent(ticket -> tickets.put(ticket.getId(), ticket));
    }

    public void registrar(Atendente atendente) {
        atendentes.put(atendente.getId(), atendente);
    }

    @Override
    public List<Ticket> buscarPorStatus(StatusTicket status) {
        return tickets.values().stream()
                .filter(ticket -> ticket.getStatus() == status)
                .toList();
    }

    @Override
    public Optional<Ticket> buscarTicket(Long id) {
        return Optional.ofNullable(tickets.get(id));
    }

    @Override
    public Optional<Atendente> buscarAtendente(Long id) {
        return Optional.ofNullable(atendentes.get(id));
    }

    @Override
    public void salvar(Ticket ticket) {
        tickets.put(ticket.getId(), ticket);
    }

    @Override
    public long contarConversasDesde(LocalDateTime inicio) {
        return conversas.stream()
                .filter(conversa -> conversa.getIniciadaEm().isAfter(inicio))
                .count();
    }

    @Override
    public long contarConversasResolvidasPeloBotDesde(LocalDateTime inicio) {
        return conversas.stream()
                .filter(conversa -> conversa.getIniciadaEm().isAfter(inicio))
                .filter(Conversa::resolvidaPeloBot)
                .count();
    }
}
