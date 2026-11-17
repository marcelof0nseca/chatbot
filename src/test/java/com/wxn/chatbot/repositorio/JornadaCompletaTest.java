package com.wxn.chatbot.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.wxn.chatbot.dominio.StatusTicket;
import com.wxn.chatbot.dto.ConversaDoAtendimento;
import com.wxn.chatbot.dto.ConversaDoCliente;
import com.wxn.chatbot.dto.ItemDaFila;
import com.wxn.chatbot.dto.MensagemDoHistorico;
import com.wxn.chatbot.dto.TransferenciaDoHistorico;
import com.wxn.chatbot.servico.AtendimentoService;
import com.wxn.chatbot.servico.CanalDoClienteService;
import com.wxn.chatbot.servico.FilaDeConversasService;

import jakarta.persistence.EntityManager;

/**
 * As duas historias seguidas, contra o banco: o cliente escreve, o Tino
 * escala, o caso entra na fila, e assumido, transferido, e quem recebe ve o
 * historico inteiro. Cada passo limpa o contexto do JPA, como se fosse uma
 * requisicao separada.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:jornada-teste;DB_CLOSE_DELAY=-1;TIME ZONE=America/Recife",
        "spring.jpa.show-sql=false"
})
@Transactional
class JornadaCompletaTest {

    private static final String TELEFONE = "5581955550001";

    @Autowired
    private CanalDoClienteService canal;

    @Autowired
    private FilaDeConversasService fila;

    @Autowired
    private AtendimentoService atendimento;

    @Autowired
    private EntityManager em;

    private ConversaDoCliente escrever(String texto) {
        ConversaDoCliente conversa = canal.receberMensagem(TELEFONE, "Fernanda Lima", texto);
        em.flush();
        em.clear();
        return conversa;
    }

    @Test
    @DisplayName("Do cliente ao atendente que recebe a transferência, com o histórico inteiro")
    void jornada() {
        // O cliente fala com o Tino: aviso, aceite, uma resposta da base e um escalonamento.
        assertEquals("AGUARDANDO_CONSENTIMENTO", escrever("oi").situacao());
        assertEquals("COM_BOT", escrever("sim").situacao());
        escrever("Qual o prazo para troca?");
        ConversaDoCliente escalada = escrever("Meu pedido chegou quebrado");
        assertEquals("AGUARDANDO_ATENDENTE", escalada.situacao());
        Long ticketId = escalada.ticketId();
        assertNotNull(ticketId);

        // Primeira historia: o caso esta na fila e e assumido.
        ItemDaFila naFila = fila.listarFila().stream()
                .filter(item -> item.ticketId().equals(ticketId)).findFirst().orElseThrow();
        assertEquals(StatusTicket.ABERTO, naFila.status());
        assertEquals("Meu pedido chegou quebrado", naFila.ultimaMensagem());
        fila.assumir(ticketId, 1001L);
        em.flush();
        em.clear();

        // Com o caso no humano, o cliente continua escrevendo e o bot fica quieto.
        ConversaDoCliente comHumano = escrever("alguém aí?");
        assertEquals("COM_ATENDENTE", comHumano.situacao());
        assertEquals("Bruno Dias", comHumano.atendente());

        // Segunda historia: o Bruno transfere para a Ana.
        fila.transferir(ticketId, 1001L, 1002L, "Avaria na entrega é com a Ana");
        em.flush();
        em.clear();

        // Quem recebe ve o caso como seu, com o historico inteiro e a transferencia.
        ConversaDoAtendimento daAna = atendimento.conversaDoTicket(ticketId);
        assertEquals("Ana Lima", daAna.atendente());
        List<String> falas = daAna.mensagens().stream().map(MensagemDoHistorico::conteudo).toList();
        assertEquals("oi", falas.get(0));
        assertTrue(falas.contains("Meu pedido chegou quebrado"));
        assertEquals("alguém aí?", falas.get(falas.size() - 1));
        assertEquals(List.of("Bruno Dias"),
                daAna.transferencias().stream().map(TransferenciaDoHistorico::de).toList());
        assertNotNull(daAna.consentimentoEm());
    }

    @Test
    @DisplayName("Um cliente da carga de exemplo que volta a escrever continua a conversa que está na fila")
    void clienteDaCarga() {
        ConversaDoCliente conversa = canal.receberMensagem("5581999994821", null, "ainda estou esperando");

        assertEquals("AGUARDANDO_ATENDENTE", conversa.situacao());
        assertEquals(1001L, conversa.ticketId());
        List<MensagemDoHistorico> mensagens = conversa.mensagens();
        assertEquals("ainda estou esperando", mensagens.get(mensagens.size() - 1).conteudo());
    }
}
