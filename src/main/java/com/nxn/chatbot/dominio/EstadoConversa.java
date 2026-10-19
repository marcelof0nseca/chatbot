package com.nxn.chatbot.dominio;

/** Estados possiveis de uma conversa. Governa quem pode falar e quando. */
public enum EstadoConversa {

    AGUARDANDO_CONSENTIMENTO,
    COM_BOT,
    AGUARDANDO_ATENDENTE,
    COM_ATENDENTE,
    ENCERRADA;

    public boolean aceitaNovaMensagem() {
        return this != ENCERRADA;
    }

    /** O bot so responde enquanto nenhum humano assumiu o caso. */
    public boolean botResponde() {
        return this == COM_BOT || this == AGUARDANDO_CONSENTIMENTO;
    }
}
