package com.wxn.chatbot.repositorio;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.wxn.chatbot.dominio.Cliente;
import com.wxn.chatbot.dominio.Conversa;
import com.wxn.chatbot.dominio.EstadoConversa;
import com.wxn.chatbot.servico.ConversaComClienteRepositorio;

/** Implementacao da porta ConversaComClienteRepositorio sobre o Spring Data. */
@Repository
public class ConversaComClienteRepositorioJpa implements ConversaComClienteRepositorio {

    private final ClienteRepository clientes;
    private final ConversaRepository conversas;

    public ConversaComClienteRepositorioJpa(ClienteRepository clientes, ConversaRepository conversas) {
        this.clientes = clientes;
        this.conversas = conversas;
    }

    @Override
    public Optional<Cliente> buscarClientePorTelefone(String telefone) {
        return clientes.findByTelefone(telefone);
    }

    @Override
    public Optional<Conversa> buscarConversaEmAndamento(Cliente cliente) {
        return conversas.findFirstByClienteAndEstadoNotOrderByIniciadaEmDescIdDesc(cliente, EstadoConversa.ENCERRADA);
    }

    @Override
    public void salvarCliente(Cliente cliente) {
        clientes.save(cliente);
    }

    /**
     * Conversa nova: persist, com mensagens e ticket em cascata. Conversa que
     * ja veio do banco: ela e gerenciada pelo JPA, entao basta descarregar as
     * mudancas; o flush grava as mensagens novas e o ticket de um escalonamento
     * e ja devolve o id dele. (Um save aqui faria merge, que copiaria o ticket
     * novo em vez de grava-lo.)
     */
    @Override
    public void salvarConversa(Conversa conversa) {
        if (conversa.getId() == null) {
            conversas.save(conversa);
        }
        conversas.flush();
    }
}
