package com.wxn.chatbot.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.wxn.chatbot.config.TinoConfig;

/** O atendimento guiado, com os fluxos de exemplo da WXN. */
class FluxoGuiadoTest {

    private Chatbot tino;
    private Cliente cliente;
    private Conversa conversa;

    @BeforeEach
    void preparar() {
        tino = new TinoConfig().tino();
        cliente = new Cliente(1L, "Fernanda Lima", "5581955550001", TipoCliente.EXTERNO);
        cliente.registrarConsentimento("v1");
        conversa = new Conversa(10L, cliente);
    }

    private String falar(String fala) {
        return tino.atender(conversa, fala).getConteudo();
    }

    private String etapaAtual() {
        return conversa.getContexto().map(c -> c.getFluxo() + "/" + c.getEtapa()).orElse(null);
    }

    @ParameterizedTest(name = "\"{0}\" abre {1}")
    @CsvSource({
            "Meu pedido chegou quebrado, defeito/pedido",
            "o produto veio com defeito, defeito/pedido",
            "quero trocar um produto, troca/motivo",
            "meu pedido está atrasado, pedido/pedido",
            "preciso cancelar uma compra, cancelamento/pedido",
            "o cupom não aplicou e cobraram em dobro, pagamento/tipo",
            "segunda via do boleto, pagamento/tipo",
    })
    @DisplayName("O Tino reconhece o assunto e abre o fluxo certo")
    void reconheceOAssunto(String fala, String esperado) {
        falar(fala);

        assertEquals(esperado, etapaAtual());
        assertEquals(EstadoConversa.COM_BOT, conversa.getEstado());
    }

    @Test
    @DisplayName("Pergunta de informação continua com a base, que pergunta se resolveu")
    void perguntaDaBase() {
        String resposta = falar("Qual o prazo para troca?");

        assertTrue(resposta.startsWith("A troca pode ser solicitada em até 30 dias"), resposta);
        assertTrue(resposta.contains("Isso resolveu a sua dúvida?"), resposta);
        assertEquals(List.of("Sim, resolveu", "Não, preciso de mais ajuda"), tino.opcoesAtuais(conversa));
    }

    @Test
    @DisplayName("Resolvido pela base: a conversa termina sem ticket e conta na contenção")
    void resolvidoPelaBase() {
        falar("Qual o prazo para troca?");
        String despedida = falar("sim, obrigado");

        assertEquals(Chatbot.TEXTO_DESPEDIDA, despedida);
        assertEquals(EstadoConversa.ENCERRADA, conversa.getEstado());
        assertTrue(conversa.resolvidaPeloBot());
    }

    @Test
    @DisplayName("\"Não resolveu\" leva ao menu, não ao encerramento")
    void naoResolveu() {
        falar("Qual o prazo para troca?");
        String resposta = falar("não resolveu");

        assertEquals("menu/assunto", etapaAtual());
        assertTrue(resposta.contains("Sobre o que você quer falar?"), resposta);
    }

    @Test
    @DisplayName("O que não se reconhece abre o menu, em vez de ir direto para a fila")
    void naoEntendeuMostraOMenu() {
        String resposta = falar("blá blá");

        assertTrue(resposta.startsWith(Chatbot.TEXTO_NAO_ENTENDI), resposta);
        assertTrue(resposta.contains("\n1. Acompanhar um pedido"), resposta);
        assertTrue(conversa.getTicket().isEmpty());
    }

    @Test
    @DisplayName("No menu vale o número, o rótulo ou uma palavra parecida")
    void escolhaNoMenu() {
        falar("oi");
        falar("4");
        assertEquals("cancelamento/pedido", etapaAtual());

        falar("menu");
        falar("Pagamento");
        assertEquals("pagamento/tipo", etapaAtual());

        falar("menu");
        falar("meu produto chegou quebrado");
        assertEquals("defeito/pedido", etapaAtual());
    }

    @Test
    @DisplayName("O número do pedido é extraído da frase, e resposta sem número pede de novo")
    void numeroDoPedido() {
        falar("Meu pedido chegou quebrado");

        String ajuda = falar("não sei onde vejo");
        assertTrue(ajuda.startsWith("Não encontrei um número de pedido"), ajuda);
        assertEquals("defeito/pedido", etapaAtual());

        falar("é o pedido nº 123456, obrigada");
        assertEquals("123456", conversa.getContexto().orElseThrow().getDados().get("pedido"));
        assertEquals("defeito/problema", etapaAtual());
    }

    @Test
    @DisplayName("Três respostas que não servem na mesma etapa: o caso vai para uma pessoa")
    void tentativasEsgotadas() {
        falar("Meu pedido chegou quebrado");
        falar("123456");
        falar("hmm");
        falar("como assim");
        String resposta = falar("???");

        assertEquals(EstadoConversa.AGUARDANDO_ATENDENTE, conversa.getEstado());
        assertEquals(MotivoEscalonamento.TENTATIVAS_ESGOTADAS, conversa.getTicket().orElseThrow().getMotivo());
        assertTrue(resposta.startsWith(Chatbot.TEXTO_FALLBACK), resposta);
        assertTrue(resposta.contains("• Pedido: 123456"), resposta);
    }

    @Test
    @DisplayName("Pedir um atendente no meio do fluxo passa o caso com o que já foi coletado")
    void atendenteNoMeio() {
        falar("preciso cancelar uma compra");
        falar("987654");
        String resposta = falar("quero falar com um atendente");

        assertEquals(MotivoEscalonamento.SOLICITACAO_CLIENTE, conversa.getTicket().orElseThrow().getMotivo());
        assertTrue(resposta.contains("• Assunto: Cancelar uma compra\n• Pedido: 987654"), resposta);
        assertEquals("Cancelar uma compra · Pedido: 987654", conversa.getAssunto().orElseThrow());
        assertTrue(conversa.getContexto().isEmpty());
    }

    @Test
    @DisplayName("Quem muda de assunto no meio do fluxo vai para o fluxo novo, levando o que já disse")
    void mudaDeAssunto() {
        falar("meu pedido está atrasado");
        falar("555123");
        falar("na verdade quero cancelar");

        assertEquals("cancelamento/envio", etapaAtual());
        assertEquals("555123", conversa.getContexto().orElseThrow().getDados().get("pedido"));
    }

    @Test
    @DisplayName("Na troca, \"veio com defeito\" leva ao atendimento de produto com defeito")
    void trocaQueViraDefeito() {
        falar("quero trocar um produto");
        String resposta = falar("Veio com defeito");

        assertEquals("defeito/pedido", etapaAtual());
        assertTrue(resposta.startsWith("Nesse caso, vou te ajudar pelo atendimento de produto com defeito."),
                resposta);
    }

    @Test
    @DisplayName("A foto é opcional: sem foto o atendimento segue")
    void fotoOpcional() {
        falar("produto com defeito");
        falar("123456");
        falar("Não funciona direito");
        String resposta = falar("não consigo agora");

        assertEquals("defeito/preferencia", etapaAtual());
        assertTrue(resposta.startsWith("Sem problema"), resposta);
        assertEquals("não enviada", conversa.getContexto().orElseThrow().getDados().get("foto"));
    }

    @Test
    @DisplayName("Concluir sem atendente pergunta se pode ajudar em mais alguma coisa")
    void concluirSemAtendente() {
        falar("meu pedido está atrasado");
        falar("123456");
        String resposta = falar("Ainda está no prazo");

        assertTrue(resposta.contains("link de rastreio"), resposta);
        assertTrue(resposta.endsWith("2. Não, era só isso"), resposta);
        falar("não, era só isso");
        assertEquals(EstadoConversa.ENCERRADA, conversa.getEstado());
        assertTrue(conversa.resolvidaPeloBot());
    }

    @Test
    @DisplayName("Depois de passar o caso, o Tino não conduz mais: não há opções")
    void semOpcoesComHumano() {
        falar("quero falar com um atendente");

        assertEquals(List.of(), tino.opcoesAtuais(conversa));
        assertThrows(IllegalStateException.class, () -> tino.atender(conversa, "oi"));
    }

    @Test
    @DisplayName("O contexto vai para o banco como JSON e volta igual")
    void contextoEmJson() {
        ContextoDoFluxoConverter conversor = new ContextoDoFluxoConverter();
        ContextoDoFluxo contexto = new ContextoDoFluxo("defeito", "foto",
                Map.of("pedido", "123456", "problema", "Não funciona direito"), 1);

        String json = conversor.convertToDatabaseColumn(contexto);

        assertTrue(json.contains("\"intencao\":\"defeito\""), json);
        assertTrue(json.contains("\"versao\":1"), json);
        assertEquals(contexto, conversor.convertToEntityAttribute(json));
        assertNull(conversor.convertToEntityAttribute("isto não é json"));
    }

    @Test
    @DisplayName("Fluxo com etapa que leva a uma etapa inexistente é recusado")
    void etapaInexistente() {
        Etapa quebrada = new EtapaDeTexto("pedido", "Qual o pedido?", "pedido", null, null,
                Transicao.irPara("nao-existe"));

        assertThrows(IllegalArgumentException.class,
                () -> new Fluxo("x", "X", List.of(), List.of(), List.of(quebrada), Map.of()));
    }

    @Test
    @DisplayName("Fluxo que abre um fluxo inexistente é pego antes de atender alguém")
    void fluxoInexistente() {
        Chatbot outro = new Chatbot("Tino", "1.0", 0.45);
        Atendente gestor = new Atendente(1L, "Ana Lima", "ana@wxn.com.br", PerfilAtendente.GESTOR);
        outro.publicarFluxo(new Fluxo(Chatbot.FLUXO_MENU, "Menu", List.of(), List.of(), List.of(
                new EtapaDeEscolha("assunto", "Sobre o quê?", null, List.of(
                        new Opcao("Fantasma", List.of(), Transicao.iniciarFluxo("fantasma")),
                        new Opcao("Atendente", List.of(), Transicao.passarParaAtendente(null))))),
                Map.of()), gestor);

        assertThrows(IllegalStateException.class, outro::validarFluxos);
    }

    @Test
    @DisplayName("Só gestor publica fluxo, e os fluxos com _ são do próprio Tino")
    void publicarFluxo() {
        Atendente bruno = new Atendente(3L, "Bruno Dias", "bruno@wxn.com.br", PerfilAtendente.ATENDENTE);
        Atendente ana = new Atendente(2L, "Ana Lima", "ana@wxn.com.br", PerfilAtendente.GESTOR);
        Fluxo qualquer = TinoConfigFluxos.cancelamento();
        Fluxo reservado = new Fluxo("_meu", "Meu", List.of(), List.of(), qualquer.getEtapas(), Map.of());

        assertThrows(IllegalStateException.class, () -> tino.publicarFluxo(qualquer, bruno));
        assertThrows(IllegalArgumentException.class, () -> tino.publicarFluxo(reservado, ana));
    }

    @Test
    @DisplayName("Sem fluxos publicados, o Tino continua só respondendo ou escalando")
    void semFluxos() {
        Chatbot soBase = new Chatbot("Tino", "1.0", 0.45);
        Conversa outra = new Conversa(11L, cliente);

        assertEquals(Chatbot.TEXTO_FALLBACK, soBase.atender(outra, "meu pedido chegou quebrado").getConteudo());
        assertFalse(outra.getTicket().isEmpty());
    }

    /** Acesso aos fluxos de exemplo, que no TinoConfig sao de pacote. */
    private static final class TinoConfigFluxos {
        static Fluxo cancelamento() {
            return new TinoConfig().tino().getFluxos().stream()
                    .filter(f -> f.getId().equals("cancelamento")).findFirst().orElseThrow();
        }
    }
}
