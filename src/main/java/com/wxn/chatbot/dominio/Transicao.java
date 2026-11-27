package com.wxn.chatbot.dominio;

import java.util.Objects;

/**
 * O que acontece depois que o cliente responde a uma etapa: ir para outra
 * etapa, abrir outro fluxo, repetir a pergunta, concluir, encerrar a conversa
 * ou passar o caso para um atendente.
 *
 * O texto e o que o Tino diz junto: para quem segue adiante, e a frase antes
 * da proxima pergunta ("Recebi a foto, obrigado!"); para quem conclui ou
 * repete, e a propria resposta.
 */
public final class Transicao {

    public enum Tipo {
        IR_PARA,
        INICIAR_FLUXO,
        REPETIR,
        CONCLUIR,
        ENCERRAR,
        PASSAR_PARA_ATENDENTE
    }

    private final Tipo tipo;
    private final String alvo;
    private final String texto;

    private Transicao(Tipo tipo, String alvo, String texto) {
        this.tipo = tipo;
        this.alvo = alvo;
        this.texto = texto;
    }

    public static Transicao irPara(String etapa) {
        return irPara(etapa, null);
    }

    public static Transicao irPara(String etapa, String textoAntes) {
        return new Transicao(Tipo.IR_PARA, obrigatorio(etapa, "A etapa de destino"), textoAntes);
    }

    public static Transicao iniciarFluxo(String fluxo) {
        return iniciarFluxo(fluxo, null);
    }

    public static Transicao iniciarFluxo(String fluxo, String textoAntes) {
        return new Transicao(Tipo.INICIAR_FLUXO, obrigatorio(fluxo, "O fluxo de destino"), textoAntes);
    }

    /** Resposta que nao serve: o Tino explica e pergunta de novo. */
    public static Transicao repetir(String ajuda) {
        return new Transicao(Tipo.REPETIR, null, obrigatorio(ajuda, "O texto de ajuda"));
    }

    /** O assunto foi resolvido; o Tino pergunta se pode ajudar em mais alguma coisa. */
    public static Transicao concluir(String texto) {
        return new Transicao(Tipo.CONCLUIR, null, obrigatorio(texto, "O texto de conclusão"));
    }

    /** Despedida: a conversa termina resolvida pelo Tino, e entra na taxa de contenção. */
    public static Transicao encerrar(String despedida) {
        return new Transicao(Tipo.ENCERRAR, null, obrigatorio(despedida, "A despedida"));
    }

    public static Transicao passarParaAtendente(String textoAntes) {
        return new Transicao(Tipo.PASSAR_PARA_ATENDENTE, null, textoAntes);
    }

    /** A mesma transicao, com uma frase dita antes ("Sem problema."). */
    public Transicao antecedidaPor(String prefixo) {
        if (prefixo == null || prefixo.isBlank()) {
            return this;
        }
        String junto = texto == null ? prefixo : prefixo + "\n\n" + texto;
        return new Transicao(tipo, alvo, junto);
    }

    public Tipo getTipo() {
        return tipo;
    }

    /** A etapa (IR_PARA) ou o fluxo (INICIAR_FLUXO) de destino; nulo nos demais. */
    public String getAlvo() {
        return alvo;
    }

    public String getTexto() {
        return texto;
    }

    private static String obrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório.");
        }
        return valor;
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof Transicao t
                && tipo == t.tipo && Objects.equals(alvo, t.alvo) && Objects.equals(texto, t.texto);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tipo, alvo, texto);
    }

    @Override
    public String toString() {
        return tipo + (alvo == null ? "" : " " + alvo);
    }
}
