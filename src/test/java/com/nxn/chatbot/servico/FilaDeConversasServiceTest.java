package com.nxn.chatbot.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.nxn.chatbot.dominio.Atendente;
import com.nxn.chatbot.dominio.Cliente;
import com.nxn.chatbot.dominio.Conversa;
import com.nxn.chatbot.dominio.EstadoConversa;
import com.nxn.chatbot.dominio.MotivoEscalonamento;
import com.nxn.chatbot.dominio.PerfilAtendente;
import com.nxn.chatbot.dominio.StatusTicket;
import com.nxn.chatbot.dominio.TipoCliente;
import com.nxn.chatbot.dto.ItemDaFila;
import com.nxn.chatbot.dto.ResumoDaFila;
import com.nxn.chatbot.repositorio.FilaRepositorioEmMemoria;

/** Regras da fila, exercitadas sem banco: o servico depende da porta. */
class FilaDeConversasServiceTest {

    private FilaRepositorioEmMemoria repositorio;
    private FilaDeConversasService servico;
    private Atendente bruno;

    @BeforeEach
    void preparar() {
        repositorio = new FilaRepositorioEmMemoria();
        servico = new FilaDeConversasService(repositorio);

        bruno = new Atendente(1L, "Bruno Dias", "bruno@wxn.com.br", PerfilAtendente.ATENDENTE);
        bruno.entrarEmServico();
        repositorio.registrar(bruno);
    }

    private Conversa escalonar(Long id, String telefone, String fala, MotivoEscalonamento motivo) {
        Cliente cliente = new Cliente(id, "Cliente " + id, telefone, TipoCliente.EXTERNO);
        cliente.registrarConsentimento("v1");
        Conversa conversa = new Conversa(id, cliente);
        conversa.registrarMensagem(cliente, fala);
        conversa.escalonar(id, motivo);
        repositorio.registrar(conversa);
        return conversa;
    }

    private Conversa resolvidaPeloBot(Long id, String telefone) {
        Cliente cliente = new Cliente(id, "Cliente " + id, telefone, TipoCliente.EXTERNO);
        cliente.registrarConsentimento("v1");
        Conversa conversa = new Conversa(id, cliente);
        conversa.registrarMensagem(cliente, "qual o prazo para troca?");
        conversa.encerrar();
        repositorio.registrar(conversa);
        return conversa;
    }

    @Test
    @DisplayName("Quem aguarda aparece antes de quem já está em atendimento")
    void aguardandoVemPrimeiro() {
        escalonar(1L, "5581999990001", "cobrança duplicada", MotivoEscalonamento.BAIXA_CONFIANCA);
        escalonar(2L, "5581999990002", "quero um atendente", MotivoEscalonamento.SOLICITACAO_CLIENTE);
        servico.assumir(1L, bruno.getId());

        List<ItemDaFila> fila = servico.listarFila();

        assertEquals(2, fila.size());
        assertEquals(StatusTicket.ABERTO, fila.get(0).status());
        assertEquals(StatusTicket.EM_ATENDIMENTO, fila.get(1).status());
    }

    @Test
    @DisplayName("Dentro da fila, o caso mais antigo fica no topo")
    void maisAntigoNoTopo() {
        escalonar(1L, "5581999990001", "primeiro", MotivoEscalonamento.BAIXA_CONFIANCA);
        escalonar(2L, "5581999990002", "segundo", MotivoEscalonamento.BAIXA_CONFIANCA);
        escalonar(3L, "5581999990003", "terceiro", MotivoEscalonamento.BAIXA_CONFIANCA);

        List<ItemDaFila> fila = servico.listarFila();

        assertEquals(List.of(1L, 2L, 3L), fila.stream().map(ItemDaFila::ticketId).toList());
    }

    @Test
    @DisplayName("O telefone completo nunca atravessa a API")
    void contatoVemMascarado() {
        escalonar(1L, "5581999994821", "cobrança duplicada", MotivoEscalonamento.BAIXA_CONFIANCA);

        ItemDaFila item = servico.listarFila().get(0);

        assertEquals("*********4821", item.contato());
        assertFalse(item.contato().contains("5581999994821"));
    }

    @Test
    @DisplayName("A prévia mostra a última fala do cliente, truncada")
    void previaDaConversa() {
        String falaLonga = "o cupom BLACK30 não aplicou no meu carrinho, a cobrança veio dobrada "
                + "e ninguém me respondeu até agora";
        escalonar(1L, "5581999990001", falaLonga, MotivoEscalonamento.BAIXA_CONFIANCA);

        ItemDaFila item = servico.listarFila().get(0);

        assertTrue(item.ultimaMensagem().endsWith("…"));
        assertTrue(item.ultimaMensagem().length() <= 80);
        assertTrue(item.ultimaMensagem().startsWith("o cupom BLACK30"));
    }

    @Test
    @DisplayName("Ticket na fila ainda não tem atendente")
    void semAtendenteNaFila() {
        escalonar(1L, "5581999990001", "cobrança duplicada", MotivoEscalonamento.BAIXA_CONFIANCA);

        assertNull(servico.listarFila().get(0).atendente());
    }

    @Test
    @DisplayName("Assumir tira a conversa do bot e registra o atendente")
    void assumirMudaOEstado() {
        Conversa conversa = escalonar(1L, "5581999990001", "cobrança duplicada",
                MotivoEscalonamento.BAIXA_CONFIANCA);

        ItemDaFila item = servico.assumir(1L, bruno.getId());

        assertEquals(StatusTicket.EM_ATENDIMENTO, item.status());
        assertEquals("Bruno Dias", item.atendente());
        assertEquals(EstadoConversa.COM_ATENDENTE, conversa.getEstado());
    }

    @Test
    @DisplayName("Ticket ou atendente inexistente é recusado")
    void assumirInexistente() {
        escalonar(1L, "5581999990001", "cobrança duplicada", MotivoEscalonamento.BAIXA_CONFIANCA);

        assertThrows(IllegalArgumentException.class, () -> servico.assumir(99L, bruno.getId()));
        assertThrows(IllegalArgumentException.class, () -> servico.assumir(1L, 99L));
    }

    @Test
    @DisplayName("O serviço não reimplementa a validação: quem recusa é o domínio")
    void validacaoFicaNoDominio() {
        escalonar(1L, "5581999990001", "cobrança duplicada", MotivoEscalonamento.BAIXA_CONFIANCA);
        Atendente foraDeServico = new Atendente(2L, "Ana Lima", "ana@wxn.com.br", PerfilAtendente.GESTOR);
        repositorio.registrar(foraDeServico);

        assertThrows(IllegalStateException.class, () -> servico.assumir(1L, 2L));

        servico.assumir(1L, bruno.getId());
        assertThrows(IllegalStateException.class, () -> servico.assumir(1L, bruno.getId()));
    }

    @Test
    @DisplayName("O resumo calcula a taxa de contenção do dia")
    void resumoCalculaContencao() {
        escalonar(1L, "5581999990001", "cobrança duplicada", MotivoEscalonamento.BAIXA_CONFIANCA);
        resolvidaPeloBot(10L, "5581999990010");
        resolvidaPeloBot(11L, "5581999990011");
        resolvidaPeloBot(12L, "5581999990012");

        ResumoDaFila resumo = servico.resumoDoDia();

        assertEquals(1, resumo.naFila());
        assertEquals(0, resumo.emAtendimento());
        assertEquals(4, resumo.total());
        assertEquals(3, resumo.resolvidasPeloBot());
        assertEquals(0.75, resumo.taxaContencao(), 0.0001);
    }

    @Test
    @DisplayName("Dia sem conversa nenhuma não vira contenção de 100%")
    void resumoSemConversas() {
        ResumoDaFila resumo = servico.resumoDoDia();

        assertEquals(0, resumo.total());
        assertEquals(0.0, resumo.taxaContencao(), 0.0001);
        assertEquals(0, resumo.esperaMediaEmSegundos());
    }

    @Test
    @DisplayName("Conversa assumida sai da contagem de resolvidas pelo bot")
    void assumidaNaoContaComoContencao() {
        escalonar(1L, "5581999990001", "cobrança duplicada", MotivoEscalonamento.BAIXA_CONFIANCA);
        servico.assumir(1L, bruno.getId());

        ResumoDaFila resumo = servico.resumoDoDia();

        assertEquals(1, resumo.total());
        assertEquals(0, resumo.resolvidasPeloBot());
        assertEquals(0.0, resumo.taxaContencao(), 0.0001);
    }
}
