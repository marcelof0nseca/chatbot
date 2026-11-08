package com.wxn.chatbot.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wxn.chatbot.dominio.Transferencia;

public interface TransferenciaRepository extends JpaRepository<Transferencia, Long> {

    /**
     * O caminho do caso, da primeira transferencia para a ultima. O id desempata
     * duas transferencias no mesmo instante, para a ordem nao variar.
     */
    List<Transferencia> findByTicketIdOrderByRealizadaEmAscIdAsc(Long ticketId);
}
