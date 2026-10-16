package com.nxn.chatbot.dominio;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class ArtigoConhecimento {

    private final String pergunta;
    private final List<String> variacoes;
    private final String resposta;
    private final String categoria;
    private final boolean publico;

    public ArtigoConhecimento(
            String pergunta,
            List<String> variacoes,
            String resposta,
            String categoria,
            boolean publico) {

        this.pergunta = validarTexto(pergunta, "Pergunta");
        this.resposta = validarTexto(resposta, "Resposta");
        this.categoria = validarTexto(categoria, "Categoria");

        Objects.requireNonNull(
            variacoes, "A lista de variações é obrigatória."
        );

        for (String variacao : variacoes) {
            validarTexto(variacao, "Variação");
        }

        this.variacoes = List.copyOf(variacoes);
        this.publico = publico;
    }

    private static String validarTexto(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(
                campo + " não pode estar vazio."
            );
        }

        return texto;
    }

    public String getPergunta() {
        return pergunta;
    }

    public List<String> getVariacoes() {
        return variacoes;
    }

    public String getResposta() {
        return resposta;
    }

    public String getCategoria() {
        return categoria;
    }

    public boolean isPublico() {
        return publico;
    }

    public double pontuarPara(String texto) {
        if (texto == null || texto.isBlank()) {
            return 0.0;
        }

        Set<String> palavrasRecebidas = separarPalavras(texto);

        double melhorPontuacao = calcularSimilaridade(
            palavrasRecebidas, separarPalavras(pergunta)
        );

        for (String variacao : variacoes) {
            double pontuacao = calcularSimilaridade(
                palavrasRecebidas, separarPalavras(variacao)
            );

            melhorPontuacao = Math.max(
                melhorPontuacao, pontuacao
            );
        }

        return melhorPontuacao;
    }

    private static Set<String> separarPalavras(String texto) {
        String normalizado = Normalizer.normalize(
            texto, Normalizer.Form.NFD
        );

        normalizado = normalizado
            .replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{L}\\p{N}]+", " ")
            .strip();

        if (normalizado.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(
            Arrays.asList(normalizado.split("\\s+"))
        );
    }

    private static double calcularSimilaridade(
            Set<String> primeiras,
            Set<String> segundas) {

        if (primeiras.isEmpty() || segundas.isEmpty()) {
            return 0.0;
        }

        Set<String> intersecao = new HashSet<>(primeiras);
        intersecao.retainAll(segundas);

        Set<String> uniao = new HashSet<>(primeiras);
        uniao.addAll(segundas);

        return (double) intersecao.size() / uniao.size();
    }
    
    public boolean disponivelPara(TipoCliente tipoCliente) {
        if (tipoCliente == null) {
            throw new IllegalArgumentException(
                "O tipo de cliente é obrigatório."
            );
        }

        return publico || tipoCliente == TipoCliente.INTERNO;
    }
}