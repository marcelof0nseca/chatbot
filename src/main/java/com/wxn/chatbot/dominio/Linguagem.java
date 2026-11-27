package com.wxn.chatbot.dominio;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Como o Tino le o que o cliente escreve: sem acento, sem pontuacao, em
 * minusculas e palavra por palavra. "Não, obrigado!" vira [nao, obrigado].
 */
final class Linguagem {

    private Linguagem() {
    }

    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .strip();
    }

    static List<String> palavras(String texto) {
        String normalizado = normalizar(texto);
        return normalizado.isEmpty() ? List.of() : Arrays.asList(normalizado.split(" "));
    }

    /**
     * Onde a frase aparece na fala, como sequencia de palavras inteiras, ou -1.
     * "cobrado" nao aparece em "descobrado", e "duas vezes" aparece em
     * "fui cobrado duas vezes".
     */
    static int posicaoDaFrase(List<String> fala, String frase) {
        List<String> procurada = palavras(frase);
        if (procurada.isEmpty() || procurada.size() > fala.size()) {
            return -1;
        }
        for (int i = 0; i + procurada.size() <= fala.size(); i++) {
            if (fala.subList(i, i + procurada.size()).equals(procurada)) {
                return i;
            }
        }
        return -1;
    }

    /** Palavras em comum sobre o total de palavras diferentes: 1 e igual, 0 e nada a ver. */
    static double semelhanca(Collection<String> primeira, Collection<String> segunda) {
        if (primeira.isEmpty() || segunda.isEmpty()) {
            return 0.0;
        }
        Set<String> comuns = new HashSet<>(primeira);
        comuns.retainAll(segunda);
        Set<String> todas = new HashSet<>(primeira);
        todas.addAll(segunda);
        return (double) comuns.size() / todas.size();
    }
}
