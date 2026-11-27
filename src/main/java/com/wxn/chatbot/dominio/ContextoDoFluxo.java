package com.wxn.chatbot.dominio;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Onde a conversa esta dentro de um fluxo guiado: o fluxo, a etapa, o que ja
 * foi coletado e quantas respostas seguidas nao serviram.
 *
 * E o "estado de trabalho" da secao 4 do modelo de dados: fica na coluna
 * conversa.contexto, em JSON, porque cada mensagem do WhatsApp chega como uma
 * requisicao separada e o Tino precisa lembrar em que pe parou. Imutavel: cada
 * passo cria um contexto novo.
 */
public final class ContextoDoFluxo {

    static final int VERSAO = 1;

    private final String fluxo;
    private final String etapa;
    private final Map<String, String> dados;
    private final int tentativas;

    public ContextoDoFluxo(String fluxo, String etapa, Map<String, String> dados, int tentativas) {
        if (fluxo == null || fluxo.isBlank() || etapa == null || etapa.isBlank()) {
            throw new IllegalArgumentException("O contexto precisa do fluxo e da etapa.");
        }
        if (tentativas < 0) {
            throw new IllegalArgumentException("As tentativas não podem ser negativas.");
        }
        this.fluxo = fluxo;
        this.etapa = etapa;
        this.dados = dados == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(dados));
        this.tentativas = tentativas;
    }

    /** Mais uma resposta que nao serviu, na mesma etapa. */
    public ContextoDoFluxo comMaisUmaTentativa() {
        return new ContextoDoFluxo(fluxo, etapa, dados, tentativas + 1);
    }

    /** No formato gravado no banco, o mesmo da secao 4 do modelo de dados. */
    Map<String, Object> paraMapa() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("versao", VERSAO);
        mapa.put("intencao", fluxo);
        mapa.put("etapa", etapa);
        mapa.put("entidades", dados);
        mapa.put("tentativas", tentativas);
        return mapa;
    }

    static ContextoDoFluxo deMapa(Map<?, ?> mapa) {
        Map<String, String> dados = new LinkedHashMap<>();
        if (mapa.get("entidades") instanceof Map<?, ?> entidades) {
            entidades.forEach((chave, valor) -> dados.put(String.valueOf(chave), String.valueOf(valor)));
        }
        int tentativas = mapa.get("tentativas") instanceof Number numero ? numero.intValue() : 0;
        return new ContextoDoFluxo((String) mapa.get("intencao"), (String) mapa.get("etapa"), dados, tentativas);
    }

    public String getFluxo() {
        return fluxo;
    }

    public String getEtapa() {
        return etapa;
    }

    public Map<String, String> getDados() {
        return dados;
    }

    public int getTentativas() {
        return tentativas;
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof ContextoDoFluxo c
                && fluxo.equals(c.fluxo) && etapa.equals(c.etapa)
                && dados.equals(c.dados) && tentativas == c.tentativas;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fluxo, etapa, dados, tentativas);
    }

    @Override
    public String toString() {
        return fluxo + "/" + etapa + " " + dados;
    }
}
