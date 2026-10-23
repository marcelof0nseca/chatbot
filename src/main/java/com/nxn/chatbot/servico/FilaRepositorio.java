package com.nxn.chatbot.servico;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.nxn.chatbot.dominio.Atendente;
import com.nxn.chatbot.dominio.StatusTicket;
import com.nxn.chatbot.dominio.Ticket;

/**
 * Porta de acesso a dados da fila. Quem consome declara o que precisa, e a
 * implementacao fica de fora: hoje em memoria, e com o mapeamento JPA pronto
 * passa a ser um adaptador sobre os repositorios Spring Data.
 *
 * E o que permite testar as regras de negocio sem subir banco nenhum.
 */
public interface FilaRepositorio {

    List<Ticket> buscarPorStatus(StatusTicket status);

    Optional<Ticket> buscarTicket(Long id);

    Optional<Atendente> buscarAtendente(Long id);

    void salvar(Ticket ticket);

    long contarConversasDesde(LocalDateTime inicio);

    long contarConversasResolvidasPeloBotDesde(LocalDateTime inicio);
}
