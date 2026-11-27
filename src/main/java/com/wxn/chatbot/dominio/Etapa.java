package com.wxn.chatbot.dominio;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Um passo de um fluxo guiado: o Tino pergunta, o cliente responde e a etapa
 * decide o que vem depois.
 *
 * Cada tipo de etapa le a resposta do seu jeito (uma escolha, um texto, uma
 * foto), mas o Chatbot conversa com todas pelo mesmo contrato: e o
 * polimorfismo que deixa criar um tipo novo sem mexer no Chatbot.
 */
public abstract class Etapa {

    private final String id;
    private final String pergunta;

    protected Etapa(String id, String pergunta) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("A etapa precisa de um identificador.");
        }
        if (pergunta == null || pergunta.isBlank()) {
            throw new IllegalArgumentException("A etapa precisa de uma pergunta.");
        }
        this.id = id.strip();
        this.pergunta = pergunta.strip();
    }

    /**
     * Le a resposta do cliente. O que ela coletar vai para os dados do fluxo
     * (o numero do pedido, o defeito escolhido), que viram o resumo do atendente.
     */
    public abstract Transicao responder(String fala, Map<String, String> dados);

    /** Para onde esta etapa pode levar. Serve para conferir, ao publicar, que nenhum destino falta. */
    public abstract List<Transicao> getDestinos();

    /** As respostas prontas, que viram botoes. A maioria das etapas nao tem. */
    public List<String> getOpcoes() {
        return List.of();
    }

    /**
     * Quando o dado desta etapa ja foi coletado (o numero do pedido, informado
     * num fluxo anterior), ela e pulada: o cliente nao responde duas vezes.
     */
    public Optional<Transicao> pularSeJaRespondida(Map<String, String> dados) {
        return Optional.empty();
    }

    public String getId() {
        return id;
    }

    public String getPergunta() {
        return pergunta;
    }
}
