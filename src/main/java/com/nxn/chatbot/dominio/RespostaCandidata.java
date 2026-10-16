package com.nxn.chatbot.dominio;

import java.util.Objects;

public final class RespostaCandidata
        implements Comparable<RespostaCandidata> {

    private final ArtigoConhecimento artigo;
    private final double confianca;

    public RespostaCandidata(
            ArtigoConhecimento artigo,
            double confianca) {

        this.artigo = Objects.requireNonNull(
            artigo, "O artigo é obrigatório."
        );

        if (!Double.isFinite(confianca)
                || confianca < 0.0
                || confianca > 1.0) {
            throw new IllegalArgumentException(
                "A confiança deve estar entre 0 e 1."
            );
        }

        this.confianca = confianca;
    }

    public ArtigoConhecimento getArtigo() {
        return artigo;
    }

    public double getConfianca() {
        return confianca;
    }

    @Override
    public int compareTo(RespostaCandidata outra) {
        return Double.compare(
            outra.confianca, this.confianca
        );
    }
}