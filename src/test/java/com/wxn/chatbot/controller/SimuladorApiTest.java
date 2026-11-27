package com.wxn.chatbot.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.wxn.chatbot.dto.ConversaDoCliente;
import com.wxn.chatbot.dto.MensagemDoHistorico;
import com.wxn.chatbot.servico.CanalDoClienteService;
import com.wxn.chatbot.servico.NaoEncontradoException;

/** A API do simulador vista pelo HTTP. */
@WebMvcTest(controllers = SimuladorController.class)
class SimuladorApiTest {

    @Autowired
    private MockMvc http;

    @MockitoBean
    private CanalDoClienteService canal;

    @Test
    @DisplayName("POST da mensagem devolve a conversa com a resposta do Tino")
    void enviar() throws Exception {
        LocalDateTime agora = LocalDateTime.of(2026, 11, 17, 9, 0);
        given(canal.receberMensagem("5581955550001", "Fernanda", "oi")).willReturn(new ConversaDoCliente(
                7L, "AGUARDANDO_CONSENTIMENTO", null, null, List.of(
                        new MensagemDoHistorico("CLIENTE", "*********0001", "oi", agora),
                        new MensagemDoHistorico("CHATBOT", "Tino", "Olá!", agora)),
                List.of("Sim", "Não")));

        http.perform(post("/api/simulador/mensagens").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"5581955550001\",\"nome\":\"Fernanda\",\"texto\":\"oi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("AGUARDANDO_CONSENTIMENTO"))
                .andExpect(jsonPath("$.mensagens[1].autor").value("CHATBOT"))
                .andExpect(jsonPath("$.mensagens[1].conteudo").value("Olá!"))
                .andExpect(jsonPath("$.opcoes[0]").value("Sim"));
    }

    @Test
    @DisplayName("Mensagem inválida responde 400 no formato do painel")
    void invalida() throws Exception {
        given(canal.receberMensagem(any(), any(), any())).willThrow(new IllegalArgumentException("A mensagem está vazia."));

        http.perform(post("/api/simulador/mensagens").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"5581955550001\",\"texto\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("A mensagem está vazia."));
    }

    @Test
    @DisplayName("Telefone sem conversa responde 404, e telefone ausente responde 400")
    void conversa() throws Exception {
        given(canal.conversaAtual("5581955550009"))
                .willThrow(new NaoEncontradoException("Nenhuma conversa com este telefone."));

        http.perform(get("/api/simulador/conversa").param("telefone", "5581955550009"))
                .andExpect(status().isNotFound());
        http.perform(get("/api/simulador/conversa"))
                .andExpect(status().isBadRequest());
    }
}
