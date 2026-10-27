package com.nxn.chatbot.repositorio;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nxn.chatbot.dominio.Conversa;
import com.nxn.chatbot.dominio.EstadoConversa;

public interface ConversaRepository extends JpaRepository<Conversa, Long> {

    long countByIniciadaEmAfter(LocalDateTime inicio);

    /**
     * Conversas resolvidas pelo bot: encerradas sem nunca ter gerado ticket.
     * Filtrar so por "sem ticket" contaria tambem as conversas ainda em
     * andamento com o bot, e inflaria a taxa de contencao.
     */
    long countByIniciadaEmAfterAndEstadoAndTicketIsNull(LocalDateTime inicio, EstadoConversa estado);
}
