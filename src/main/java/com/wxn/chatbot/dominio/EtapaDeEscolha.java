package com.wxn.chatbot.dominio;

import java.util.List;
import java.util.Map;

/**
 * Uma pergunta com respostas prontas, que viram botoes no WhatsApp. Por texto,
 * vale o numero da opcao, o rotulo ou uma palavra parecida.
 */
public class EtapaDeEscolha extends Etapa {

    static final String AJUDA = "Não entendi a sua resposta. Pode escolher uma das opções?";

    private final String chave;
    private final List<Opcao> opcoes;

    /**
     * @param chave onde guardar a opcao escolhida nos dados do fluxo; nula quando
     *              a escolha so decide o caminho (como no menu)
     */
    public EtapaDeEscolha(String id, String pergunta, String chave, List<Opcao> opcoes) {
        super(id, pergunta);
        if (opcoes == null || opcoes.size() < 2) {
            throw new IllegalArgumentException("Uma escolha precisa de pelo menos duas opções.");
        }
        this.chave = chave;
        this.opcoes = List.copyOf(opcoes);
    }

    @Override
    public Transicao responder(String fala, Map<String, String> dados) {
        Opcao escolhida = reconhecer(fala);
        if (escolhida == null) {
            return Transicao.repetir(AJUDA);
        }
        if (chave != null) {
            dados.put(chave, escolhida.getRotulo());
        }
        return escolhida.getDestino();
    }

    /**
     * Pelo numero ("2"), pelo rotulo inteiro ou por um sinonimo. Se mais de uma
     * opcao combina, vale a que aparece primeiro na fala: em "não resolveu",
     * o "não" vem antes do "resolveu".
     */
    Opcao reconhecer(String fala) {
        List<String> palavras = Linguagem.palavras(fala);
        if (palavras.isEmpty()) {
            return null;
        }
        if (palavras.size() == 1 && palavras.get(0).matches("\\d{1,2}")) {
            int numero = Integer.parseInt(palavras.get(0));
            return numero >= 1 && numero <= opcoes.size() ? opcoes.get(numero - 1) : null;
        }
        String normalizada = String.join(" ", palavras);
        for (Opcao opcao : opcoes) {
            if (Linguagem.normalizar(opcao.getRotulo()).equals(normalizada)) {
                return opcao;
            }
        }
        Opcao melhor = null;
        int melhorPosicao = Integer.MAX_VALUE;
        for (Opcao opcao : opcoes) {
            for (String sinonimo : opcao.getSinonimos()) {
                int posicao = Linguagem.posicaoDaFrase(palavras, sinonimo);
                if (posicao >= 0 && posicao < melhorPosicao) {
                    melhor = opcao;
                    melhorPosicao = posicao;
                }
            }
        }
        return melhor;
    }

    @Override
    public List<String> getOpcoes() {
        return opcoes.stream().map(Opcao::getRotulo).toList();
    }

    @Override
    public List<Transicao> getDestinos() {
        return opcoes.stream().map(Opcao::getDestino).toList();
    }

    public String getChave() {
        return chave;
    }
}
