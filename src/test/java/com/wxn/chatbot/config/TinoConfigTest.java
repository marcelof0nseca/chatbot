package com.wxn.chatbot.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.wxn.chatbot.dominio.Chatbot;
import com.wxn.chatbot.dominio.Fluxo;
import com.wxn.chatbot.dominio.RespostaCandidata;
import com.wxn.chatbot.dominio.TipoCliente;
import com.wxn.chatbot.dominio.Transicao;

/**
 * A base de exemplo responde as perguntas de informacao, e o resto fica para
 * os fluxos guiados, que tambem sao conferidos aqui: o menu abre todos e
 * nenhum leva a um destino que nao existe.
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
    @DisplayName("As perguntas de informação são respondidas pela base")
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
    @DisplayName("O que a base não cobre fica abaixo do limite: a base não inventa resposta")
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

    @org.junit.jupiter.api.Test
    @DisplayName("Todo assunto guiado pode ser aberto pelo menu, e o menu oferece um atendente")
    void menuAbreTodosOsAssuntos() {
        Fluxo menu = tino.getFluxos().stream()
                .filter(f -> f.getId().equals(Chatbot.FLUXO_MENU)).findFirst().orElseThrow();
        Set<String> abertosPeloMenu = new HashSet<>(menu.getFluxosDeDestino());
        Set<String> assuntos = new HashSet<>();
        tino.getFluxos().stream().map(Fluxo::getId)
                .filter(id -> !id.equals(Chatbot.FLUXO_MENU)).forEach(assuntos::add);

        assertEquals(assuntos, abertosPeloMenu);
        assertTrue(menu.getEtapaInicial().getDestinos().stream()
                .anyMatch(t -> t.getTipo() == Transicao.Tipo.PASSAR_PARA_ATENDENTE));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Os fluxos de exemplo passam na conferência de destinos")
    void fluxosValidos() {
        tino.validarFluxos();
        assertEquals(6, tino.getFluxos().size());
    }
}
