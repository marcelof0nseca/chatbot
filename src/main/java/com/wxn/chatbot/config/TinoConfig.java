package com.wxn.chatbot.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.wxn.chatbot.dominio.ArtigoConhecimento;
import com.wxn.chatbot.dominio.Atendente;
import com.wxn.chatbot.dominio.Chatbot;
import com.wxn.chatbot.dominio.PerfilAtendente;

/**
 * O Tino que atende os clientes, com a base de conhecimento inicial.
 *
 * A base ainda vive em memoria: a tabela artigo_conhecimento esta desenhada no
 * modelo de dados, mas e de um proximo epico. Os artigos abaixo sao EXEMPLOS
 * para demonstrar o fluxo; a WXN os substitui pelo conteudo real.
 */
@Configuration
public class TinoConfig {

    /** Abaixo desta confianca o Tino nao responde: escala para um humano. */
    static final double LIMITE_DE_CONFIANCA = 0.45;

    @Bean
    public Chatbot tino() {
        Chatbot tino = new Chatbot("Tino", "1.0", LIMITE_DE_CONFIANCA);

        // Publicar exige um gestor (regra do dominio). Enquanto a base nao e
        // editada pelo painel, a carga inicial publica em nome deste gestor.
        Atendente cargaInicial = new Atendente(null, "Carga inicial", "base@wxn.com.br", PerfilAtendente.GESTOR);

        for (ArtigoConhecimento artigo : artigosDeExemplo()) {
            tino.publicarArtigo(artigo, cargaInicial);
        }
        return tino;
    }

    static List<ArtigoConhecimento> artigosDeExemplo() {
        return List.of(
                new ArtigoConhecimento(
                        "Qual o prazo para troca?",
                        List.of("prazo para trocar", "como faço para trocar um produto", "quero trocar um produto",
                                "posso trocar o produto"),
                        "A troca pode ser solicitada em até 30 dias corridos após o recebimento. "
                                + "O produto precisa estar sem uso e com a etiqueta original.",
                        "Trocas", true),
                new ArtigoConhecimento(
                        "Como acompanho meu pedido?",
                        List.of("como rastrear meu pedido", "rastreio do pedido", "acompanhar a entrega do pedido"),
                        "Você acompanha o pedido pelo link de rastreio enviado por e-mail logo depois do envio. "
                                + "O prazo de entrega aparece na confirmação da compra.",
                        "Entrega", true),
                new ArtigoConhecimento(
                        "Quais as formas de pagamento?",
                        List.of("posso pagar com pix", "aceita cartão de crédito", "posso parcelar a compra"),
                        "Aceitamos Pix, cartão de crédito e boleto. O Pix e o cartão são aprovados na hora; "
                                + "o boleto, em até 2 dias úteis.",
                        "Pagamento", true));
    }
}
