package com.nxn.chatbot.repositorio;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nxn.chatbot.dominio.Conversa;

public interface ConversaRepository extends JpaRepository<Conversa, Long> {

    long countByIniciadaEmAfter(LocalDateTime inicio);

    long countByIniciadaEmAfterAndTicketIsNull(LocalDateTime inicio);
}