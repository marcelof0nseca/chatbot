package com.wxn.chatbot.servico;

import java.util.Optional;

import com.wxn.chatbot.dominio.Cliente;
import com.wxn.chatbot.dominio.Conversa;

/**
 * Porta de acesso a dados do lado do cliente: achar quem escreve pelo telefone
 * e a conversa que ainda esta em andamento. Em producao, a implementacao e o
 * ConversaComClienteRepositorioJpa; nos testes, um duble em memoria.
 */
public interface ConversaComClienteRepositorio {

    /** O telefone chega so com digitos, como o Cliente o guarda. */
    Optional<Cliente> buscarClientePorTelefone(String telefone);

    /** A conversa mais recente do cliente que ainda nao foi encerrada. */
    Optional<Conversa> buscarConversaEmAndamento(Cliente cliente);

    void salvarCliente(Cliente cliente);

    /** Grava a conversa com as mensagens e o ticket, se o bot escalou. */
    void salvarConversa(Conversa conversa);
}
