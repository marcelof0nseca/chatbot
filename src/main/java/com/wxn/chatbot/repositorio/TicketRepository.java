package com.wxn.chatbot.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wxn.chatbot.dominio.StatusTicket;
import com.wxn.chatbot.dominio.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStatusOrderByAbertoEmAsc(StatusTicket status);

    long countByStatus(StatusTicket status);
}