package com.nxn.chatbot.dto;

/**
 * Os indicadores do topo da Tela 6.
 *
 * A taxa de contencao e a metrica central do projeto: a pesquisa de similares
 * mostrou que o setor cobra por problema resolvido, nao por mensagem trocada.
 */
public record ResumoDaFila(
        long naFila,
        long emAtendimento,
        long esperaMediaEmSegundos,
        long resolvidasPeloBot,
        long total,
        double taxaContencao) {
}
