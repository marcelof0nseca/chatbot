package com.wxn.chatbot.dominio;

import java.util.List;
import java.util.Map;

/**
 * O Tino pede uma foto (do produto, da embalagem). A foto e opcional: quem nao
 * consegue mandar agora segue o atendimento, e o atendente pode pedir depois.
 *
 * Hoje a foto chega como uma mensagem que comeca com [Foto], que e o que o
 * simulador manda. Com o WhatsApp, a imagem chega pelo webhook e o canal
 * escreve a mesma marcacao.
 */
public class EtapaDeFoto extends Etapa {

    public static final String MARCA_DE_FOTO = "[Foto]";
    public static final String ENVIAR = "Enviar foto";
    public static final String SEM_FOTO = "Não consigo enviar agora";

    static final String ENVIADA = "enviada";
    static final String NAO_ENVIADA = "não enviada";

    private final String chave;
    private final Transicao proxima;

    public EtapaDeFoto(String id, String pergunta, String chave, Transicao proxima) {
        super(id, pergunta);
        if (chave == null || chave.isBlank()) {
            throw new IllegalArgumentException("A etapa de foto precisa de uma chave.");
        }
        if (proxima == null || proxima.getTipo() == Transicao.Tipo.REPETIR) {
            throw new IllegalArgumentException("A etapa de foto precisa de um próximo passo.");
        }
        this.chave = chave;
        this.proxima = proxima;
    }

    @Override
    public Transicao responder(String fala, Map<String, String> dados) {
        String texto = fala == null ? "" : fala.strip();
        List<String> palavras = Linguagem.palavras(texto);
        if (texto.startsWith(MARCA_DE_FOTO) || palavras.equals(List.of("1"))
                || Linguagem.normalizar(texto).equals(Linguagem.normalizar(ENVIAR))) {
            dados.put(chave, ENVIADA);
            return proxima.antecedidaPor("Recebi a foto, obrigado!");
        }
        if (palavras.equals(List.of("2")) || Linguagem.posicaoDaFrase(palavras, "nao") == 0
                || Linguagem.posicaoDaFrase(palavras, "depois") >= 0
                || Linguagem.posicaoDaFrase(palavras, "sem foto") >= 0) {
            dados.put(chave, NAO_ENVIADA);
            return proxima.antecedidaPor("Sem problema, o atendente pode pedir a foto depois.");
        }
        return Transicao.repetir("Para seguir, mande a foto ou toque em “" + SEM_FOTO + "”.");
    }

    @Override
    public List<String> getOpcoes() {
        return List.of(ENVIAR, SEM_FOTO);
    }

    @Override
    public List<Transicao> getDestinos() {
        return List.of(proxima);
    }

    public String getChave() {
        return chave;
    }
}
