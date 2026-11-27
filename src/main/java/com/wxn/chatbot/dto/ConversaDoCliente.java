package com.wxn.chatbot.dto;

import java.util.List;

/**
 * A conversa vista pelo cliente, como o simulador (e, depois, o WhatsApp)
 * mostra: as falas em ordem e em que pe esta o atendimento.
 *
 * situacao e o estado da conversa: AGUARDANDO_CONSENTIMENTO, COM_BOT,
 * AGUARDANDO_ATENDENTE, COM_ATENDENTE ou ENCERRADA. ticketId so existe depois
 * que o Tino passou o caso para atendimento humano. opcoes sao as respostas
 * prontas da pergunta que o Tino acabou de fazer: no simulador viram botoes,
 * e no WhatsApp viram os botoes ou a lista da mensagem interativa.
 */
public record ConversaDoCliente(
        Long conversaId,
        String situacao,
        Long ticketId,
        String atendente,
        List<MensagemDoHistorico> mensagens,
        List<String> opcoes) {
}
