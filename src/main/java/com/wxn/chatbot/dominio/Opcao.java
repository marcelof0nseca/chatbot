package com.wxn.chatbot.dominio;

import java.util.List;

/**
 * Uma das respostas prontas de uma etapa de escolha. No WhatsApp vira um botao;
 * por texto, o cliente pode responder o numero, o rotulo ou uma palavra
 * parecida ("defeito" escolhe "Produto com defeito").
 */
public final class Opcao {

    private final String rotulo;
    private final List<String> sinonimos;
    private final Transicao destino;

    public Opcao(String rotulo, List<String> sinonimos, Transicao destino) {
        if (rotulo == null || rotulo.isBlank()) {
            throw new IllegalArgumentException("O rótulo da opção é obrigatório.");
        }
        if (destino == null) {
            throw new IllegalArgumentException("A opção precisa de um destino.");
        }
        if (destino.getTipo() == Transicao.Tipo.REPETIR) {
            throw new IllegalArgumentException("Escolher uma opção não pode repetir a pergunta.");
        }
        this.rotulo = rotulo.strip();
        this.sinonimos = sinonimos == null ? List.of() : List.copyOf(sinonimos);
        this.destino = destino;
    }

    public String getRotulo() {
        return rotulo;
    }

    public List<String> getSinonimos() {
        return sinonimos;
    }

    public Transicao getDestino() {
        return destino;
    }
}
