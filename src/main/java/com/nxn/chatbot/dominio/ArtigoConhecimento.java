package com.nxn.chatbot.dominio;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class ArtigoConhecimento {

    private String pergunta;
    private List<String> variacoes;
    private String resposta;
    private final String categoria;
    private final boolean publico;
    private boolean ativo;
    private LocalDateTime atualizadoEm;

    public ArtigoConhecimento(
            String pergunta,
            List<String> variacoes,
            String resposta,
            String categoria,
            boolean publico) {

        this.categoria = validarTexto(categoria, "Categoria");
        this.publico = publico;
        this.ativo = true;
        atualizar(pergunta, resposta, variacoes);
    }

    /**
     * O gestor corrige o conteúdo pelo painel, sem desenvolvedor (Épico 2).
     * Toda alteração carimba a data: conteúdo velho é o que faz o bot errar.
     */
    public final void atualizar(String pergunta, String resposta, List<String> variacoes) {
        this.pergunta = validarTexto(pergunta, "Pergunta");
        this.resposta = validarTexto(resposta, "Resposta");

        Objects.requireNonNull(
            variacoes, "A lista de variações é obrigatória."
        );

        for (String variacao : variacoes) {
            validarTexto(variacao, "Variação");
        }

        this.variacoes = List.copyOf(variacoes);
        this.atualizadoEm = LocalDateTime.now();
    }

    public void adicionarVariacao(String variacao) {
        validarTexto(variacao, "Variação");
        List<String> novas = new ArrayList<>(variacoes);
        novas.add(variacao);
        this.variacoes = List.copyOf(novas);
        this.atualizadoEm = LocalDateTime.now();
    }

    /** Desativar em vez de apagar preserva o vínculo com mensagens antigas. */
    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
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
        return Collections.unmodifiableList(variacoes);
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

    public boolean estaAtivo() {
        return ativo;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
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

    /** Artigo desativado nao responde; artigo interno nao chega a cliente externo. */
    public boolean disponivelPara(TipoCliente tipoCliente) {
        if (tipoCliente == null) {
            throw new IllegalArgumentException(
                "O tipo de cliente é obrigatório."
            );
        }

        if (!ativo) {
            return false;
        }

        return publico || tipoCliente == TipoCliente.INTERNO;
    }
}
