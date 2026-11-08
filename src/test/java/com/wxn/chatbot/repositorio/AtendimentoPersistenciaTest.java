package com.wxn.chatbot.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.wxn.chatbot.dominio.StatusTicket;
import com.wxn.chatbot.dto.AtendenteResumo;
import com.wxn.chatbot.dto.ConversaDoAtendimento;
import com.wxn.chatbot.dto.MensagemDoHistorico;
import com.wxn.chatbot.dto.TransferenciaDoHistorico;
import com.wxn.chatbot.servico.AtendimentoService;
import com.wxn.chatbot.servico.FilaDeConversasService;
import com.wxn.chatbot.servico.NaoEncontradoException;

import jakarta.persistence.EntityManager;

/** A conversa da Tela 7 lida do banco, com a carga do data.sql. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:atendimento-teste;DB_CLOSE_DELAY=-1;TIME ZONE=America/Recife",
        "spring.jpa.show-sql=false"
})
@Transactional
class AtendimentoPersistenciaTest {

    @Autowired
    private AtendimentoService atendimento;

    @Autowired
    private FilaDeConversasService fila;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("O histórico vem completo e em ordem cronológica, com o bot identificado")
    void historicoCompleto() {
        ConversaDoAtendimento conversa = atendimento.conversaDoTicket(1001L);
        List<MensagemDoHistorico> mensagens = conversa.mensagens();

        assertEquals(List.of("CLIENTE", "CHATBOT", "CLIENTE", "CHATBOT"),
                mensagens.stream().map(MensagemDoHistorico::autor).toList());
        assertEquals("oi, tenho uma dúvida na cobrança", mensagens.get(0).conteudo());
        assertEquals("Tino", mensagens.get(1).identificacao());
        for (int i = 1; i < mensagens.size(); i++) {
            assertFalse(mensagens.get(i).dataHora().isBefore(mensagens.get(i - 1).dataHora()),
                    "mensagem fora de ordem na posição " + i);
        }
    }

    @Test
    @DisplayName("O telefone completo não aparece em lugar nenhum da conversa")
    void telefoneMascarado() {
        ConversaDoAtendimento conversa = atendimento.conversaDoTicket(1001L);

        assertEquals("*********4821", conversa.contato());
        assertEquals("*********4821", conversa.mensagens().get(0).identificacao());
        assertFalse(conversa.toString().contains("5581999994821"));
    }

    @Test
    @DisplayName("Caso na fila: aberto, sem atendente, com o motivo e o consentimento")
    void casoNaFila() {
        ConversaDoAtendimento conversa = atendimento.conversaDoTicket(1001L);

        assertEquals(StatusTicket.ABERTO, conversa.status());
        assertEquals("BAIXA_CONFIANCA", conversa.motivo());
        assertEquals("AGUARDANDO_ATENDENTE", conversa.estadoConversa());
        assertNull(conversa.atendente());
        assertNull(conversa.assumidoEm());
        assertNotNull(conversa.consentimentoEm());
        assertTrue(conversa.transferencias().isEmpty());
    }

    @Test
    @DisplayName("Depois de assumido, a conversa mostra quem está atendendo")
    void casoAssumido() {
        fila.assumir(1002L, 1001L);
        em.flush();
        em.clear();

        ConversaDoAtendimento conversa = atendimento.conversaDoTicket(1002L);

        assertEquals(StatusTicket.EM_ATENDIMENTO, conversa.status());
        assertEquals("COM_ATENDENTE", conversa.estadoConversa());
        assertEquals("Bruno Dias", conversa.atendente());
        assertNotNull(conversa.assumidoEm());
    }

    @Test
    @DisplayName("Ticket inexistente é recusado")
    void ticketInexistente() {
        assertThrows(NaoEncontradoException.class, () -> atendimento.conversaDoTicket(99L));
    }

    @Test
    @DisplayName("A equipe vem do banco, em ordem alfabética")
    void equipe() {
        List<AtendenteResumo> equipe = atendimento.atendentes();

        assertEquals(List.of("Ana Lima", "Bruno Dias"), equipe.stream().map(AtendenteResumo::nome).toList());
        assertTrue(equipe.stream().allMatch(AtendenteResumo::disponivel));
        assertEquals("GESTOR", equipe.get(0).perfil());
    }

    @Test
    @DisplayName("Caso transferido mostra o atendente atual e o caminho, com o motivo")
    void casoTransferido() {
        ConversaDoAtendimento conversa = atendimento.conversaDoTicket(1004L);

        assertEquals("Bruno Dias", conversa.atendente());
        assertEquals(1, conversa.transferencias().size());
        TransferenciaDoHistorico transferencia = conversa.transferencias().get(0);
        assertEquals("Ana Lima", transferencia.de());
        assertEquals("Bruno Dias", transferencia.para());
        assertEquals("Ana Lima", transferencia.realizadaPor());
        assertTrue(transferencia.motivo().startsWith("Segunda via de boleto"));
        assertEquals(List.of("CLIENTE", "CHATBOT", "ATENDENTE", "ATENDENTE"),
                conversa.mensagens().stream().map(MensagemDoHistorico::autor).toList());
        assertEquals("Ana Lima", conversa.mensagens().get(2).identificacao());
    }

    @Test
    @DisplayName("Transferir pelo serviço grava ticket e registro juntos, e a Tela 7 já mostra")
    void transferirPeloServico() {
        fila.transferir(1004L, 1001L, 1002L, "Cliente quer negociar desconto, decisão da gestão");
        em.flush();
        em.clear();

        ConversaDoAtendimento conversa = atendimento.conversaDoTicket(1004L);

        assertEquals(StatusTicket.EM_ATENDIMENTO, conversa.status());
        assertEquals("COM_ATENDENTE", conversa.estadoConversa());
        assertEquals("Ana Lima", conversa.atendente());
        assertEquals(List.of("Ana Lima", "Bruno Dias"),
                conversa.transferencias().stream().map(TransferenciaDoHistorico::de).toList());
    }

    @Test
    @DisplayName("Transferência recusada não grava nada: o caso fica com quem estava")
    void transferenciaRecusada() {
        fila.assumir(1001L, 1001L);
        em.flush();
        em.clear();

        assertThrows(IllegalStateException.class, () -> fila.transferir(1001L, 1001L, 1001L, "Encaminhar"));
        em.clear();

        ConversaDoAtendimento conversa = atendimento.conversaDoTicket(1001L);
        assertEquals("Bruno Dias", conversa.atendente());
        assertTrue(conversa.transferencias().isEmpty());
    }
}
