package com.wxn.chatbot.dominio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Um atendimento guiado sobre um assunto: produto com defeito, troca,
 * cancelamento. O Tino reconhece o assunto pelas frases e palavras-chave,
 * conduz o cliente etapa por etapa e, no fim, resolve ou passa o caso para um
 * atendente com um resumo do que coletou.
 *
 * O conteudo (perguntas, opcoes, textos da politica) e da WXN, como os artigos
 * da base de conhecimento; o Chatbot so sabe percorrer.
 */
public final class Fluxo {

    /** Nota de quem cita uma palavra-chave do assunto ("quebrado", "cancelar"). */
    static final double NOTA_DA_PALAVRA_CHAVE = 0.7;

    private final String id;
    private final String assunto;
    private final List<String> gatilhos;
    private final List<String> palavrasChave;
    private final Map<String, Etapa> etapas = new LinkedHashMap<>();
    private final Map<String, String> rotulosDoResumo;

    /**
     * @param gatilhos        frases inteiras que abrem o fluxo ("meu pedido chegou quebrado")
     * @param palavrasChave   palavras ou expressoes que, sozinhas, ja indicam o assunto
     * @param etapas          a primeira e onde o fluxo comeca
     * @param rotulosDoResumo o que entra no resumo do atendente, na ordem: chave dos dados -> rotulo
     */
    public Fluxo(String id, String assunto, List<String> gatilhos, List<String> palavrasChave,
            List<Etapa> etapas, Map<String, String> rotulosDoResumo) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("O fluxo precisa de um identificador.");
        }
        if (assunto == null || assunto.isBlank()) {
            throw new IllegalArgumentException("O fluxo precisa de um assunto.");
        }
        if (etapas == null || etapas.isEmpty()) {
            throw new IllegalArgumentException("O fluxo precisa de pelo menos uma etapa.");
        }
        this.id = id.strip();
        this.assunto = assunto.strip();
        this.gatilhos = gatilhos == null ? List.of() : List.copyOf(gatilhos);
        this.palavrasChave = palavrasChave == null ? List.of() : List.copyOf(palavrasChave);
        for (Etapa etapa : etapas) {
            if (this.etapas.putIfAbsent(etapa.getId(), etapa) != null) {
                throw new IllegalArgumentException("Etapa repetida no fluxo " + id + ": " + etapa.getId());
            }
        }
        for (Etapa etapa : etapas) {
            for (Transicao destino : etapa.getDestinos()) {
                if (destino.getTipo() == Transicao.Tipo.IR_PARA && !this.etapas.containsKey(destino.getAlvo())) {
                    throw new IllegalArgumentException(
                            "No fluxo " + id + ", a etapa " + etapa.getId() + " leva a uma etapa que não existe: "
                                    + destino.getAlvo());
                }
            }
        }
        this.rotulosDoResumo = rotulosDoResumo == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(rotulosDoResumo));
    }

    /**
     * Quanto a fala parece com este assunto, de 0 a 1: a semelhanca com a frase
     * mais parecida, ou a nota da palavra-chave, o que for maior.
     */
    public double pontuarPara(String fala) {
        List<String> palavras = Linguagem.palavras(fala);
        if (palavras.isEmpty()) {
            return 0.0;
        }
        double nota = 0.0;
        for (String gatilho : gatilhos) {
            nota = Math.max(nota, Linguagem.semelhanca(new HashSet<>(palavras),
                    new HashSet<>(Linguagem.palavras(gatilho))));
        }
        for (String palavra : palavrasChave) {
            if (Linguagem.posicaoDaFrase(palavras, palavra) >= 0) {
                nota = Math.max(nota, NOTA_DA_PALAVRA_CHAVE);
            }
        }
        return nota;
    }

    public Etapa getEtapaInicial() {
        return etapas.values().iterator().next();
    }

    public Optional<Etapa> getEtapa(String idDaEtapa) {
        return Optional.ofNullable(etapas.get(idDaEtapa));
    }

    public List<Etapa> getEtapas() {
        return List.copyOf(etapas.values());
    }

    /** Os fluxos que este pode abrir (a troca abre o de defeito, por exemplo). */
    public List<String> getFluxosDeDestino() {
        List<String> destinos = new ArrayList<>();
        for (Etapa etapa : etapas.values()) {
            for (Transicao destino : etapa.getDestinos()) {
                if (destino.getTipo() == Transicao.Tipo.INICIAR_FLUXO) {
                    destinos.add(destino.getAlvo());
                }
            }
        }
        return destinos;
    }

    /** O resumo para o atendente, uma informacao por linha. */
    public String resumir(Map<String, String> dados) {
        StringBuilder resumo = new StringBuilder("• Assunto: ").append(assunto);
        rotulosDoResumo.forEach((chave, rotulo) -> {
            String valor = dados.get(chave);
            if (valor != null) {
                resumo.append("\n• ").append(rotulo).append(": ").append(valor);
            }
        });
        return resumo.toString();
    }

    /** O mesmo resumo numa linha, para a previa da fila. */
    public String resumirEmUmaLinha(Map<String, String> dados) {
        StringBuilder resumo = new StringBuilder(assunto);
        rotulosDoResumo.forEach((chave, rotulo) -> {
            String valor = dados.get(chave);
            if (valor != null) {
                resumo.append(" · ").append(rotulo).append(": ").append(valor);
            }
        });
        return resumo.toString();
    }

    public String getId() {
        return id;
    }

    public String getAssunto() {
        return assunto;
    }

    @Override
    public String toString() {
        return "Fluxo{" + id + ", " + etapas.size() + " etapas}";
    }
}
