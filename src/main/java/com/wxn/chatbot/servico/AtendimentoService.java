package com.wxn.chatbot.servico;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wxn.chatbot.dominio.Atendente;
import com.wxn.chatbot.dominio.Cliente;
import com.wxn.chatbot.dominio.Conversa;
import com.wxn.chatbot.dominio.Mensagem;
import com.wxn.chatbot.dominio.Ticket;
import com.wxn.chatbot.dto.AtendenteResumo;
import com.wxn.chatbot.dto.ConversaDoAtendimento;
import com.wxn.chatbot.dto.MensagemDoHistorico;

/**
 * Leitura da conversa de um caso (Tela 7) e da equipe que pode atende-lo.
 *
 * Usa a mesma porta da fila: a conversa e alcancada pelo ticket, que ja e o
 * que a fila entrega ao painel.
 */
@Service
public class AtendimentoService {

    /** Nome com que o bot aparece no historico. */
    static final String NOME_DO_BOT = "Tino";

    private final FilaRepositorio repositorio;

    public AtendimentoService(FilaRepositorio repositorio) {
        if (repositorio == null) {
            throw new IllegalArgumentException("O repositório é obrigatório.");
        }
        this.repositorio = repositorio;
    }

    /** Transacional porque as mensagens sao carregadas sob demanda pelo JPA. */
    @Transactional(readOnly = true)
    public ConversaDoAtendimento conversaDoTicket(Long ticketId) {
        Ticket ticket = repositorio.buscarTicket(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket não encontrado: " + ticketId));
        Conversa conversa = ticket.getConversa();
        Cliente cliente = conversa.getCliente();

        List<MensagemDoHistorico> historico = conversa.getMensagens().stream()
                .map(AtendimentoService::paraHistorico)
                .toList();

        return new ConversaDoAtendimento(
                ticket.getId(),
                conversa.getId(),
                cliente.getTelefoneMascarado(),
                cliente.getTipo().name(),
                conversa.getEstado().name(),
                ticket.getStatus(),
                ticket.getMotivo().name(),
                ticket.getAtendente().map(Atendente::getNome).orElse(null),
                ticket.getAbertoEm(),
                ticket.getAssumidoEm(),
                cliente.getConsentimento() == null ? null : cliente.getConsentimento().getData(),
                historico);
    }

    @Transactional(readOnly = true)
    public List<AtendenteResumo> atendentes() {
        return repositorio.listarAtendentes().stream()
                .map(a -> new AtendenteResumo(a.getId(), a.getNome(), a.getPerfil().name(), a.estaDisponivel()))
                .toList();
    }

    private static MensagemDoHistorico paraHistorico(Mensagem mensagem) {
        String identificacao = switch (mensagem.getRemetente()) {
            case CLIENTE -> mensagem.getAutor().getIdentificacao();
            case ATENDENTE -> ((Atendente) mensagem.getAutor()).getNome();
            case CHATBOT -> NOME_DO_BOT;
        };
        return new MensagemDoHistorico(mensagem.getRemetente().name(), identificacao,
                mensagem.getConteudo(), mensagem.getDataHora());
    }
}
