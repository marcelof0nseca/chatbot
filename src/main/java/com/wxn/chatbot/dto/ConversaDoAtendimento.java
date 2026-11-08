package com.wxn.chatbot.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.wxn.chatbot.dominio.StatusTicket;

/**
 * Tudo o que a Tela 7 precisa para o atendente seguir a conversa sem pedir
 * ao cliente que repita nada: o historico completo, o motivo do
 * encaminhamento, o momento em que o bot passou o caso adiante (abertoEm) e
 * por quais atendentes o caso ja passou (transferencias, da mais antiga para
 * a mais recente; vazia se o caso nunca mudou de maos).
 *
 * Como na fila, o telefone sai mascarado.
 */
public record ConversaDoAtendimento(
        Long ticketId,
        Long conversaId,
        String contato,
        String tipoCliente,
        String estadoConversa,
        StatusTicket status,
        String motivo,
        String atendente,
        LocalDateTime abertoEm,
        LocalDateTime assumidoEm,
        LocalDateTime consentimentoEm,
        List<MensagemDoHistorico> mensagens,
        List<TransferenciaDoHistorico> transferencias) {
}
