package com.wxn.chatbot.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.wxn.chatbot.dominio.Chatbot;
import com.wxn.chatbot.dominio.RespostaCandidata;
import com.wxn.chatbot.dominio.TipoCliente;

/**
 * A base de exemplo responde as perguntas que o simulador sugere, e escala o
 * que nao sabe. As perguntas sugeridas estao em painel/js/simulador.js: se
 * mudarem la, mudam aqui.
 */
class TinoConfigTest {

    private final Chatbot tino = new TinoConfig().tino();

    @ParameterizedTest
    @ValueSource(strings = {
            "Qual o prazo para troca?",
            "Como acompanho meu pedido?",
            "Quais as formas de pagamento?",
            "posso pagar com pix?",
    })
    @DisplayName("As perguntas sugeridas no simulador são respondidas pela base")
    void respondeAsSugeridas(String pergunta) {
        Optional<RespostaCandidata> resposta = tino.melhorResposta(pergunta, TipoCliente.EXTERNO);

        assertTrue(resposta.isPresent(), pergunta);
        assertTrue(resposta.get().getConfianca() >= TinoConfig.LIMITE_DE_CONFIANCA,
                pergunta + ": confiança " + resposta.get().getConfianca());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Meu pedido chegou quebrado",
            "quero falar com um atendente",
    })
    @DisplayName("O que a base não cobre fica abaixo do limite: o Tino escala em vez de inventar")
    void escalaOQueNaoSabe(String pergunta) {
        double confianca = tino.melhorResposta(pergunta, TipoCliente.EXTERNO)
                .map(RespostaCandidata::getConfianca)
                .orElse(0.0);

        assertTrue(confianca < TinoConfig.LIMITE_DE_CONFIANCA, pergunta + ": confiança " + confianca);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("A base de exemplo tem os três artigos, todos públicos")
    void base() {
        assertEquals(3, tino.getBaseDeConhecimento().size());
        assertTrue(tino.getBaseDeConhecimento().stream().allMatch(a -> a.disponivelPara(TipoCliente.EXTERNO)));
    }
}
