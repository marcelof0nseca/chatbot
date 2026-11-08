package com.wxn.chatbot.servico;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wxn.chatbot.dominio.Atendente;
import com.wxn.chatbot.dominio.Conversa;
import com.wxn.chatbot.dominio.Mensagem;
import com.wxn.chatbot.dominio.StatusTicket;
import com.wxn.chatbot.dominio.Ticket;
import com.wxn.chatbot.dominio.Transferencia;
import com.wxn.chatbot.dto.ItemDaFila;
import com.wxn.chatbot.dto.ResumoDaFila;

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

    /** Relogio com a zona fixa: define onde comeca o "dia" do resumo. */
    private final Clock relogio;

    public FilaDeConversasService(FilaRepositorio repositorio, Clock relogio) {
        if (repositorio == null) {
            throw new IllegalArgumentException("O repositório é obrigatório.");
        }
        if (relogio == null) {
            throw new IllegalArgumentException("O relógio é obrigatório.");
        }
        this.repositorio = repositorio;
        this.relogio = relogio;
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

        // Meia-noite de Recife, nao a do servidor: em UTC o dia viraria as 21h.
        LocalDateTime inicioDoDia = LocalDate.now(relogio).atStartOfDay();
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
        Ticket ticket = buscarTicket(ticketId);
        Atendente atendente = buscarAtendente(atendenteId);

        ticket.assumir(atendente);
        repositorio.salvar(ticket);
        return paraItem(ticket);
    }

    /**
     * Passa um caso em atendimento para outro atendente, sem devolve-lo a
     * fila. As regras (quem pode transferir, para quem, com que motivo) sao
     * do Ticket.transferirPara(); o servico busca, chama e grava.
     *
     * O ticket com o novo atendente e o registro da transferencia sao
     * gravados na mesma transacao: nao existe caso transferido sem registro,
     * nem registro de uma transferencia que nao aconteceu.
     */
    @Transactional
    public ItemDaFila transferir(Long ticketId, Long quemTransfereId, Long destinoId, String motivo) {
        Ticket ticket = buscarTicket(ticketId);
        Atendente quemTransfere = buscarAtendente(quemTransfereId);
        Atendente destino = buscarAtendente(destinoId);

        Transferencia transferencia = ticket.transferirPara(quemTransfere, destino, motivo);
        repositorio.salvar(ticket);
        repositorio.salvarTransferencia(transferencia);
        return paraItem(ticket);
    }

    private Ticket buscarTicket(Long ticketId) {
        if (ticketId == null) {
            throw new IllegalArgumentException("O ticket é obrigatório.");
        }
        return repositorio.buscarTicket(ticketId)
                .orElseThrow(() -> new NaoEncontradoException("Ticket não encontrado: " + ticketId));
    }

    /** Id nulo e dado invalido (400), e nao atendente inexistente (404). */
    private Atendente buscarAtendente(Long atendenteId) {
        if (atendenteId == null) {
            throw new IllegalArgumentException("Atendente é obrigatório.");
        }
        return repositorio.buscarAtendente(atendenteId)
                .orElseThrow(() -> new NaoEncontradoException("Atendente não encontrado: " + atendenteId));
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
