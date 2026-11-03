package com.wxn.chatbot.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traducao das excecoes do dominio para respostas HTTP, valida para todos os
 * controllers. O corpo e sempre {"erro": "..."}, que o painel exibe.
 */
@RestControllerAdvice
public class ErrosDaApi {

    /** Ticket ou atendente que nao existe. */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> naoEncontrado(IllegalArgumentException erro) {
        return Map.of("erro", erro.getMessage());
    }

    /** Regra do dominio recusou: ticket ja assumido, atendente fora de servico. */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> conflito(IllegalStateException erro) {
        return Map.of("erro", erro.getMessage());
    }
}
