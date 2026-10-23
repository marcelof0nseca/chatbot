package com.nxn.chatbot.repositorio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.nxn.chatbot.dominio.Atendente;
import com.nxn.chatbot.dominio.Cliente;
import com.nxn.chatbot.dominio.Conversa;
import com.nxn.chatbot.dominio.MotivoEscalonamento;
import com.nxn.chatbot.dominio.PerfilAtendente;
import com.nxn.chatbot.dominio.StatusTicket;
import com.nxn.chatbot.dominio.Ticket;
import com.nxn.chatbot.dominio.TipoCliente;
import com.nxn.chatbot.servico.FilaRepositorio;

import jakarta.annotation.PostConstruct;

/**
 * Implementacao provisoria em memoria da porta FilaRepositorio.
 *
 * Existe para a fila funcionar ponta a ponta enquanto o mapeamento JPA das
 * entidades nao esta pronto. Quando os repositorios Spring Data chegarem,
 * esta classe e substituida por um adaptador que delega a eles — nenhuma
 * linha do servico muda, porque ele depende da porta e nao da implementacao.
 */
@Repository
public class FilaRepositorioEmMemoria implements FilaRepositorio {

    private final Map<Long, Ticket> tickets = new LinkedHashMap<>();
    private final Map<Long, Atendente> atendentes = new LinkedHashMap<>();
    private final List<Conversa> conversas = new ArrayList<>();

    public void registrar(Conversa conversa) {
        conversas.add(conversa);
        conversa.getTicket().ifPresent(ticket -> tickets.put(ticket.getId(), ticket));
    }

    public void registrar(Atendente atendente) {
        atendentes.put(atendente.getId(), atendente);
    }

    @Override
    public List<Ticket> buscarPorStatus(StatusTicket status) {
        return tickets.values().stream()
                .filter(ticket -> ticket.getStatus() == status)
                .toList();
    }

    @Override
    public Optional<Ticket> buscarTicket(Long id) {
        return Optional.ofNullable(tickets.get(id));
    }

    @Override
    public Optional<Atendente> buscarAtendente(Long id) {
        return Optional.ofNullable(atendentes.get(id));
    }

    @Override
    public void salvar(Ticket ticket) {
        tickets.put(ticket.getId(), ticket);
    }

    @Override
    public long contarConversasDesde(LocalDateTime inicio) {
        return conversas.stream()
                .filter(conversa -> conversa.getIniciadaEm().isAfter(inicio))
                .count();
    }

    @Override
    public long contarConversasResolvidasPeloBotDesde(LocalDateTime inicio) {
        return conversas.stream()
                .filter(conversa -> conversa.getIniciadaEm().isAfter(inicio))
                .filter(Conversa::resolvidaPeloBot)
                .count();
    }

    /** Carga de exemplo, para a tela ter o que mostrar antes do banco existir. */
    @PostConstruct
    void carregarExemplos() {
        Atendente bruno = new Atendente(1L, "Bruno Dias", "bruno@wxn.com.br", PerfilAtendente.ATENDENTE);
        bruno.entrarEmServico();
        registrar(bruno);

        Atendente ana = new Atendente(2L, "Ana Lima", "ana@wxn.com.br", PerfilAtendente.GESTOR);
        ana.entrarEmServico();
        registrar(ana);

        registrar(escalonada(1L, "Maria Souza", "5581999994821",
                "o cupom BLACK30 não aplicou no meu carrinho e a cobrança veio dobrada",
                MotivoEscalonamento.BAIXA_CONFIANCA));
        registrar(escalonada(2L, "João Pereira", "5581988881190",
                "quero falar com um atendente",
                MotivoEscalonamento.SOLICITACAO_CLIENTE));
        registrar(escalonada(3L, "Carla Nunes", "5581977777735",
                "preciso cancelar uma compra",
                MotivoEscalonamento.FORA_DO_EXPEDIENTE));

        registrar(resolvidaPeloBot(10L, "Pedro Alves", "5581966663064"));
        registrar(resolvidaPeloBot(11L, "Lucia Ramos", "5581955552218"));
        registrar(resolvidaPeloBot(12L, "Rafael Dias", "5581944449907"));
    }

    private static Conversa escalonada(Long id, String nome, String telefone, String fala,
                                       MotivoEscalonamento motivo) {
        Conversa conversa = iniciar(id, nome, telefone);
        conversa.registrarMensagem(conversa.getCliente(), fala);
        conversa.escalonar(id, motivo);
        return conversa;
    }

    private static Conversa resolvidaPeloBot(Long id, String nome, String telefone) {
        Conversa conversa = iniciar(id, nome, telefone);
        conversa.registrarMensagem(conversa.getCliente(), "qual o prazo para troca?");
        conversa.encerrar();
        return conversa;
    }

    private static Conversa iniciar(Long id, String nome, String telefone) {
        Cliente cliente = new Cliente(id, nome, telefone, TipoCliente.EXTERNO);
        cliente.registrarConsentimento("v1");
        return new Conversa(id, cliente);
    }
}
