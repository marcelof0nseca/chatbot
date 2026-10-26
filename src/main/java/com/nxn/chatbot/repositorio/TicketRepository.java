package com.nxn.chatbot.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nxn.chatbot.dominio.StatusTicket;
import com.nxn.chatbot.dominio.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStatusOrderByAbertoEmAsc(StatusTicket status);

    long countByStatus(StatusTicket status);
}