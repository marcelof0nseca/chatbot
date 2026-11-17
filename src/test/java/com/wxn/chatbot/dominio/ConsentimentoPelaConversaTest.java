package com.wxn.chatbot.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** O aceite do aviso de privacidade (LGPD) dado pelo proprio cliente, na conversa. */
class ConsentimentoPelaConversaTest {

    private Chatbot tino;
    private Cliente cliente;
    private Conversa conversa;

    @BeforeEach
    void preparar() {
        tino = new Chatbot("Tino", "1.0", 0.45);
        Atendente gestor = new Atendente(1L, "Ana Lima", "ana@wxn.com.br", PerfilAtendente.GESTOR);
        tino.publicarArtigo(new ArtigoConhecimento("Qual o prazo para troca?", List.of(),
                "A troca pode ser solicitada em até 30 dias.", "Trocas", true), gestor);
        cliente = new Cliente(1L, "Maria Souza", "5581999994821", TipoCliente.EXTERNO);
        conversa = new Conversa(10L, cliente);
    }

    @Test
    @DisplayName("Depois do aviso, SIM registra o consentimento e libera o bot")
    void aceite() {
        tino.atender(conversa, "oi");

        Mensagem resposta = tino.atender(conversa, "sim");

        assertTrue(cliente.consentiu());
        assertEquals(Chatbot.VERSAO_DA_POLITICA, cliente.getConsentimento().getVersaoPolitica());
        assertEquals(EstadoConversa.COM_BOT, conversa.getEstado());
        assertEquals(Chatbot.TEXTO_APOS_ACEITE, resposta.getConteudo());
        assertTrue(tino.atender(conversa, "qual o prazo para troca?").getConteudo().contains("30 dias"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Sim", "SIM!", "sim, pode continuar", "Pode", "aceito", "ok", "Claro." })
    @DisplayName("O aceite aceita as formas comuns de dizer sim")
    void formasDeAceite(String fala) {
        tino.atender(conversa, "oi");
        tino.atender(conversa, fala);

        assertTrue(cliente.consentiu(), fala);
    }

    @ParameterizedTest
    @ValueSource(strings = { "não", "Nao, obrigado", "N" })
    @DisplayName("NÃO é respeitado: nada é registrado e o bot explica como voltar atrás")
    void recusa(String fala) {
        tino.atender(conversa, "oi");

        Mensagem resposta = tino.atender(conversa, fala);

        assertFalse(cliente.consentiu());
        assertEquals(EstadoConversa.AGUARDANDO_CONSENTIMENTO, conversa.getEstado());
        assertEquals(Chatbot.TEXTO_APOS_RECUSA, resposta.getConteudo());
    }

    @Test
    @DisplayName("Um SIM antes de ver o aviso não vale como aceite")
    void simAntesDoAviso() {
        Mensagem resposta = tino.atender(conversa, "sim");

        assertFalse(cliente.consentiu());
        assertEquals(Chatbot.AVISO_PRIVACIDADE, resposta.getConteudo());
    }

    @Test
    @DisplayName("Outra fala depois do aviso recebe o aviso de novo")
    void outraFalaRepeteOAviso() {
        tino.atender(conversa, "oi");

        Mensagem resposta = tino.atender(conversa, "qual o prazo para troca?");

        assertFalse(cliente.consentiu());
        assertEquals(Chatbot.AVISO_PRIVACIDADE, resposta.getConteudo());
    }

    @Test
    @DisplayName("Quem já aceitou numa conversa anterior não precisa aceitar de novo")
    void consentimentoAnterior() {
        Conversa anterior = new Conversa(9L, cliente);
        tino.atender(anterior, "oi");
        tino.atender(anterior, "sim");

        // Esta conversa nasceu antes do aceite, ainda aguardando consentimento.
        Mensagem resposta = tino.atender(conversa, "qual o prazo para troca?");

        assertEquals(EstadoConversa.COM_BOT, conversa.getEstado());
        assertTrue(resposta.getConteudo().contains("30 dias"));
    }
}
