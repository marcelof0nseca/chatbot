package com.wxn.chatbot.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.wxn.chatbot.dominio.Atendente;
import com.wxn.chatbot.dominio.StatusTicket;
import com.wxn.chatbot.dominio.Ticket;
import com.wxn.chatbot.dominio.Transferencia;
import com.wxn.chatbot.servico.FilaRepositorio;

import jakarta.persistence.EntityManager;

/** O registro das transferencias no banco, com a carga do data.sql. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:transferencia-teste;DB_CLOSE_DELAY=-1;TIME ZONE=America/Recife",
        "spring.jpa.show-sql=false"
})
@Transactional
class TransferenciaPersistenciaTest {

    @Autowired
    private FilaRepositorio repositorio;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("A transferência da carga volta do banco com origem, destino, autor e motivo")
    void transferenciaDaCarga() {
        List<Transferencia> caminho = repositorio.listarTransferencias(1004L);

        assertEquals(1, caminho.size());
        Transferencia transferencia = caminho.get(0);
        assertEquals("Ana Lima", transferencia.getOrigem().getNome());
        assertEquals("Bruno Dias", transferencia.getDestino().getNome());
        assertEquals("Ana Lima", transferencia.getRealizadaPor().getNome());
        assertTrue(transferencia.getMotivo().startsWith("Segunda via de boleto"));
        assertNotNull(transferencia.getRealizadaEm());
    }

    @Test
    @DisplayName("O ticket transferido na carga está com o destino da última transferência")
    void ticketComODestino() {
        Ticket ticket = repositorio.buscarTicket(1004L).orElseThrow();

        assertEquals(StatusTicket.EM_ATENDIMENTO, ticket.getStatus());
        assertEquals("Bruno Dias", ticket.getAtendente().orElseThrow().getNome());
    }

    @Test
    @DisplayName("Uma transferência nova é gravada e entra no fim do caminho do caso")
    void gravaNoFimDoCaminho() {
        Ticket ticket = repositorio.buscarTicket(1004L).orElseThrow();
        Atendente bruno = repositorio.buscarAtendente(1001L).orElseThrow();
        Atendente ana = repositorio.buscarAtendente(1002L).orElseThrow();

        Transferencia volta = ticket.transferirPara(bruno, ana, "Cliente quer negociar desconto, decisão da gestão");
        repositorio.salvar(ticket);
        repositorio.salvarTransferencia(volta);
        em.flush();
        em.clear();

        List<Transferencia> caminho = repositorio.listarTransferencias(1004L);
        assertEquals(List.of("Ana Lima", "Bruno Dias"),
                caminho.stream().map(t -> t.getOrigem().getNome()).toList());
        assertNotNull(caminho.get(1).getId());
        assertEquals("Ana Lima", em.find(Ticket.class, 1004L).getAtendente().orElseThrow().getNome());
    }

    @Test
    @DisplayName("Caso que nunca foi transferido tem caminho vazio")
    void semTransferencia() {
        assertFalse(repositorio.buscarTicket(1001L).isEmpty());
        assertTrue(repositorio.listarTransferencias(1001L).isEmpty());
    }
}
