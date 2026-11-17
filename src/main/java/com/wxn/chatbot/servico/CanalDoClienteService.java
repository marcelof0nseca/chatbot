package com.wxn.chatbot.servico;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wxn.chatbot.dominio.Atendente;
import com.wxn.chatbot.dominio.Chatbot;
import com.wxn.chatbot.dominio.Cliente;
import com.wxn.chatbot.dominio.Conversa;
import com.wxn.chatbot.dominio.Ticket;
import com.wxn.chatbot.dominio.TipoCliente;
import com.wxn.chatbot.dto.ConversaDoCliente;

/**
 * Porta de entrada das mensagens do cliente (Epico 1).
 *
 * Quem chama nao importa: hoje e o simulador do painel; com a integracao do
 * WhatsApp, o webhook da Meta chama o mesmo receberMensagem(). O servico acha
 * o cliente pelo telefone, retoma a conversa em andamento e deixa o Tino
 * decidir: pedir o aceite da LGPD, responder pela base ou escalar. Quando a
 * conversa ja esta com um humano, o bot fica quieto e a fala so e registrada,
 * para o atendente ver na Tela 7.
 */
@Service
public class CanalDoClienteService {

    private final ConversaComClienteRepositorio repositorio;
    private final Chatbot tino;

    public CanalDoClienteService(ConversaComClienteRepositorio repositorio, Chatbot tino) {
        if (repositorio == null) {
            throw new IllegalArgumentException("O repositório é obrigatório.");
        }
        if (tino == null) {
            throw new IllegalArgumentException("O chatbot é obrigatório.");
        }
        this.repositorio = repositorio;
        this.tino = tino;
    }

    /**
     * Uma mensagem do cliente e a resposta do Tino, se houver. Tudo na mesma
     * transacao: cliente, conversa, mensagens e o ticket de um escalonamento.
     */
    @Transactional
    public ConversaDoCliente receberMensagem(String telefone, String nome, String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("A mensagem está vazia.");
        }
        String digitos = soDigitos(telefone);

        Cliente cliente = repositorio.buscarClientePorTelefone(digitos)
                .orElseGet(() -> cadastrar(digitos, nome));
        Conversa conversa = repositorio.buscarConversaEmAndamento(cliente)
                .orElseGet(() -> new Conversa(null, cliente));

        String fala = texto.trim();
        if (conversa.botPodeResponder()) {
            tino.atender(conversa, fala);
        } else {
            conversa.registrarMensagem(cliente, fala);
        }

        // O consentimento mora no cliente: o aceite precisa ser gravado tambem.
        repositorio.salvarCliente(cliente);
        repositorio.salvarConversa(conversa);
        return paraCliente(conversa);
    }

    /** A conversa em andamento de um telefone, para o simulador retomar ou atualizar. */
    @Transactional(readOnly = true)
    public ConversaDoCliente conversaAtual(String telefone) {
        String digitos = soDigitos(telefone);
        Cliente cliente = repositorio.buscarClientePorTelefone(digitos)
                .orElseThrow(() -> new NaoEncontradoException("Nenhuma conversa com este telefone."));
        Conversa conversa = repositorio.buscarConversaEmAndamento(cliente)
                .orElseThrow(() -> new NaoEncontradoException("Nenhuma conversa em andamento com este telefone."));
        return paraCliente(conversa);
    }

    /**
     * Primeiro contato: o cliente e gravado antes da primeira fala, porque a
     * mensagem guarda o id do autor no momento em que e criada.
     */
    private Cliente cadastrar(String digitos, String nome) {
        String nomeInformado = (nome == null || nome.isBlank())
                ? "Cliente " + digitos.substring(Math.max(0, digitos.length() - 4))
                : nome.trim();
        Cliente novo = new Cliente(null, nomeInformado, digitos, TipoCliente.EXTERNO);
        repositorio.salvarCliente(novo);
        return novo;
    }

    private static String soDigitos(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            throw new IllegalArgumentException("O telefone é obrigatório.");
        }
        return telefone.replaceAll("\\D", "");
    }

    private static ConversaDoCliente paraCliente(Conversa conversa) {
        return new ConversaDoCliente(
                conversa.getId(),
                conversa.getEstado().name(),
                conversa.getTicket().map(Ticket::getId).orElse(null),
                conversa.getTicket().flatMap(Ticket::getAtendente).map(Atendente::getNome).orElse(null),
                conversa.getMensagens().stream().map(AtendimentoService::paraHistorico).toList());
    }
}
