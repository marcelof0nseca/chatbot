package com.wxn.chatbot.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.wxn.chatbot.config.TinoConfig;
import com.wxn.chatbot.dominio.Atendente;
import com.wxn.chatbot.dominio.Chatbot;
import com.wxn.chatbot.dominio.Conversa;
import com.wxn.chatbot.dominio.PerfilAtendente;
import com.wxn.chatbot.dto.ConversaDoCliente;
import com.wxn.chatbot.dto.MensagemDoHistorico;
import com.wxn.chatbot.repositorio.ConversaComClienteRepositorioEmMemoria;

/** A porta de entrada das mensagens do cliente, sem banco. */
class CanalDoClienteServiceTest {

    private static final String TELEFONE = "+55 81 95555-0001";

    private ConversaComClienteRepositorioEmMemoria repositorio;
    private CanalDoClienteService canal;

    @BeforeEach
    void preparar() {
        repositorio = new ConversaComClienteRepositorioEmMemoria();
        canal = new CanalDoClienteService(repositorio, new TinoConfig().tino());
    }

    private ConversaDoCliente escrever(String texto) {
        return canal.receberMensagem(TELEFONE, "Fernanda", texto);
    }

    private static List<String> falas(ConversaDoCliente conversa) {
        return conversa.mensagens().stream().map(MensagemDoHistorico::conteudo).toList();
    }

    @Test
    @DisplayName("Primeiro contato: o cliente é cadastrado pelo telefone e recebe o aviso de privacidade")
    void primeiroContato() {
        ConversaDoCliente conversa = escrever("oi");

        assertEquals(1, repositorio.totalDeClientes());
        assertEquals("AGUARDANDO_CONSENTIMENTO", conversa.situacao());
        assertEquals(List.of("oi", Chatbot.AVISO_PRIVACIDADE), falas(conversa));
    }

    @Test
    @DisplayName("O mesmo telefone, escrito de outro jeito, continua a mesma conversa")
    void mesmoCliente() {
        escrever("oi");
        canal.receberMensagem("5581955550001", null, "sim");

        assertEquals(1, repositorio.totalDeClientes());
        assertEquals(1, repositorio.conversas().size());
    }

    @Test
    @DisplayName("Depois do aceite, o Tino responde pela base")
    void respondePelaBase() {
        escrever("oi");
        escrever("sim");

        ConversaDoCliente conversa = escrever("Qual o prazo para troca?");

        assertEquals("COM_BOT", conversa.situacao());
        List<String> falas = falas(conversa);
        assertEquals(true, falas.get(falas.size() - 1).contains("30 dias"));
    }

    @Test
    @DisplayName("O que a base não cobre vira um caso para atendimento humano")
    void escala() {
        escrever("oi");
        escrever("sim");

        ConversaDoCliente conversa = escrever("Meu pedido chegou quebrado");

        assertEquals("AGUARDANDO_ATENDENTE", conversa.situacao());
        List<String> falas = falas(conversa);
        assertEquals(Chatbot.TEXTO_FALLBACK, falas.get(falas.size() - 1));
        assertEquals(true, repositorio.conversas().get(0).getTicket().isPresent());
    }

    @Test
    @DisplayName("Com o caso no humano, o bot não responde: a fala fica registrada para o atendente")
    void botQuietoComHumano() {
        escrever("oi");
        escrever("sim");
        escrever("Meu pedido chegou quebrado");
        Conversa conversa = repositorio.conversas().get(0);
        Atendente bruno = new Atendente(1L, "Bruno Dias", "bruno@wxn.com.br", PerfilAtendente.ATENDENTE);
        bruno.entrarEmServico();
        conversa.getTicket().orElseThrow().assumir(bruno);

        ConversaDoCliente depois = escrever("alguém aí?");

        assertEquals("COM_ATENDENTE", depois.situacao());
        assertEquals("Bruno Dias", depois.atendente());
        List<String> falas = falas(depois);
        assertEquals("alguém aí?", falas.get(falas.size() - 1));
    }

    @Test
    @DisplayName("Sem nome, o cliente é cadastrado com o final do telefone")
    void nomePadrao() {
        canal.receberMensagem(TELEFONE, " ", "oi");

        assertEquals("Cliente 0001", repositorio.conversas().get(0).getCliente().getNome());
    }

    @Test
    @DisplayName("Mensagem vazia, telefone ausente ou curto são dados inválidos")
    void dadosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> escrever("  "));
        assertThrows(IllegalArgumentException.class, () -> canal.receberMensagem(null, "x", "oi"));
        assertThrows(IllegalArgumentException.class, () -> canal.receberMensagem("8199", "x", "oi"));
    }

    @Test
    @DisplayName("Telefone sem conversa responde não encontrado")
    void semConversa() {
        assertThrows(NaoEncontradoException.class, () -> canal.conversaAtual(TELEFONE));
    }
}
