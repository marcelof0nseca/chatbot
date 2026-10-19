package com.nxn.chatbot.dominio;

/** Por que a conversa saiu do bot. Aparece na fila do painel. */
public enum MotivoEscalonamento {
    BAIXA_CONFIANCA,
    SOLICITACAO_CLIENTE,
    ASSUNTO_NAO_SUPORTADO,
    FORA_DO_EXPEDIENTE,
    TENTATIVAS_ESGOTADAS
}
