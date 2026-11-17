package com.wxn.chatbot.repositorio;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wxn.chatbot.dominio.Cliente;
import com.wxn.chatbot.dominio.Conversa;
import com.wxn.chatbot.dominio.EstadoConversa;

public interface ConversaRepository extends JpaRepository<Conversa, Long> {

    long countByIniciadaEmAfter(LocalDateTime inicio);

    /**
     * Conversas resolvidas pelo bot: encerradas sem nunca ter gerado ticket.
     * Filtrar so por "sem ticket" contaria tambem as conversas ainda em
     * andamento com o bot, e inflaria a taxa de contencao.
     */
    long countByIniciadaEmAfterAndEstadoAndTicketIsNull(LocalDateTime inicio, EstadoConversa estado);

    /** A conversa mais recente do cliente que ainda nao foi encerrada. */
    Optional<Conversa> findFirstByClienteAndEstadoNotOrderByIniciadaEmDescIdDesc(Cliente cliente, EstadoConversa estado);
}
