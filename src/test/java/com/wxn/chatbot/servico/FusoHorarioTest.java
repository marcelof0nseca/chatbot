package com.wxn.chatbot.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.wxn.chatbot.dominio.Atendente;
import com.wxn.chatbot.dominio.Cliente;
import com.wxn.chatbot.dominio.Conversa;
import com.wxn.chatbot.dominio.MotivoEscalonamento;
import com.wxn.chatbot.dominio.PerfilAtendente;
import com.wxn.chatbot.dominio.Relogio;
import com.wxn.chatbot.dominio.Ticket;
import com.wxn.chatbot.dominio.TipoCliente;
import com.wxn.chatbot.dto.ResumoDaFila;
import com.wxn.chatbot.repositorio.FilaRepositorioEmMemoria;

/**
 * O fuso fica fixo em Recife, qualquer que seja o fuso do servidor.
 *
 * Os testes usam relogio fixo, entao nao dependem da hora em que rodam, e
 * simulam o servidor em nuvem trocando o fuso padrao da JVM para UTC.
 */
class FusoHorarioTest {

    private TimeZone fusoOriginal;
    private FilaRepositorioEmMemoria repositorio;
    private long proximoId;

    @BeforeEach
    void simularServidorEmUtc() {
        fusoOriginal = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        repositorio = new FilaRepositorioEmMemoria();
        proximoId = 1L;
    }

    @AfterEach
    void restaurar() {
        Relogio.restaurarPadrao();
        TimeZone.setDefault(fusoOriginal);
    }

    /** Relogio parado num horario de Recife, como "2026-11-02T22:00". */
    private static Clock em(String horarioDeRecife) {
        return Clock.fixed(LocalDateTime.parse(horarioDeRecife).atZone(Relogio.ZONA_PADRAO).toInstant(),
                Relogio.ZONA_PADRAO);
    }

    private Conversa conversaIniciadaEm(String horarioDeRecife) {
        Relogio.usar(em(horarioDeRecife));
        long id = proximoId++;
        Cliente cliente = new Cliente(id, "Cliente " + id, "55819999900" + String.format("%02d", id),
                TipoCliente.EXTERNO);
        cliente.registrarConsentimento("v1");
        Conversa conversa = new Conversa(id, cliente);
        conversa.registrarMensagem(cliente, "oi");
        return conversa;
    }

    private void resolvidaPeloBotEm(String horarioDeRecife) {
        Conversa conversa = conversaIniciadaEm(horarioDeRecife);
        conversa.encerrar();
        repositorio.registrar(conversa);
    }

    private Ticket escalonadaEm(String horarioDeRecife) {
        Conversa conversa = conversaIniciadaEm(horarioDeRecife);
        Ticket ticket = conversa.escalonar(conversa.getId(), MotivoEscalonamento.BAIXA_CONFIANCA);
        repositorio.registrar(conversa);
        return ticket;
    }

    private FilaDeConversasService servicoAs(String horarioDeRecife) {
        Clock relogio = em(horarioDeRecife);
        Relogio.usar(relogio);
        return new FilaDeConversasService(repositorio, relogio);
    }

    @Test
    @DisplayName("Às 22h de Recife, num servidor em UTC, o resumo ainda conta o dia de Recife")
    void resumoNaoViraAs21h() {
        resolvidaPeloBotEm("2026-11-02T10:00");
        escalonadaEm("2026-11-02T21:30");

        ResumoDaFila resumo = servicoAs("2026-11-02T22:00").resumoDoDia();

        // Para o servidor ja seria dia 3: e onde o codigo sem zona errava.
        assertEquals(LocalDate.of(2026, 11, 3),
                LocalDate.now(em("2026-11-02T22:00").withZone(ZoneOffset.UTC)));
        assertEquals(2, resumo.total());
        assertEquals(1, resumo.resolvidasPeloBot());
        assertEquals(0.5, resumo.taxaContencao(), 0.0001);
    }

    @Test
    @DisplayName("A meia-noite que separa os dias é a de Recife")
    void viradaDoDiaEmRecife() {
        resolvidaPeloBotEm("2026-11-02T23:50");
        resolvidaPeloBotEm("2026-11-03T00:10");

        ResumoDaFila resumo = servicoAs("2026-11-03T00:30").resumoDoDia();

        assertEquals(1, resumo.total());
    }

    @Test
    @DisplayName("Os carimbos de tempo do domínio vêm do relógio, no horário de Recife")
    void carimbosDoDominio() {
        Ticket ticket = escalonadaEm("2026-11-02T14:00");
        assertEquals(LocalDateTime.parse("2026-11-02T14:00"), ticket.getAbertoEm());

        Relogio.usar(em("2026-11-02T14:10"));
        assertEquals(Duration.ofMinutes(10), ticket.tempoDeEspera());

        Atendente atendente = new Atendente(1L, "Bruno Dias", "bruno@wxn.com.br", PerfilAtendente.ATENDENTE);
        atendente.entrarEmServico();
        ticket.assumir(atendente);
        assertEquals(LocalDateTime.parse("2026-11-02T14:10"), ticket.getAssumidoEm());

        Relogio.usar(em("2026-11-02T15:00"));
        assertEquals(Duration.ofMinutes(10), ticket.tempoDeEspera(), "a espera para quando o caso é assumido");
        assertEquals(Duration.ofMinutes(50), ticket.tempoDeAtendimento());
    }

    @Test
    @DisplayName("A espera média do resumo usa o mesmo relógio")
    void esperaMedia() {
        escalonadaEm("2026-11-02T14:00");
        escalonadaEm("2026-11-02T14:10");

        ResumoDaFila resumo = servicoAs("2026-11-02T14:20").resumoDoDia();

        assertEquals(2, resumo.naFila());
        assertEquals(15 * 60, resumo.esperaMediaEmSegundos());
    }
}
