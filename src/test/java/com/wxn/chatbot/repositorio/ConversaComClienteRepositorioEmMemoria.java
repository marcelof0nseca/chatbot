package com.wxn.chatbot.repositorio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.wxn.chatbot.dominio.Cliente;
import com.wxn.chatbot.dominio.Conversa;
import com.wxn.chatbot.dominio.EstadoConversa;
import com.wxn.chatbot.servico.ConversaComClienteRepositorio;

/** Duble de teste da porta ConversaComClienteRepositorio: tudo em memoria. */
public class ConversaComClienteRepositorioEmMemoria implements ConversaComClienteRepositorio {

    private final Map<String, Cliente> clientes = new LinkedHashMap<>();
    private final List<Conversa> conversas = new ArrayList<>();

    @Override
    public Optional<Cliente> buscarClientePorTelefone(String telefone) {
        return Optional.ofNullable(clientes.get(telefone));
    }

    @Override
    public Optional<Conversa> buscarConversaEmAndamento(Cliente cliente) {
        for (int i = conversas.size() - 1; i >= 0; i--) {
            Conversa conversa = conversas.get(i);
            if (conversa.getCliente() == cliente && conversa.getEstado() != EstadoConversa.ENCERRADA) {
                return Optional.of(conversa);
            }
        }
        return Optional.empty();
    }

    @Override
    public void salvarCliente(Cliente cliente) {
        clientes.put(cliente.getTelefone(), cliente);
    }

    @Override
    public void salvarConversa(Conversa conversa) {
        if (!conversas.contains(conversa)) {
            conversas.add(conversa);
        }
    }

    public int totalDeClientes() {
        return clientes.size();
    }

    public List<Conversa> conversas() {
        return List.copyOf(conversas);
    }
}
