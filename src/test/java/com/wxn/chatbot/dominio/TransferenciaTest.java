package com.wxn.chatbot.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Regras da transferencia de um caso entre atendentes (Epico 4, segunda historia). */
class TransferenciaTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 9, 9, 0);

    private Atendente bruno;
    private Atendente carla;
    private Atendente ana;
    private Conversa conversa;
    private Ticket ticket;

    @BeforeEach
    void preparar() {
        mudarRelogioPara(INICIO);
        bruno = new Atendente(1L, "Bruno Dias", "bruno@wxn.com.br", PerfilAtendente.ATENDENTE);
        carla = new Atendente(3L, "Carla Melo", "carla@wxn.com.br", PerfilAtendente.ATENDENTE);
        ana = new Atendente(2L, "Ana Lima", "ana@wxn.com.br", PerfilAtendente.GESTOR);
        bruno.entrarEmServico();
        carla.entrarEmServico();
        ana.entrarEmServico();

        Cliente cliente = new Cliente(1L, "Maria Souza", "+55 81 99999-4821", TipoCliente.EXTERNO);
        cliente.registrarConsentimento("v1");
        conversa = new Conversa(10L, cliente);
        ticket = conversa.escalonar(MotivoEscalonamento.BAIXA_CONFIANCA);

        mudarRelogioPara(INICIO.plusMinutes(5));
        ticket.assumir(bruno);
    }

    @AfterEach
    void restaurar() {
        Relogio.restaurarPadrao();
    }

    @Test
    @DisplayName("O atendente do caso transfere: o caso muda de mãos e continua em atendimento")
    void atendenteTransfere() {
        mudarRelogioPara(INICIO.plusMinutes(20));

        Transferencia transferencia = ticket.transferirPara(bruno, carla, "Cliente pediu segunda via do boleto");

        assertSame(carla, ticket.getAtendente().orElseThrow());
        assertEquals(StatusTicket.EM_ATENDIMENTO, ticket.getStatus());
        assertEquals(EstadoConversa.COM_ATENDENTE, conversa.getEstado());
        assertFalse(conversa.botPodeResponder());

        assertSame(ticket, transferencia.getTicket());
        assertSame(bruno, transferencia.getOrigem());
        assertSame(carla, transferencia.getDestino());
        assertSame(bruno, transferencia.getRealizadaPor());
        assertEquals("Cliente pediu segunda via do boleto", transferencia.getMotivo());
        assertEquals(INICIO.plusMinutes(20), transferencia.getRealizadaEm());
    }

    @Test
    @DisplayName("Transferir não zera o tempo de espera do cliente")
    void preservaAssumidoEm() {
        mudarRelogioPara(INICIO.plusMinutes(20));
        ticket.transferirPara(bruno, carla, "Assunto financeiro");

        assertEquals(INICIO.plusMinutes(5), ticket.getAssumidoEm());
        assertEquals(5, ticket.tempoDeEspera().toMinutes());
    }

    @Test
    @DisplayName("Um gestor transfere um caso que não é dele, e fica registrado como autor")
    void gestorTransfere() {
        Transferencia transferencia = ticket.transferirPara(ana, carla, "Redistribuição da equipe");

        assertSame(carla, ticket.getAtendente().orElseThrow());
        assertSame(bruno, transferencia.getOrigem());
        assertSame(ana, transferencia.getRealizadaPor());
    }

    @Test
    @DisplayName("Atendente comum não transfere caso de outro atendente")
    void atendenteDeForaNaoTransfere() {
        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> ticket.transferirPara(carla, carla, "Quero esse caso"));

        assertEquals("Só o atendente do caso ou um gestor pode transferi-lo.", erro.getMessage());
        assertSame(bruno, ticket.getAtendente().orElseThrow());
    }

    @Test
    @DisplayName("Ticket que ainda está na fila não é transferido: deve ser assumido")
    void ticketNaFila() {
        Cliente outro = new Cliente(2L, "João Alves", "+55 81 98888-1234", TipoCliente.EXTERNO);
        outro.registrarConsentimento("v1");
        Ticket naFila = new Conversa(11L, outro).escalonar(MotivoEscalonamento.SOLICITACAO_CLIENTE);

        assertThrows(IllegalStateException.class, () -> naFila.transferirPara(ana, carla, "Encaminhar"));
    }

    @Test
    @DisplayName("Ticket encerrado não é transferido")
    void ticketEncerrado() {
        ticket.resolver();

        assertThrows(IllegalStateException.class, () -> ticket.transferirPara(bruno, carla, "Encaminhar"));
    }

    @Test
    @DisplayName("Não se transfere para quem já está com o caso")
    void destinoIgualAoAtual() {
        assertThrows(IllegalStateException.class, () -> ticket.transferirPara(ana, bruno, "Encaminhar"));
    }

    @Test
    @DisplayName("Destino fora de serviço é recusado e o caso fica onde estava")
    void destinoForaDeServico() {
        carla.sairDeServico();

        assertThrows(IllegalStateException.class, () -> ticket.transferirPara(bruno, carla, "Encaminhar"));
        assertSame(bruno, ticket.getAtendente().orElseThrow());
    }

    @Test
    @DisplayName("Motivo é obrigatório, sem espaços nas pontas e com até 280 caracteres")
    void motivo() {
        assertThrows(IllegalArgumentException.class, () -> ticket.transferirPara(bruno, carla, null));
        assertThrows(IllegalArgumentException.class, () -> ticket.transferirPara(bruno, carla, "   "));
        assertThrows(IllegalArgumentException.class,
                () -> ticket.transferirPara(bruno, carla, "x".repeat(281)));

        Transferencia transferencia = ticket.transferirPara(bruno, carla, "  " + "x".repeat(280) + "  ");
        assertEquals(280, transferencia.getMotivo().length());
    }

    @Test
    @DisplayName("Destino e autor são obrigatórios")
    void obrigatorios() {
        assertThrows(IllegalArgumentException.class, () -> ticket.transferirPara(bruno, null, "Encaminhar"));
        assertThrows(IllegalArgumentException.class, () -> ticket.transferirPara(null, carla, "Encaminhar"));
    }

    @Test
    @DisplayName("O caso pode ser transferido mais de uma vez; a origem é sempre quem estava com ele")
    void transferenciasEmSequencia() {
        ticket.transferirPara(bruno, carla, "Assunto financeiro");
        Transferencia volta = ticket.transferirPara(carla, bruno, "Cliente voltou a falar de entrega");

        assertSame(carla, volta.getOrigem());
        assertSame(bruno, ticket.getAtendente().orElseThrow());
    }

    private static void mudarRelogioPara(LocalDateTime horario) {
        Relogio.usar(Clock.fixed(horario.atZone(Relogio.ZONA_PADRAO).toInstant(), Relogio.ZONA_PADRAO));
    }
}
