package com.wxn.chatbot.controller;

import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.wxn.chatbot.dominio.StatusTicket;
import com.wxn.chatbot.dto.AtendenteResumo;
import com.wxn.chatbot.dto.ConversaDoAtendimento;
import com.wxn.chatbot.dto.MensagemDoHistorico;
import com.wxn.chatbot.servico.AtendimentoService;
import com.wxn.chatbot.servico.FilaDeConversasService;
import com.wxn.chatbot.servico.NaoEncontradoException;

/**
 * A API vista de fora, pelo HTTP: o JSON que o painel recebe e os codigos de
 * erro. Os servicos sao substituidos por mocks; a regra de negocio e testada
 * nos testes de servico e de persistencia.
 */
@WebMvcTest(controllers = { FilaController.class, AtendimentoController.class })
class ApiDoPainelTest {

    private static final LocalDateTime ABERTO_EM = LocalDateTime.of(2026, 11, 9, 9, 15);

    @Autowired
    private MockMvc http;

    @MockitoBean
    private FilaDeConversasService fila;

    @MockitoBean
    private AtendimentoService atendimento;

    private static ConversaDoAtendimento conversaDeExemplo() {
        return new ConversaDoAtendimento(1001L, 1001L, "*********4821", "EXTERNO", "AGUARDANDO_ATENDENTE",
                StatusTicket.ABERTO, "BAIXA_CONFIANCA", null, ABERTO_EM, null, ABERTO_EM.minusMinutes(2),
                List.of(new MensagemDoHistorico("CLIENTE", "*********4821", "oi, tenho uma dúvida",
                        ABERTO_EM.minusMinutes(2))),
                List.of());
    }

    @Test
    @DisplayName("GET da conversa entrega o JSON que a Tela 7 lê")
    void conversaEmJson() throws Exception {
        given(atendimento.conversaDoTicket(1001L)).willReturn(conversaDeExemplo());

        http.perform(get("/api/fila/1001/conversa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contato").value("*********4821"))
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.abertoEm").value("2026-11-09T09:15:00"))
                .andExpect(jsonPath("$.mensagens[0].autor").value("CLIENTE"))
                .andExpect(jsonPath("$.mensagens[0].conteudo").value("oi, tenho uma dúvida"));
    }

    @Test
    @DisplayName("O telefone completo não sai na resposta")
    void semTelefoneCompleto() throws Exception {
        given(atendimento.conversaDoTicket(1001L)).willReturn(conversaDeExemplo());

        http.perform(get("/api/fila/1001/conversa"))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("5581999994821"))));
    }

    @Test
    @DisplayName("Ticket que não existe responde 404")
    void ticketInexistente() throws Exception {
        given(atendimento.conversaDoTicket(99L)).willThrow(new NaoEncontradoException("Ticket não encontrado: 99"));

        http.perform(get("/api/fila/99/conversa"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Ticket não encontrado: 99"));
    }

    @Test
    @DisplayName("Dado inválido responde 400, e não 404")
    void validacaoRespondeBadRequest() throws Exception {
        given(fila.assumir(anyLong(), anyLong())).willThrow(new IllegalArgumentException("Atendente é obrigatório."));

        http.perform(post("/api/fila/1001/assumir").param("atendenteId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Atendente é obrigatório."));
    }

    @Test
    @DisplayName("Regra recusada responde 409")
    void regraRecusada() throws Exception {
        given(fila.assumir(anyLong(), anyLong()))
                .willThrow(new IllegalStateException("Apenas um ticket aberto pode ser assumido."));

        http.perform(post("/api/fila/1001/assumir").param("atendenteId", "1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Apenas um ticket aberto pode ser assumido."));
    }

    @Test
    @DisplayName("Atendente inexistente ao assumir responde 404")
    void atendenteInexistente() throws Exception {
        given(fila.assumir(anyLong(), anyLong())).willThrow(new NaoEncontradoException("Atendente não encontrado: 9"));

        http.perform(post("/api/fila/1001/assumir").param("atendenteId", "9"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Parâmetro ausente ou id que não é número respondem 400 no formato do painel")
    void errosDoProprioSpring() throws Exception {
        http.perform(post("/api/fila/1001/assumir"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Parâmetro obrigatório ausente: atendenteId."));

        http.perform(get("/api/fila/abc/conversa"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Valor inválido para ticketId: abc."));
    }

    @Test
    @DisplayName("GET da equipe entrega nome, perfil e disponibilidade")
    void equipe() throws Exception {
        given(atendimento.atendentes()).willReturn(List.of(
                new AtendenteResumo(1002L, "Ana Lima", "GESTOR", true),
                new AtendenteResumo(1001L, "Bruno Dias", "ATENDENTE", false)));

        http.perform(get("/api/atendentes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Ana Lima"))
                .andExpect(jsonPath("$[0].perfil").value("GESTOR"))
                .andExpect(jsonPath("$[1].disponivel").value(false));
    }
}
