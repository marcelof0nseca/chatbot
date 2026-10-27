package com.nxn.chatbot.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.nxn.chatbot.dominio.Cliente;
import com.nxn.chatbot.dominio.Conversa;
import com.nxn.chatbot.dominio.EstadoConversa;
import com.nxn.chatbot.dominio.Mensagem;
import com.nxn.chatbot.dominio.StatusTicket;
import com.nxn.chatbot.dominio.Ticket;
import com.nxn.chatbot.dominio.TipoCliente;
import com.nxn.chatbot.dominio.TipoRemetente;
import com.nxn.chatbot.dto.ItemDaFila;
import com.nxn.chatbot.servico.FilaDeConversasService;
import com.nxn.chatbot.servico.FilaRepositorio;

import jakarta.persistence.EntityManager;

/**
 * A fila de ponta a ponta contra um banco de verdade: mapeamento JPA,
 * repositorios, adaptador e servico, com a carga do data.sql.
 *
 * Roda num H2 em memoria, para nao tocar o banco de desenvolvimento em
 * ./data. Cada teste e desfeito ao final (rollback).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fila-teste;DB_CLOSE_DELAY=-1",
        "spring.jpa.show-sql=false"
})
@Transactional
class FilaPersistenciaTest {

    @Autowired
    private FilaDeConversasService servico;

    @Autowired
    private FilaRepositorio repositorio;

    @Autowired
    private ConversaRepository conversas;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("Em produção a fila usa o banco, e não a implementação em memória")
    void filaUsaOBanco() {
        assertEquals(FilaRepositorioJpa.class, AopUtils.getTargetClass(repositorio));
    }

    @Test
    @DisplayName("A fila vem do banco, com o caso mais antigo no topo")
    void filaVemDoBanco() {
        List<ItemDaFila> fila = servico.listarFila();

        assertEquals(List.of(1001L, 1002L, 1003L), fila.stream().map(ItemDaFila::ticketId).toList());
        fila.forEach(item -> assertEquals(StatusTicket.ABERTO, item.status()));
    }

    @Test
    @DisplayName("O telefone sai mascarado e a prévia mantém os acentos do data.sql")
    void dadosDaFila() {
        ItemDaFila primeiro = servico.listarFila().get(0);

        assertEquals("*********4821", primeiro.contato());
        assertTrue(primeiro.ultimaMensagem().startsWith("o cupom BLACK30 não aplicou"));
        assertTrue(primeiro.esperaEmSegundos() >= 40 * 60, "aberto há 41 minutos na carga");
    }

    @Test
    @DisplayName("Assumir grava ticket e conversa no banco, na mesma transação")
    void assumirPersiste() {
        servico.assumir(1001L, 1001L);
        em.flush();
        em.clear();

        Ticket ticket = em.find(Ticket.class, 1001L);
        assertEquals(StatusTicket.EM_ATENDIMENTO, ticket.getStatus());
        assertEquals("Bruno Dias", ticket.getAtendente().orElseThrow().getNome());
        assertNotNull(ticket.getAssumidoEm());
        assertEquals(EstadoConversa.COM_ATENDENTE, ticket.getConversa().getEstado());
    }

    @Test
    @DisplayName("Depois de assumido, o caso desce para depois de quem ainda aguarda")
    void assumidoDesceNaFila() {
        servico.assumir(1001L, 1001L);
        em.flush();
        em.clear();

        List<Long> ordem = servico.listarFila().stream().map(ItemDaFila::ticketId).toList();
        assertEquals(List.of(1002L, 1003L, 1001L), ordem);
    }

    @Test
    @DisplayName("O banco não deixa assumir o mesmo caso duas vezes")
    void naoAssumeDuasVezes() {
        servico.assumir(1001L, 1001L);
        em.flush();
        em.clear();

        assertThrows(IllegalStateException.class, () -> servico.assumir(1001L, 1002L));
    }

    @Test
    @DisplayName("Conversa em andamento com o bot não conta como resolvida")
    void contencaoContaSoEncerradas() {
        LocalDateTime ontem = LocalDateTime.now().minusDays(1);
        assertEquals(6, repositorio.contarConversasDesde(ontem));
        assertEquals(3, repositorio.contarConversasResolvidasPeloBotDesde(ontem));

        Cliente cliente = new Cliente(null, "Teste", "5581911112222", TipoCliente.EXTERNO);
        cliente.registrarConsentimento("v1");
        em.persist(cliente);
        em.persist(new Conversa(null, cliente));
        em.flush();

        assertEquals(7, repositorio.contarConversasDesde(ontem));
        assertEquals(3, repositorio.contarConversasResolvidasPeloBotDesde(ontem));
        assertEquals(3, conversas.countByIniciadaEmAfterAndEstadoAndTicketIsNull(ontem, EstadoConversa.ENCERRADA));
    }

    @Test
    @DisplayName("A mensagem carregada do banco recupera o autor concreto")
    void autorDaMensagem() {
        Conversa conversa = em.find(Conversa.class, 1001L);
        Mensagem mensagem = conversa.getMensagens().get(0);

        assertEquals(TipoRemetente.CLIENTE, mensagem.getRemetente());
        assertEquals("*********4821", mensagem.getAutor().getIdentificacao());
    }

    @Test
    @DisplayName("Uma conversa nova, salva com mensagens, volta do banco com o histórico")
    void composicaoSalvaMensagensEmCascata() {
        Cliente cliente = new Cliente(null, "Teste", "5581933334444", TipoCliente.EXTERNO);
        cliente.registrarConsentimento("v1");
        em.persist(cliente);

        Conversa conversa = new Conversa(null, cliente);
        conversa.registrarMensagem(cliente, "primeira");
        conversa.registrarMensagem(cliente, "segunda");
        em.persist(conversa);
        em.flush();
        em.clear();

        Conversa recarregada = em.find(Conversa.class, conversa.getId());
        assertEquals(List.of("primeira", "segunda"),
                recarregada.getMensagens().stream().map(Mensagem::getConteudo).toList());
    }
}
