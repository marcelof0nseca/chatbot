package com.nxn.chatbot.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Exercita as regras do modelo de entidades. */
class DominioTest {

    private Chatbot tino;
    private Cliente cliente;
    private Atendente gestor;
    private Atendente atendente;

    @BeforeEach
    void preparar() {
        tino = new Chatbot("Tino", "1.0", 0.45);
        cliente = new Cliente(1L, "Maria Souza", "+55 81 99999-4821", TipoCliente.EXTERNO);
        gestor = new Atendente(2L, "Ana Lima", "ana@wxn.com.br", PerfilAtendente.GESTOR);
        atendente = new Atendente(3L, "Bruno Dias", "bruno@wxn.com.br", PerfilAtendente.ATENDENTE);

        ArtigoConhecimento troca = new ArtigoConhecimento(
                "Qual o prazo para troca?",
                List.of("prazo troca produto"),
                "A troca pode ser solicitada em até 30 dias corridos após o recebimento.",
                "Trocas", true);
        tino.publicarArtigo(troca, gestor);
    }

    @Test
    @DisplayName("Atendente comum não altera a base de conhecimento")
    void apenasGestorPublica() {
        ArtigoConhecimento novo = new ArtigoConhecimento(
                "Como acompanho meu pedido?", List.of(), "Pelo link enviado por e-mail.",
                "Entrega", true);
        assertThrows(IllegalStateException.class, () -> tino.publicarArtigo(novo, atendente));
    }

    @Test
    @DisplayName("Cliente do WhatsApp nasce sem consentimento e sem e-mail")
    void clienteDoWhatsapp() {
        assertFalse(cliente.consentiu());
        assertEquals(null, cliente.getEmail());
    }

    @Test
    @DisplayName("Sem consentimento o bot só envia o aviso de privacidade")
    void exigeConsentimento() {
        Conversa conversa = new Conversa(10L, cliente);
        assertEquals(EstadoConversa.AGUARDANDO_CONSENTIMENTO, conversa.getEstado());

        Mensagem resposta = tino.atender(conversa, "qual o prazo pra troca?");
        assertEquals(Chatbot.AVISO_PRIVACIDADE, resposta.getConteudo());
    }

    @Test
    @DisplayName("O consentimento pode ser registrado e revogado (LGPD)")
    void consentimentoVaiEVolta() {
        cliente.registrarConsentimento("v1");
        assertTrue(cliente.consentiu());
        assertThrows(IllegalStateException.class, () -> cliente.registrarConsentimento("v1"));

        cliente.revogarConsentimento();
        assertFalse(cliente.consentiu());
    }

    @Test
    @DisplayName("Com confiança acima do limite o bot responde pela base")
    void respondePelaBase() {
        cliente.registrarConsentimento("v1");
        Conversa conversa = new Conversa(10L, cliente);

        Mensagem resposta = tino.atender(conversa, "qual o prazo para troca?");

        assertTrue(resposta.getConteudo().contains("30 dias"));
        assertEquals(TipoRemetente.CHATBOT, resposta.getRemetente());
        assertEquals(tino, resposta.getAutor());
        assertEquals(EstadoConversa.COM_BOT, conversa.getEstado());
        assertTrue(conversa.getTicket().isEmpty());
    }

    @Test
    @DisplayName("Abaixo do limite o bot não inventa: escalona com fallback")
    void fallbackEscalona() {
        cliente.registrarConsentimento("v1");
        Conversa conversa = new Conversa(10L, cliente);

        Mensagem resposta = tino.atender(conversa, "o cupom BLACK30 não aplicou e cobraram em dobro");

        assertEquals(Chatbot.TEXTO_FALLBACK, resposta.getConteudo());
        assertEquals(EstadoConversa.AGUARDANDO_ATENDENTE, conversa.getEstado());
        assertTrue(conversa.getTicket().isPresent());
        assertEquals(MotivoEscalonamento.BAIXA_CONFIANCA, conversa.getTicket().get().getMotivo());
    }

    @Test
    @DisplayName("Ticket assumido tira a conversa do bot e preserva o histórico")
    void handoffPreservaHistorico() {
        cliente.registrarConsentimento("v1");
        Conversa conversa = new Conversa(10L, cliente);
        tino.atender(conversa, "cobrança duplicada no cartão");

        Ticket ticket = conversa.getTicket().orElseThrow();
        atendente.entrarEmServico();
        ticket.assumir(atendente);

        assertEquals(StatusTicket.EM_ATENDIMENTO, ticket.getStatus());
        assertEquals(EstadoConversa.COM_ATENDENTE, conversa.getEstado());
        assertFalse(conversa.botPodeResponder());
        assertEquals(2, conversa.getTotalDeMensagens());
        assertEquals(atendente, ticket.getAtendente().orElseThrow());
    }

    @Test
    @DisplayName("Atendente fora de serviço não assume ticket")
    void atendenteForaDeServico() {
        cliente.registrarConsentimento("v1");
        Conversa conversa = new Conversa(10L, cliente);
        tino.atender(conversa, "cobrança duplicada no cartão");
        Ticket ticket = conversa.getTicket().orElseThrow();

        assertThrows(IllegalStateException.class, () -> ticket.assumir(atendente));
    }

    @Test
    @DisplayName("Resolver o ticket encerra a conversa")
    void resolverEncerra() {
        cliente.registrarConsentimento("v1");
        Conversa conversa = new Conversa(10L, cliente);
        tino.atender(conversa, "cobrança duplicada no cartão");
        Ticket ticket = conversa.getTicket().orElseThrow();
        atendente.entrarEmServico();
        ticket.assumir(atendente);

        ticket.resolver();

        assertEquals(StatusTicket.RESOLVIDO, ticket.getStatus());
        assertEquals(EstadoConversa.ENCERRADA, conversa.getEstado());
        assertThrows(IllegalStateException.class, ticket::resolver);
    }

    @Test
    @DisplayName("Fluxo completo do consentimento: aviso, aceite e liberação")
    void fluxoDoConsentimento() {
        Conversa conversa = new Conversa(10L, cliente);

        Mensagem aviso = tino.atender(conversa, "qual o prazo pra troca?");
        assertEquals(Chatbot.AVISO_PRIVACIDADE, aviso.getConteudo());
        assertEquals(EstadoConversa.AGUARDANDO_CONSENTIMENTO, conversa.getEstado());

        cliente.registrarConsentimento("v1");
        conversa.liberarAposConsentimento();
        assertEquals(EstadoConversa.COM_BOT, conversa.getEstado());

        Mensagem resposta = tino.atender(conversa, "qual o prazo para troca?");
        assertTrue(resposta.getConteudo().contains("30 dias"));
    }

    @Test
    @DisplayName("Liberar sem o cliente ter aceitado é recusado")
    void liberacaoSemAceite() {
        Conversa conversa = new Conversa(10L, cliente);
        assertThrows(IllegalStateException.class, conversa::liberarAposConsentimento);
    }

    @Test
    @DisplayName("Com a conversa no humano, o bot recusa o turno sem gravar a mensagem")
    void botNaoGravaQuandoRecusa() {
        cliente.registrarConsentimento("v1");
        Conversa conversa = new Conversa(10L, cliente);
        tino.atender(conversa, "cobrança duplicada no cartão");
        atendente.entrarEmServico();
        conversa.getTicket().orElseThrow().assumir(atendente);

        int antes = conversa.getTotalDeMensagens();
        assertThrows(IllegalStateException.class, () -> tino.atender(conversa, "e aí?"));
        assertEquals(antes, conversa.getTotalDeMensagens());
    }

    @Test
    @DisplayName("Ticket não ressuscita uma conversa já encerrada")
    void ticketNaoRessuscitaConversa() {
        cliente.registrarConsentimento("v1");
        Conversa conversa = new Conversa(10L, cliente);
        tino.atender(conversa, "cobrança duplicada no cartão");
        Ticket ticket = conversa.getTicket().orElseThrow();
        conversa.encerrar();

        atendente.entrarEmServico();
        assertThrows(IllegalStateException.class, () -> ticket.assumir(atendente));
        assertEquals(StatusTicket.ABERTO, ticket.getStatus());
        assertEquals(EstadoConversa.ENCERRADA, conversa.getEstado());
    }

    @Test
    @DisplayName("Conversa encerrada não aceita nova mensagem")
    void historicoNaoEReescrito() {
        cliente.registrarConsentimento("v1");
        Conversa conversa = new Conversa(10L, cliente);
        tino.atender(conversa, "qual o prazo para troca?");
        conversa.encerrar();

        assertTrue(conversa.resolvidaPeloBot());
        assertThrows(IllegalStateException.class, () -> conversa.registrarMensagem(cliente, "oi"));
    }

    @Test
    @DisplayName("A lista de mensagens é somente leitura")
    void colecaoEncapsulada() {
        cliente.registrarConsentimento("v1");
        Conversa conversa = new Conversa(10L, cliente);
        tino.atender(conversa, "qual o prazo para troca?");

        assertThrows(UnsupportedOperationException.class, () -> conversa.getMensagens().clear());
    }

    @Test
    @DisplayName("Artigo interno não chega a cliente externo")
    void artigoInternoFicaInterno() {
        ArtigoConhecimento interno = new ArtigoConhecimento(
                "Qual a política de desconto por volume?", List.of(),
                "Até 15% para pedidos acima de 500 unidades.", "Comercial", false);
        tino.publicarArtigo(interno, gestor);

        assertTrue(tino.melhorResposta("política de desconto por volume", TipoCliente.EXTERNO).isEmpty());
        assertTrue(tino.melhorResposta("política de desconto por volume", TipoCliente.INTERNO).isPresent());
    }

    @Test
    @DisplayName("Artigo desativado para de responder")
    void artigoDesativado() {
        ArtigoConhecimento artigo = tino.getBaseDeConhecimento().get(0);
        artigo.desativar();

        assertTrue(tino.melhorResposta("qual o prazo para troca?", TipoCliente.EXTERNO).isEmpty());
    }

    @Test
    @DisplayName("O gestor atualiza a resposta sem recriar o artigo")
    void gestorAtualizaArtigo() {
        ArtigoConhecimento artigo = tino.getBaseDeConhecimento().get(0);
        artigo.atualizar("Qual o prazo para troca?",
                "A troca pode ser solicitada em até 15 dias corridos.",
                List.of("prazo troca produto"));

        cliente.registrarConsentimento("v1");
        Conversa conversa = new Conversa(10L, cliente);
        Mensagem resposta = tino.atender(conversa, "qual o prazo para troca?");

        assertTrue(resposta.getConteudo().contains("15 dias"));
    }

    @Test
    @DisplayName("Entre dois artigos, o bot escolhe o de maior confiança")
    void escolheOMelhorArtigo() {
        ArtigoConhecimento entrega = new ArtigoConhecimento(
                "Como acompanho a entrega do meu pedido?", List.of(),
                "Pelo código de rastreio enviado por e-mail.", "Entrega", true);
        tino.publicarArtigo(entrega, gestor);

        RespostaCandidata escolhida =
                tino.melhorResposta("qual o prazo para troca?", TipoCliente.EXTERNO).orElseThrow();

        assertEquals("Qual o prazo para troca?", escolhida.getArtigo().getPergunta());
    }

    @Test
    @DisplayName("O telefone aparece mascarado na identificação")
    void telefoneMascarado() {
        assertEquals("*********4821", cliente.getIdentificacao());
    }

    @Test
    @DisplayName("Dados inválidos são recusados na criação")
    void validacaoNoConstrutor() {
        assertThrows(IllegalArgumentException.class,
                () -> new Cliente(1L, "", "5581999994821", TipoCliente.EXTERNO));
        assertThrows(IllegalArgumentException.class,
                () -> new Cliente(1L, "João", "123", TipoCliente.EXTERNO));
        assertThrows(IllegalArgumentException.class,
                () -> new Atendente(2L, "Ana", "sem-arroba", PerfilAtendente.GESTOR));
        assertThrows(IllegalArgumentException.class,
                () -> new Chatbot("Tino", "1.0", 1.5));
    }
}
