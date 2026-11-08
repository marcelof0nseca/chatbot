package com.wxn.chatbot.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.wxn.chatbot.servico.NaoEncontradoException;

/**
 * Traducao das excecoes em respostas HTTP, valida para todos os controllers.
 * O corpo e sempre {"erro": "..."}, que o painel exibe ao atendente.
 *
 *   404  o que se procurou nao existe            NaoEncontradoException
 *   400  o pedido veio com dado invalido          IllegalArgumentException
 *   409  a regra do dominio recusou a operacao    IllegalStateException
 */
@RestControllerAdvice
public class ErrosDaApi {

    @ExceptionHandler(NaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> naoEncontrado(NaoEncontradoException erro) {
        return corpo(erro.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> dadoInvalido(IllegalArgumentException erro) {
        return corpo(erro.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> regraRecusada(IllegalStateException erro) {
        return corpo(erro.getMessage());
    }

    // Erros que o proprio Spring detecta antes de chegar ao controller. Sem
    // estes tratadores eles sairiam no formato padrao do Spring, e nao no
    // {"erro": ...} que o painel le.

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> corpoIlegivel(HttpMessageNotReadableException erro) {
        return corpo("O corpo da requisição não é um JSON válido.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> parametroAusente(MissingServletRequestParameterException erro) {
        return corpo("Parâmetro obrigatório ausente: " + erro.getParameterName() + ".");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> tipoInvalido(MethodArgumentTypeMismatchException erro) {
        return corpo("Valor inválido para " + erro.getName() + ": " + erro.getValue() + ".");
    }

    private static Map<String, String> corpo(String mensagem) {
        return Map.of("erro", mensagem == null ? "Erro sem descrição." : mensagem);
    }
}
