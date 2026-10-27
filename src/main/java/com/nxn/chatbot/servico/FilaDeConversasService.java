package com.nxn.chatbot.servico;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nxn.chatbot.dominio.Atendente;
import com.nxn.chatbot.dominio.Conversa;
import com.nxn.chatbot.dominio.Mensagem;
import com.nxn.chatbot.dominio.StatusTicket;
import com.nxn.chatbot.dominio.Ticket;
import com.nxn.chatbot.dto.ItemDaFila;
import com.nxn.chatbot.dto.ResumoDaFila;

/**
 * Regras de negocio da fila de conversas (Epico 4).
 *
 * O servico orquestra: busca, ordena, calcula e traduz o dominio para o que a
 * tela precisa. Ele nao reimplementa validacao — assumir um ticket continua
 * sendo decisao de Ticket.assumir(), que ja sabe recusar ticket que nao esta
 * aberto, atendente fora de servico e conversa encerrada.
 */
@Service
public class FilaDeConversasService {

    /** Tamanho da previa da conversa exibida na fila. */
    private static final int TAMANHO_DA_PREVIA = 80;

    private final FilaRepositorio repositorio;

    public FilaDeConversasService(FilaRepositorio repositorio) {
        if (repositorio == null) {
            throw new IllegalArgumentException("O repositório é obrigatório.");
        }
        this.repositorio = repositorio;
    }

    /**
     * A fila como o atendente ve: quem aguarda vem antes de quem ja esta sendo
     * atendido e, dentro de cada grupo, o caso mais antigo fica no topo.
     *
     * Transacional porque a previa le as mensagens da conversa, carregadas
     * sob demanda pelo JPA; fora de uma transacao isso falharia.
     */
    @Transactional(readOnly = true)
    public List<ItemDaFila> listarFila() {
        Stream<Ticket> aguardando = porAntiguidade(StatusTicket.ABERTO);
        Stream<Ticket> emAtendimento = porAntiguidade(StatusTicket.EM_ATENDIMENTO);
        return Stream.concat(aguardando, emAtendimento)
                .map(this::paraItem)
                .toList();
    }

    /** Indicadores do dia exibidos no topo da tela. */
    @Transactional(readOnly = true)
    public ResumoDaFila resumoDoDia() {
        List<Ticket> aguardando = repositorio.buscarPorStatus(StatusTicket.ABERTO);
        long emAtendimento = repositorio.buscarPorStatus(StatusTicket.EM_ATENDIMENTO).size();

        long esperaMedia = aguardando.isEmpty() ? 0L
                : Math.round(aguardando.stream()
                        .mapToLong(ticket -> ticket.tempoDeEspera().toSeconds())
                        .average()
                        .orElse(0.0));

        LocalDateTime inicioDoDia = LocalDate.now().atStartOfDay();
        long total = repositorio.contarConversasDesde(inicioDoDia);
        long peloBot = repositorio.contarConversasResolvidasPeloBotDesde(inicioDoDia);

        // Dia sem conversa nenhuma nao e contencao de 100%, e sim ausencia de dado.
        double contencao = (total == 0L) ? 0.0 : (double) peloBot / total;

        return new ResumoDaFila(aguardando.size(), emAtendimento, esperaMedia,
                peloBot, total, arredondar(contencao));
    }

    /**
     * O atendente assume um caso da fila. A conversa sai do bot no mesmo
     * movimento, porque quem muda o estado e o proprio Ticket.
     *
     * Ticket e conversa sao gravados na mesma transacao: ou os dois mudam,
     * ou nenhum muda.
     */
    @Transactional
    public ItemDaFila assumir(Long ticketId, Long atendenteId) {
        Ticket ticket = repositorio.buscarTicket(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket não encontrado: " + ticketId));
        Atendente atendente = repositorio.buscarAtendente(atendenteId)
                .orElseThrow(() -> new IllegalArgumentException("Atendente não encontrado: " + atendenteId));

        ticket.assumir(atendente);
        repositorio.salvar(ticket);
        return paraItem(ticket);
    }

    private Stream<Ticket> porAntiguidade(StatusTicket status) {
        return repositorio.buscarPorStatus(status).stream()
                .sorted(Comparator.comparing(Ticket::getAbertoEm));
    }

    private ItemDaFila paraItem(Ticket ticket) {
        Conversa conversa = ticket.getConversa();
        return new ItemDaFila(
                ticket.getId(),
                conversa.getCliente().getTelefoneMascarado(),
                ticket.getMotivo().name(),
                ticket.tempoDeEspera().toSeconds(),
                ticket.getStatus(),
                ultimaFalaDoCliente(conversa),
                ticket.getAtendente().map(Atendente::getNome).orElse(null));
    }

    /** O que o cliente disse por ultimo e o que diz ao atendente do que se trata. */
    private String ultimaFalaDoCliente(Conversa conversa) {
        List<Mensagem> mensagens = conversa.getMensagens();
        for (int i = mensagens.size() - 1; i >= 0; i--) {
            Mensagem mensagem = mensagens.get(i);
            if (mensagem.ehDoCliente()) {
                return resumir(mensagem.getConteudo());
            }
        }
        return "";
    }

    private static String resumir(String texto) {
        if (texto.length() <= TAMANHO_DA_PREVIA) {
            return texto;
        }
        return texto.substring(0, TAMANHO_DA_PREVIA - 1).trim() + "…";
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 1000.0) / 1000.0;
    }
}
