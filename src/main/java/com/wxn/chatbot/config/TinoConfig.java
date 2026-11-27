package com.wxn.chatbot.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.wxn.chatbot.dominio.ArtigoConhecimento;
import com.wxn.chatbot.dominio.Atendente;
import com.wxn.chatbot.dominio.Chatbot;
import com.wxn.chatbot.dominio.Etapa;
import com.wxn.chatbot.dominio.EtapaDeEscolha;
import com.wxn.chatbot.dominio.EtapaDeFoto;
import com.wxn.chatbot.dominio.EtapaDeTexto;
import com.wxn.chatbot.dominio.Fluxo;
import com.wxn.chatbot.dominio.Opcao;
import com.wxn.chatbot.dominio.PerfilAtendente;
import com.wxn.chatbot.dominio.Transicao;

/**
 * O Tino que atende os clientes, com a base de conhecimento e os fluxos
 * guiados iniciais.
 *
 * A base e os fluxos ainda vivem em memoria: as tabelas estao desenhadas no
 * modelo de dados, mas sao de um proximo epico. Os artigos e os textos de
 * politica abaixo (prazos, postagem, estorno) sao EXEMPLOS para demonstrar o
 * atendimento; a WXN os substitui pelo conteudo real.
 */
@Configuration
public class TinoConfig {

    /** Abaixo desta confianca o Tino nao responde: escala para um humano. */
    static final double LIMITE_DE_CONFIANCA = 0.45;

    /** Numero de pedido: so digitos, a partir de 4. Guarda so o numero ("é o 123456" vira "123456"). */
    static final Pattern NUMERO_DO_PEDIDO = Pattern.compile("(\\d{4,})");

    static final String AJUDA_DO_PEDIDO =
            "Não encontrei um número de pedido nessa mensagem. Ele tem só números, por exemplo 123456, "
            + "e está no e-mail de confirmação da compra.";

    static final String TEXTO_RASTREIO =
            "Você acompanha a entrega pelo link de rastreio que enviamos por e-mail assim que o pedido sai. "
            + "O prazo de entrega aparece na confirmação da compra.";

    static final String TEXTO_DEVOLUCAO =
            "Funciona assim:\n"
            + "1. Embale o produto, de preferência na caixa original, com os acessórios e o manual.\n"
            + "2. Você recebe por e-mail um código de postagem, sem custo.\n"
            + "3. Leve o pacote a uma agência dos Correios em até 7 dias.\n"
            + "4. Quando o produto chegar, fazemos a troca ou o reembolso em até 5 dias úteis.";

    static final String TEXTO_TROCA =
            "Funciona assim:\n"
            + "1. Embale o produto sem uso, com a etiqueta.\n"
            + "2. Você recebe por e-mail um código de postagem, sem custo.\n"
            + "3. Leve o pacote a uma agência dos Correios em até 7 dias.\n"
            + "4. Quando ele chegar, enviamos o novo tamanho ou cor, ou um vale-compra.";

    @Bean
    public Chatbot tino() {
        Chatbot tino = new Chatbot("Tino", "1.0", LIMITE_DE_CONFIANCA);

        // Publicar exige um gestor (regra do dominio). Enquanto a base nao e
        // editada pelo painel, a carga inicial publica em nome deste gestor.
        Atendente cargaInicial = new Atendente(null, "Carga inicial", "base@wxn.com.br", PerfilAtendente.GESTOR);

        for (ArtigoConhecimento artigo : artigosDeExemplo()) {
            tino.publicarArtigo(artigo, cargaInicial);
        }
        for (Fluxo fluxo : fluxosDeExemplo()) {
            tino.publicarFluxo(fluxo, cargaInicial);
        }
        tino.validarFluxos();
        return tino;
    }

    /** Perguntas de informacao: o Tino responde e pergunta se resolveu. */
    static List<ArtigoConhecimento> artigosDeExemplo() {
        return List.of(
                new ArtigoConhecimento(
                        "Qual o prazo para troca?",
                        List.of("prazo para trocar", "até quando posso trocar"),
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

    /** Assuntos que pedem conversa: o Tino conduz o cliente etapa por etapa. */
    static List<Fluxo> fluxosDeExemplo() {
        return List.of(menu(), acompanharPedido(), produtoComDefeito(), troca(), cancelamento(), pagamento());
    }

    static Fluxo menu() {
        return new Fluxo(Chatbot.FLUXO_MENU, "Menu", List.of(), List.of(), List.of(
                new EtapaDeEscolha("assunto", "Sobre o que você quer falar?", null, List.of(
                        opcao("Acompanhar um pedido", Transicao.iniciarFluxo("pedido"),
                                "acompanhar", "entrega", "rastrear", "rastreio", "atraso"),
                        opcao("Produto com defeito", Transicao.iniciarFluxo("defeito"),
                                "defeito", "quebrado", "quebrada", "danificado", "estragado"),
                        opcao("Trocar um produto", Transicao.iniciarFluxo("troca"),
                                "troca", "trocar", "tamanho"),
                        opcao("Cancelar uma compra", Transicao.iniciarFluxo("cancelamento"),
                                "cancelar", "cancelamento"),
                        opcao("Pagamento", Transicao.iniciarFluxo("pagamento"),
                                "pagamento", "pagar", "boleto", "cobranca", "pix", "cartao"),
                        opcao("Falar com um atendente", Transicao.passarParaAtendente("Claro!"),
                                "atendente", "pessoa")))),
                Map.of());
    }

    static Fluxo acompanharPedido() {
        return new Fluxo("pedido", "Acompanhar um pedido",
                List.of("onde está meu pedido", "meu pedido não chegou", "meu pedido está atrasado",
                        "quero acompanhar meu pedido", "cadê meu pedido"),
                List.of("atrasado", "atrasada", "atraso", "nao chegou", "ainda nao recebi"),
                List.of(
                        numeroDoPedido("Claro! Qual é o número do pedido? Ele está no e-mail de confirmação da compra.",
                                Transicao.irPara("prazo")),
                        escolha("prazo", "Obrigado! E o prazo de entrega, como está?", "prazo",
                                opcao("Ainda está no prazo",
                                        Transicao.concluir(TEXTO_RASTREIO + " Se passar do prazo, é só me chamar."),
                                        "no prazo", "dentro"),
                                opcao("Já passou do prazo",
                                        Transicao.irPara("verificar", "Sinto muito pela demora."),
                                        "passou", "atrasado", "atrasou"),
                                opcao("Não sei o prazo",
                                        Transicao.concluir(TEXTO_RASTREIO), "nao sei")),
                        escolha("verificar",
                                "Posso pedir para um atendente verificar com a transportadora e te dar um retorno?",
                                null,
                                opcao("Sim, por favor", Transicao.passarParaAtendente("Combinado!"), "sim"),
                                opcao("Agora não", Transicao.concluir(
                                        "Tudo bem. Se o pedido não chegar nos próximos dias, é só me chamar."),
                                        "nao"))),
                resumo("pedido", "Pedido", "prazo", "Prazo de entrega"));
    }

    static Fluxo produtoComDefeito() {
        return new Fluxo("defeito", "Produto com defeito",
                List.of("meu pedido chegou quebrado", "o produto chegou quebrado", "produto com defeito",
                        "o produto veio com defeito", "o produto não funciona", "chegou danificado"),
                List.of("quebrado", "quebrada", "quebrou", "defeito", "defeituoso", "danificado", "danificada",
                        "avariado", "avariada", "estragado", "amassado", "nao funciona", "nao liga"),
                List.of(
                        numeroDoPedido("Sinto muito por isso! Vou te ajudar a resolver. Qual é o número do pedido? "
                                + "Ele está no e-mail de confirmação da compra.", Transicao.irPara("problema")),
                        escolha("problema", "O que aconteceu com o produto?", "problema",
                                opcao("Chegou quebrado ou amassado", Transicao.irPara("foto"),
                                        "quebrado", "quebrada", "amassado", "danificado", "avariado"),
                                opcao("Não funciona direito", Transicao.irPara("foto"),
                                        "funciona", "liga", "defeito"),
                                opcao("Veio diferente do que comprei", Transicao.irPara("foto"),
                                        "diferente", "errado", "outro")),
                        new EtapaDeFoto("foto",
                                "Pode me mandar uma foto do produto e da embalagem? Ela ajuda a resolver mais rápido.",
                                "foto", Transicao.irPara("preferencia")),
                        escolha("preferencia", "Como você prefere resolver?", "preferencia",
                                opcao("Trocar por um novo", Transicao.irPara("devolucao"), "trocar", "troca", "novo"),
                                opcao("Receber o dinheiro de volta", Transicao.irPara("devolucao"),
                                        "dinheiro", "reembolso", "estorno", "devolver")),
                        escolha("devolucao", TEXTO_DEVOLUCAO + "\n\nPosso abrir a solicitação agora? "
                                + "Um atendente confere as informações e te envia o código de postagem.", null,
                                opcao("Sim, pode abrir", Transicao.passarParaAtendente("Pronto, solicitação aberta!"),
                                        "sim", "pode"),
                                opcao("Agora não", Transicao.concluir(
                                        "Tudo bem. Quando quiser abrir a solicitação, é só me chamar."), "nao"))),
                resumo("pedido", "Pedido", "problema", "Problema", "foto", "Foto", "preferencia", "Prefere"));
    }

    static Fluxo troca() {
        return new Fluxo("troca", "Trocar um produto",
                List.of("quero trocar um produto", "quero fazer uma troca", "trocar o tamanho", "trocar a cor",
                        "posso trocar o produto", "como faço para trocar um produto"),
                List.of("trocar", "troca"),
                List.of(
                        escolha("motivo", "Vamos lá! Por que você quer trocar?", "motivo",
                                opcao("Tamanho ou cor", Transicao.irPara("pedido"),
                                        "tamanho", "cor", "serviu", "pequeno", "grande"),
                                opcao("Não gostei do produto", Transicao.irPara("pedido"),
                                        "gostei", "arrependi", "desisti"),
                                opcao("Veio com defeito", Transicao.iniciarFluxo("defeito",
                                        "Nesse caso, vou te ajudar pelo atendimento de produto com defeito."),
                                        "defeito", "quebrado", "quebrada")),
                        numeroDoPedido("Qual é o número do pedido? Ele está no e-mail de confirmação da compra.",
                                Transicao.irPara("estado")),
                        escolha("estado", "O produto está sem uso, com a etiqueta e na embalagem original?", "estado",
                                opcao("Sim, está sem uso", Transicao.irPara("recebido"), "sim", "sem uso"),
                                opcao("Já usei", Transicao.concluir(
                                        "Pela nossa política, a troca só vale para produto sem uso e com a etiqueta. "
                                                + "Se ele apresentou defeito, escolha “Produto com defeito” no menu "
                                                + "que eu te ajudo por lá."),
                                        "usei", "usado", "nao")),
                        escolha("recebido", "Faz quanto tempo que você recebeu o produto?", "recebido",
                                opcao("Até 30 dias", Transicao.irPara("como"), "ate", "menos"),
                                opcao("Mais de 30 dias", Transicao.irPara("fora-do-prazo"), "mais")),
                        escolha("fora-do-prazo", "A troca pode ser pedida em até 30 dias depois do recebimento, "
                                + "então esse pedido já passou do prazo. Quer que um atendente avalie o seu caso?", null,
                                opcao("Sim, quero", Transicao.passarParaAtendente("Combinado!"), "sim"),
                                opcao("Não precisa", Transicao.concluir("Tudo bem, obrigado por avisar."), "nao")),
                        escolha("como", TEXTO_TROCA + "\n\nPosso abrir a troca agora?", null,
                                opcao("Sim, pode abrir", Transicao.passarParaAtendente("Pronto, troca aberta!"),
                                        "sim", "pode"),
                                opcao("Agora não", Transicao.concluir(
                                        "Tudo bem! Você tem até 30 dias depois do recebimento para pedir a troca."),
                                        "nao"))),
                resumo("motivo", "Motivo", "pedido", "Pedido", "estado", "Produto", "recebido", "Recebido há"));
    }

    static Fluxo cancelamento() {
        return new Fluxo("cancelamento", "Cancelar uma compra",
                List.of("quero cancelar minha compra", "cancelar o pedido", "quero cancelar um pedido",
                        "preciso cancelar uma compra"),
                List.of("cancelar", "cancelamento", "cancela"),
                List.of(
                        numeroDoPedido("Certo. Qual é o número do pedido que você quer cancelar?",
                                Transicao.irPara("envio")),
                        escolha("envio", "O pedido já foi enviado? Você recebe um e-mail quando ele sai.", "envio",
                                opcao("Ainda não foi enviado", Transicao.irPara("confirmar",
                                        "Como ainda não saiu, o cancelamento é sem custo. O estorno aparece em até "
                                                + "5 dias úteis no Pix ou no boleto, e em até duas faturas no cartão."),
                                        "ainda nao", "nao foi"),
                                opcao("Já foi enviado", Transicao.irPara("confirmar",
                                        "Como já saiu, você pode recusar a entrega quando ela chegar, ou pedir a "
                                                + "devolução em até 7 dias depois de receber: é o direito de "
                                                + "arrependimento. O estorno sai quando o produto voltar."),
                                        "ja foi", "enviado", "saiu"),
                                opcao("Não sei", Transicao.irPara("confirmar",
                                        "Sem problema: o atendente confere se o pedido já saiu e te explica o "
                                                + "próximo passo."),
                                        "nao sei")),
                        escolha("confirmar", "Posso pedir o cancelamento a um atendente agora?", null,
                                opcao("Sim, quero cancelar", Transicao.passarParaAtendente(
                                        "Pedido de cancelamento registrado!"), "sim", "quero"),
                                opcao("Desisti de cancelar", Transicao.concluir(
                                        "Combinado, o pedido continua normalmente."), "desisti", "nao"))),
                resumo("pedido", "Pedido", "envio", "Situação"));
    }

    static Fluxo pagamento() {
        return new Fluxo("pagamento", "Pagamento",
                List.of("problema com o pagamento", "fui cobrado duas vezes", "cobrança em dobro",
                        "segunda via do boleto", "meu pagamento não foi aprovado", "cobrança indevida"),
                List.of("cobranca", "cobrado", "cobrada", "cobraram", "cobrou", "boleto", "estorno",
                        "segunda via", "nao aprovado", "recusado"),
                List.of(
                        escolha("tipo", "Certo! O que aconteceu com o pagamento?", "tipo",
                                opcao("Fui cobrado a mais ou duas vezes", Transicao.irPara("pedido-cobranca"),
                                        "dobro", "duas vezes", "duplicada", "indevida", "a mais", "cobrado",
                                        "cobraram", "cobrou"),
                                opcao("Pagamento não aprovado", Transicao.irPara("nao-aprovado"),
                                        "aprovado", "recusado", "negado"),
                                opcao("Segunda via do boleto", Transicao.irPara("pedido-boleto"),
                                        "boleto", "segunda via"),
                                opcao("Outro assunto de pagamento", Transicao.passarParaAtendente("Certo!"),
                                        "outro")),
                        new EtapaDeTexto("pedido-cobranca", "Vou te ajudar com isso. Qual é o número do pedido?",
                                "pedido", NUMERO_DO_PEDIDO, AJUDA_DO_PEDIDO, Transicao.irPara("detalhe")),
                        new EtapaDeTexto("detalhe", "Me conta o que aconteceu: o valor cobrado, a data e os 4 "
                                + "últimos dígitos do cartão. Por segurança, nunca mande o número completo.",
                                "detalhe", null, null, Transicao.passarParaAtendente(
                                        "Obrigado! Cobrança indevida precisa ser conferida por uma pessoa.")),
                        escolha("nao-aprovado", "Algumas dicas que costumam resolver:\n"
                                + "• Confira os dados e o limite do cartão.\n"
                                + "• Tente o Pix, que é aprovado na hora.\n"
                                + "• Se o banco bloqueou a compra, libere pelo aplicativo dele.\n\n"
                                + "Deu certo?", null,
                                opcao("Sim, consegui pagar", Transicao.concluir("Que bom!"), "sim", "consegui"),
                                opcao("Não, continua recusando", Transicao.passarParaAtendente(
                                        "Vou chamar alguém para olhar com você."), "nao", "continua")),
                        new EtapaDeTexto("pedido-boleto", "Qual é o número do pedido do boleto?",
                                "pedido", NUMERO_DO_PEDIDO, AJUDA_DO_PEDIDO, Transicao.passarParaAtendente(
                                        "Pronto! Pedi a segunda via: ela chega por aqui e no seu e-mail."))),
                resumo("tipo", "Problema", "pedido", "Pedido", "detalhe", "Detalhe"));
    }

    // ---------- atalhos para escrever os fluxos ----------

    private static Etapa numeroDoPedido(String pergunta, Transicao proxima) {
        return new EtapaDeTexto("pedido", pergunta, "pedido", NUMERO_DO_PEDIDO, AJUDA_DO_PEDIDO, proxima);
    }

    private static EtapaDeEscolha escolha(String id, String pergunta, String chave, Opcao... opcoes) {
        return new EtapaDeEscolha(id, pergunta, chave, List.of(opcoes));
    }

    private static Opcao opcao(String rotulo, Transicao destino, String... sinonimos) {
        return new Opcao(rotulo, List.of(sinonimos), destino);
    }

    /** Pares chave, rotulo, na ordem em que aparecem no resumo do atendente. */
    private static Map<String, String> resumo(String... chavesERotulos) {
        Map<String, String> rotulos = new LinkedHashMap<>();
        for (int i = 0; i + 1 < chavesERotulos.length; i += 2) {
            rotulos.put(chavesERotulos[i], chavesERotulos[i + 1]);
        }
        return rotulos;
    }
}
