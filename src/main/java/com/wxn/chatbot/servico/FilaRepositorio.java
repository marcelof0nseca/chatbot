package com.wxn.chatbot.servico;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.wxn.chatbot.dominio.Atendente;
import com.wxn.chatbot.dominio.StatusTicket;
import com.wxn.chatbot.dominio.Ticket;
import com.wxn.chatbot.dominio.Transferencia;

/**
 * Porta de acesso a dados da fila. Quem consome declara o que precisa, e a
 * implementacao fica de fora: em producao, FilaRepositorioJpa, sobre os
 * repositorios Spring Data; nos testes, um duble em memoria.
 *
 * E o que permite testar as regras de negocio sem subir banco nenhum.
 */
public interface FilaRepositorio {

    List<Ticket> buscarPorStatus(StatusTicket status);

    Optional<Ticket> buscarTicket(Long id);

    Optional<Atendente> buscarAtendente(Long id);

    /** Atendentes em ordem alfabetica, para o painel escolher quem esta atendendo. */
    List<Atendente> listarAtendentes();

    void salvar(Ticket ticket);

    void salvarTransferencia(Transferencia transferencia);

    /** As transferencias de um caso, da mais antiga para a mais recente. */
    List<Transferencia> listarTransferencias(Long ticketId);

    long contarConversasDesde(LocalDateTime inicio);

    long contarConversasResolvidasPeloBotDesde(LocalDateTime inicio);
}
