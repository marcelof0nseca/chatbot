package com.wxn.chatbot.servico;

/**
 * O que foi procurado pelo id nao existe: ticket, atendente. A API responde 404.
 *
 * Fica separada de IllegalArgumentException, que o dominio usa para dado
 * invalido (400). Sem essa separacao, "o nome e obrigatorio" chegaria ao
 * painel como "nao encontrado".
 */
public class NaoEncontradoException extends RuntimeException {

    public NaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
