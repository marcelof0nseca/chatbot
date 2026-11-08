package com.wxn.chatbot.controller;

import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
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
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.wxn.chatbot.dominio.StatusTicket;
import com.wxn.chatbot.dto.AtendenteResumo;
import com.wxn.chatbot.dto.ConversaDoAtendimento;
import com.wxn.chatbot.dto.ItemDaFila;
import com.wxn.chatbot.dto.MensagemDoHistorico;
import com.wxn.chatbot.dto.TransferenciaDoHistorico;
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

    // Transferencia (segunda historia do Epico 4)

    private static final String PEDIDO = """
            {"quemTransfereId": 1002, "destinoId": 1001, "motivo": "Assunto do financeiro"}
            """;

    private ResultActions transferir(String corpo) throws Exception {
        return http.perform(post("/api/fila/1004/transferir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    @Test
    @DisplayName("POST da transferência repassa o pedido ao serviço e devolve o caso com o novo atendente")
    void transferirComSucesso() throws Exception {
        given(fila.transferir(1004L, 1002L, 1001L, "Assunto do financeiro")).willReturn(new ItemDaFila(
                1004L, "*********6650", "ASSUNTO_NAO_SUPORTADO", 180, StatusTicket.EM_ATENDIMENTO,
                "preciso da segunda via do boleto", "Bruno Dias"));

        transferir(PEDIDO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(1004))
                .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"))
                .andExpect(jsonPath("$.atendente").value("Bruno Dias"));
    }

    @Test
    @DisplayName("Transferência recusada pela regra responde 409 com a mensagem do domínio")
    void transferenciaRecusada() throws Exception {
        given(fila.transferir(anyLong(), anyLong(), anyLong(), any()))
                .willThrow(new IllegalStateException("Só o atendente do caso ou um gestor pode transferi-lo."));

        transferir(PEDIDO)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Só o atendente do caso ou um gestor pode transferi-lo."));
    }

    @Test
    @DisplayName("Motivo vazio responde 400")
    void transferenciaSemMotivo() throws Exception {
        given(fila.transferir(anyLong(), anyLong(), anyLong(), any()))
                .willThrow(new IllegalArgumentException("O motivo da transferência é obrigatório."));

        transferir("""
                {"quemTransfereId": 1002, "destinoId": 1001, "motivo": ""}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O motivo da transferência é obrigatório."));
    }

    @Test
    @DisplayName("Destino inexistente responde 404")
    void destinoInexistente() throws Exception {
        given(fila.transferir(anyLong(), anyLong(), anyLong(), any()))
                .willThrow(new NaoEncontradoException("Atendente não encontrado: 9"));

        transferir(PEDIDO).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Campo ausente chega ao serviço como nulo, que o recusa como dado inválido")
    void campoAusente() throws Exception {
        given(fila.transferir(anyLong(), anyLong(), isNull(), any()))
                .willThrow(new IllegalArgumentException("Atendente é obrigatório."));

        transferir("""
                {"quemTransfereId": 1002, "motivo": "Assunto do financeiro"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Atendente é obrigatório."));
    }

    @Test
    @DisplayName("Corpo ausente, nulo ou que não é JSON responde 400 sem chamar o serviço")
    void corpoInvalido() throws Exception {
        http.perform(post("/api/fila/1004/transferir").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O corpo da requisição não é um JSON válido."));
        transferir("null").andExpect(status().isBadRequest());
        transferir("{motivo: sem aspas").andExpect(status().isBadRequest());

        then(fila).should(never()).transferir(any(), any(), any(), any());
    }

    @Test
    @DisplayName("A conversa da Tela 7 traz o caminho do caso entre atendentes")
    void conversaComTransferencias() throws Exception {
        ConversaDoAtendimento base = conversaDeExemplo();
        ConversaDoAtendimento transferida = new ConversaDoAtendimento(1004L, 1007L, base.contato(), "EXTERNO",
                "COM_ATENDENTE", StatusTicket.EM_ATENDIMENTO, "ASSUNTO_NAO_SUPORTADO", "Bruno Dias",
                ABERTO_EM, ABERTO_EM.plusMinutes(3), base.consentimentoEm(), base.mensagens(),
                List.of(new TransferenciaDoHistorico("Ana Lima", "Bruno Dias", "Ana Lima",
                        "Segunda via de boleto é com o financeiro", ABERTO_EM.plusMinutes(9))));
        given(atendimento.conversaDoTicket(1004L)).willReturn(transferida);

        http.perform(get("/api/fila/1004/conversa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atendente").value("Bruno Dias"))
                .andExpect(jsonPath("$.transferencias[0].de").value("Ana Lima"))
                .andExpect(jsonPath("$.transferencias[0].para").value("Bruno Dias"))
                .andExpect(jsonPath("$.transferencias[0].realizadaPor").value("Ana Lima"))
                .andExpect(jsonPath("$.transferencias[0].realizadaEm").value("2026-11-09T09:24:00"));
    }
}
