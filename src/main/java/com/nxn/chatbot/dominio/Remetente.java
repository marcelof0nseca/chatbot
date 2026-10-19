package com.nxn.chatbot.dominio;

/**
 * Contrato de quem pode enviar mensagem. Cliente, Atendente e Chatbot o
 * implementam, o que permite a Mensagem guardar o autor de forma polimorfica
 * sem conhecer a classe concreta.
 */
public interface Remetente {

    /** Identificacao segura para exibir no painel, nunca o dado pessoal cru. */
    String getIdentificacao();

    TipoRemetente getTipoRemetente();
}
