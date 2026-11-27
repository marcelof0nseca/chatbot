package com.wxn.chatbot.dominio;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * O Tino. Tambem e um Remetente, porque assina as mensagens que envia.
 *
 * Aqui mora a regra central do projeto: abaixo do limite de confianca o bot
 * nao responde conteudo nenhum, assume que nao sabe e escalona. "Nunca
 * inventar" e codigo, nao recomendacao de comportamento.
 *
 * Com fluxos publicados (e um deles e o menu), o Tino tambem conduz o
 * cliente: reconhece o assunto, pergunta o que precisa, explica o proximo
 * passo e, quando o caso pede uma pessoa, passa com um resumo pronto. Tudo o
 * que ele diz num fluxo foi escrito pela WXN: conduzir tambem nao e inventar.
 */
public class Chatbot implements Remetente {

    public static final String AVISO_PRIVACIDADE =
            "Olá! Eu sou o Tino, o assistente virtual da WXN. Você está falando com um robô. "
            + "Para te atender preciso registrar o seu número e o conteúdo desta conversa. "
            + "Posso continuar?";

    public static final String TEXTO_FALLBACK =
            "Não tenho certeza sobre esse caso e prefiro não te dar uma resposta errada. "
            + "Vou te passar para um atendente, com o histórico desta conversa.";

    public static final String TEXTO_APOS_ACEITE =
            "Obrigado! Agora posso te ajudar. Qual é a sua dúvida?";

    public static final String TEXTO_APOS_RECUSA =
            "Tudo bem. Sem o seu aceite eu não consigo continuar o atendimento por aqui. "
            + "Se mudar de ideia, é só responder SIM.";

    /** Versao do aviso de privacidade que o cliente aceita ao responder SIM. */
    public static final String VERSAO_DA_POLITICA = "v1";

    // ---------- textos do atendimento guiado ----------

    /** O fluxo que abre o atendimento guiado: a lista de assuntos. */
    public static final String FLUXO_MENU = "menu";

    public static final String TEXTO_ACEITE_GUIADO = "Obrigado! Agora posso te ajudar.";
    public static final String TEXTO_SAUDACAO = "Oi! Que bom falar com você.";
    public static final String TEXTO_NAO_ENTENDI =
            "Não entendi bem o que você precisa. Pode escolher um dos assuntos abaixo?";
    public static final String TEXTO_DESPEDIDA =
            "Obrigado pelo contato! Se precisar de mais alguma coisa, é só mandar uma mensagem.";
    public static final String TEXTO_PASSAGEM_COM_RESUMO =
            "Um atendente vai continuar daqui, com esta conversa e o resumo abaixo. "
            + "Você não precisa repetir nada.";
    public static final String TEXTO_PASSAGEM =
            "Um atendente vai continuar daqui, com o histórico desta conversa. Você não precisa repetir nada.";

    /** Fluxos do proprio Tino, que fecham um atendimento. O "_" os separa dos fluxos da WXN. */
    static final String FLUXO_CONFIRMACAO = "_confirmacao";
    static final String FLUXO_ENCERRAMENTO = "_encerramento";

    /** Respostas seguidas que nao servem, na mesma etapa, antes de chamar uma pessoa. */
    static final int MAX_TENTATIVAS_NA_ETAPA = 3;

    /** Primeiras palavras que valem como aceite do aviso: "sim", "pode continuar", "aceito"... */
    private static final Set<String> ACEITES =
            Set.of("sim", "s", "aceito", "pode", "ok", "claro", "concordo", "autorizo", "yes");

    private static final Set<String> RECUSAS = Set.of("nao", "n", "recuso", "no");

    private static final Set<String> SAUDACOES =
            Set.of("oi", "ola", "oie", "opa", "bom dia", "boa tarde", "boa noite", "ajuda", "preciso de ajuda");

    private static final Set<String> PEDIDOS_DE_MENU = Set.of("menu", "inicio", "voltar", "voltar ao inicio");

    private static final List<String> PEDIDOS_DE_ATENDENTE = List.of(
            "atendente", "humano", "falar com alguem", "falar com uma pessoa", "pessoa de verdade");

    private final String nome;
    private final String versao;
    private double limiteConfianca;
    private final List<ArtigoConhecimento> baseDeConhecimento = new ArrayList<>();
    private final Map<String, Fluxo> fluxos = new LinkedHashMap<>();
    private final Map<String, Fluxo> fluxosDoTino = fluxosDoTino();

    public Chatbot(String nome, String versao, double limiteConfianca) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do chatbot é obrigatório.");
        }
        if (versao == null || versao.isBlank()) {
            throw new IllegalArgumentException("A versão do chatbot é obrigatória.");
        }
        this.nome = nome.trim();
        this.versao = versao.trim();
        definirLimiteConfianca(limiteConfianca);
    }

    public final void definirLimiteConfianca(double limite) {
        if (!Double.isFinite(limite) || limite <= 0.0 || limite > 1.0) {
            throw new IllegalArgumentException("O limite de confiança deve ficar entre 0 e 1.");
        }
        this.limiteConfianca = limite;
    }

    /** So gestor altera a base. A autorizacao fica no dominio, nao so na tela. */
    public void publicarArtigo(ArtigoConhecimento artigo, Atendente responsavel) {
        if (artigo == null) {
            throw new IllegalArgumentException("O artigo é obrigatório.");
        }
        exigirGestor(responsavel);
        baseDeConhecimento.add(artigo);
    }

    /** Fluxos tambem sao conteudo da WXN: so gestor publica. Publicar de novo substitui. */
    public void publicarFluxo(Fluxo fluxo, Atendente responsavel) {
        if (fluxo == null) {
            throw new IllegalArgumentException("O fluxo é obrigatório.");
        }
        if (fluxo.getId().startsWith("_")) {
            throw new IllegalArgumentException("Fluxos que começam com _ são do próprio Tino.");
        }
        exigirGestor(responsavel);
        fluxos.put(fluxo.getId(), fluxo);
    }

    /**
     * Confere, depois de publicar tudo, que nenhum fluxo leva a um fluxo que nao
     * existe e que ha um menu. Melhor falhar na subida do que no meio de um atendimento.
     */
    public void validarFluxos() {
        if (fluxos.isEmpty()) {
            return;
        }
        if (!fluxos.containsKey(FLUXO_MENU)) {
            throw new IllegalStateException("Com fluxos publicados, é preciso publicar o fluxo \"menu\".");
        }
        List<String> problemas = new ArrayList<>();
        for (Fluxo fluxo : todosOsFluxos()) {
            for (String destino : fluxo.getFluxosDeDestino()) {
                if (buscarFluxo(destino).isEmpty()) {
                    problemas.add(fluxo.getId() + " -> " + destino);
                }
            }
        }
        if (!problemas.isEmpty()) {
            throw new IllegalStateException("Fluxos que levam a fluxos inexistentes: " + problemas);
        }
    }

    /**
     * Melhor artigo para a pergunta, respeitando o publico do cliente.
     *
     * A comparacao usa getConfianca() diretamente, e nao compareTo: a ordem
     * natural de RespostaCandidata e decrescente (melhor primeiro, para
     * ordenar listas), o que inverteria o sentido de um teste com > 0.
     */
    public Optional<RespostaCandidata> melhorResposta(String pergunta, TipoCliente publico) {
        RespostaCandidata melhor = null;
        for (ArtigoConhecimento artigo : baseDeConhecimento) {
            if (!artigo.disponivelPara(publico)) {
                continue;
            }
            double nota = artigo.pontuarPara(pergunta);
            if (nota <= 0.0) {
                continue;
            }
            if (melhor == null || nota > melhor.getConfianca()) {
                melhor = new RespostaCandidata(artigo, nota);
            }
        }
        return Optional.ofNullable(melhor);
    }

    /** O fluxo da WXN cujo assunto mais parece com a fala. O menu nao entra: ele nao tem assunto. */
    public Optional<Fluxo> melhorFluxo(String fala) {
        Fluxo melhor = null;
        double melhorNota = 0.0;
        for (Fluxo fluxo : fluxos.values()) {
            double nota = fluxo.pontuarPara(fala);
            if (nota > melhorNota) {
                melhor = fluxo;
                melhorNota = nota;
            }
        }
        return Optional.ofNullable(melhor);
    }

    /**
     * Fluxo completo de um turno de conversa: registra a fala do cliente,
     * cobra consentimento e, depois, responde, conduz ou escalona.
     *
     * @return a mensagem que o Tino enviou de volta
     */
    public Mensagem atender(Conversa conversa, String perguntaDoCliente) {
        if (conversa == null) {
            throw new IllegalArgumentException("A conversa é obrigatória.");
        }
        // A verificacao vem antes de gravar: recusar o turno nao pode deixar a
        // fala do cliente registrada sem resposta.
        if (!conversa.botPodeResponder()) {
            throw new IllegalStateException(
                    "O bot não responde nesta conversa: estado " + conversa.getEstado() + ".");
        }

        Cliente cliente = conversa.getCliente();
        boolean avisoJaEnviado = jaEnviouOAviso(conversa);
        conversa.registrarMensagem(cliente, perguntaDoCliente);

        if (!cliente.consentiu()) {
            return tratarConsentimento(conversa, perguntaDoCliente, avisoJaEnviado);
        }
        if (conversa.getEstado() == EstadoConversa.AGUARDANDO_CONSENTIMENTO) {
            // O cliente aceitou numa conversa anterior: esta nao precisa pedir de novo.
            conversa.liberarAposConsentimento();
        }

        return modoGuiado()
                ? conduzir(conversa, perguntaDoCliente)
                : responderOuEscalar(conversa, perguntaDoCliente);
    }

    /** As respostas prontas da etapa em que a conversa esta: viram botoes no canal. */
    public List<String> opcoesAtuais(Conversa conversa) {
        if (conversa == null || !conversa.botPodeResponder()) {
            return List.of();
        }
        if (conversa.getEstado() == EstadoConversa.AGUARDANDO_CONSENTIMENTO) {
            return jaEnviouOAviso(conversa) ? List.of("Sim", "Não") : List.of();
        }
        return conversa.getContexto()
                .flatMap(contexto -> buscarFluxo(contexto.getFluxo())
                        .flatMap(fluxo -> fluxo.getEtapa(contexto.getEtapa())))
                .map(Etapa::getOpcoes)
                .orElse(List.of());
    }

    /** Sem fluxos: a base responde ou o caso vai para uma pessoa. */
    private Mensagem responderOuEscalar(Conversa conversa, String pergunta) {
        Optional<RespostaCandidata> candidata =
                melhorResposta(pergunta, conversa.getCliente().getTipo());

        if (candidata.isPresent() && candidata.get().getConfianca() >= limiteConfianca) {
            conversa.reiniciarTentativas();
            return conversa.registrarMensagem(this, candidata.get().getArtigo().getResposta());
        }

        conversa.registrarTentativaSemSucesso();
        MotivoEscalonamento motivo = conversa.esgotouTentativas()
                ? MotivoEscalonamento.TENTATIVAS_ESGOTADAS
                : MotivoEscalonamento.BAIXA_CONFIANCA;
        conversa.escalonar(motivo);
        return conversa.registrarMensagem(this, TEXTO_FALLBACK);
    }

    // ---------- atendimento guiado ----------

    private boolean modoGuiado() {
        return fluxos.containsKey(FLUXO_MENU);
    }

    private Mensagem conduzir(Conversa conversa, String fala) {
        Optional<ContextoDoFluxo> contexto = conversa.getContexto();
        if (pedeAtendente(fala)) {
            Fluxo fluxo = contexto.flatMap(c -> buscarFluxo(c.getFluxo())).orElse(null);
            Map<String, String> dados = contexto.map(ContextoDoFluxo::getDados).orElse(Map.of());
            return passarParaAtendente(conversa, fluxo, dados, MotivoEscalonamento.SOLICITACAO_CLIENTE, "Claro!");
        }
        if (contexto.isEmpty()) {
            return reconhecerAssunto(conversa, fala);
        }
        if (ehUmDos(fala, PEDIDOS_DE_MENU)) {
            return iniciarFluxo(conversa, FLUXO_MENU, null, Map.of());
        }
        if (FLUXO_MENU.equals(contexto.get().getFluxo())) {
            // No menu, quem escreve a propria frase ("meu pedido chegou quebrado")
            // e entendido pela frase inteira antes de comparar com as opcoes.
            Optional<Mensagem> reconhecido = reconhecerComConfianca(conversa, fala);
            if (reconhecido.isPresent()) {
                return reconhecido.get();
            }
        }
        return continuar(conversa, contexto.get(), fala);
    }

    /**
     * Fora de um fluxo: uma pergunta da base vira resposta; um assunto conhecido
     * abre o fluxo dele; um "oi" abre o menu. O que nao se reconhece tambem abre
     * o menu, e so na terceira vez seguida o caso vai para uma pessoa.
     */
    private Mensagem reconhecerAssunto(Conversa conversa, String fala) {
        Optional<Mensagem> reconhecido = reconhecerComConfianca(conversa, fala);
        if (reconhecido.isPresent()) {
            return reconhecido.get();
        }
        if (ehUmDos(fala, SAUDACOES) || ehUmDos(fala, PEDIDOS_DE_MENU)) {
            return iniciarFluxo(conversa, FLUXO_MENU, TEXTO_SAUDACAO, Map.of());
        }
        conversa.registrarTentativaSemSucesso();
        if (conversa.esgotouTentativas()) {
            return passarParaAtendente(conversa, null, Map.of(), MotivoEscalonamento.TENTATIVAS_ESGOTADAS, null);
        }
        return iniciarFluxo(conversa, FLUXO_MENU, TEXTO_NAO_ENTENDI, Map.of());
    }

    /**
     * Uma pergunta da base acima do limite vira resposta (seguida de "isso
     * resolveu?"); um assunto acima do limite abre o fluxo dele. Empate fica
     * com a base: quem pergunta "qual o prazo para troca?" quer a resposta, nao
     * abrir uma troca.
     */
    private Optional<Mensagem> reconhecerComConfianca(Conversa conversa, String fala) {
        Optional<RespostaCandidata> artigo = melhorResposta(fala, conversa.getCliente().getTipo());
        double notaDoArtigo = artigo.map(RespostaCandidata::getConfianca).orElse(0.0);
        Optional<Fluxo> fluxo = melhorFluxo(fala);
        double notaDoFluxo = fluxo.map(f -> f.pontuarPara(fala)).orElse(0.0);

        if (notaDoArtigo >= limiteConfianca && notaDoArtigo >= notaDoFluxo) {
            conversa.reiniciarTentativas();
            return Optional.of(iniciarFluxo(conversa, FLUXO_CONFIRMACAO,
                    artigo.get().getArtigo().getResposta(), Map.of()));
        }
        if (notaDoFluxo >= limiteConfianca) {
            conversa.reiniciarTentativas();
            return Optional.of(iniciarFluxo(conversa, fluxo.get().getId(), null, Map.of()));
        }
        return Optional.empty();
    }

    private Mensagem continuar(Conversa conversa, ContextoDoFluxo contexto, String fala) {
        Optional<Fluxo> fluxo = buscarFluxo(contexto.getFluxo());
        Optional<Etapa> etapa = fluxo.flatMap(f -> f.getEtapa(contexto.getEtapa()));
        if (etapa.isEmpty()) {
            // O conteudo mudou desde a ultima mensagem: recomeca pelo menu.
            conversa.sairDoFluxo();
            return iniciarFluxo(conversa, FLUXO_MENU, TEXTO_NAO_ENTENDI, Map.of());
        }

        Map<String, String> dados = new LinkedHashMap<>(contexto.getDados());
        Transicao transicao = etapa.get().responder(fala, dados);
        if (transicao.getTipo() != Transicao.Tipo.REPETIR) {
            conversa.reiniciarTentativas();
            return aplicar(conversa, fluxo.get(), transicao, dados);
        }

        // A resposta nao serviu. Talvez o cliente tenha mudado de assunto.
        Optional<Fluxo> outroAssunto = melhorFluxo(fala)
                .filter(f -> !f.getId().equals(contexto.getFluxo()))
                .filter(f -> f.pontuarPara(fala) >= limiteConfianca);
        if (outroAssunto.isPresent()) {
            return iniciarFluxo(conversa, outroAssunto.get().getId(), null, contexto.getDados());
        }
        ContextoDoFluxo maisUma = contexto.comMaisUmaTentativa();
        if (maisUma.getTentativas() >= MAX_TENTATIVAS_NA_ETAPA) {
            return passarParaAtendente(conversa, fluxo.get(), contexto.getDados(),
                    MotivoEscalonamento.TENTATIVAS_ESGOTADAS, null);
        }
        conversa.seguirFluxo(maisUma);
        return conversa.registrarMensagem(this, montarTexto(transicao.getTexto(), etapa.get()));
    }

    private Mensagem aplicar(Conversa conversa, Fluxo fluxo, Transicao transicao, Map<String, String> dados) {
        return switch (transicao.getTipo()) {
            case IR_PARA -> irParaEtapa(conversa, fluxo, transicao.getAlvo(), transicao.getTexto(), dados);
            case INICIAR_FLUXO -> iniciarFluxo(conversa, transicao.getAlvo(), transicao.getTexto(), dados);
            case CONCLUIR -> iniciarFluxo(conversa, FLUXO_ENCERRAMENTO, transicao.getTexto(), Map.of());
            case ENCERRAR -> encerrar(conversa, transicao.getTexto());
            case PASSAR_PARA_ATENDENTE -> passarParaAtendente(conversa, fluxo, dados,
                    MotivoEscalonamento.SOLICITACAO_CLIENTE, transicao.getTexto());
            case REPETIR -> throw new IllegalStateException("Repetir é tratado antes de aplicar.");
        };
    }

    private Mensagem iniciarFluxo(Conversa conversa, String idDoFluxo, String textoAntes, Map<String, String> dados) {
        Fluxo fluxo = buscarFluxo(idDoFluxo).orElseThrow(
                () -> new IllegalStateException("Fluxo não publicado: " + idDoFluxo));
        return irParaEtapa(conversa, fluxo, fluxo.getEtapaInicial().getId(), textoAntes, dados);
    }

    private Mensagem irParaEtapa(Conversa conversa, Fluxo fluxo, String idDaEtapa, String textoAntes,
            Map<String, String> dados) {
        Etapa etapa = fluxo.getEtapa(idDaEtapa).orElseThrow(
                () -> new IllegalStateException("Etapa não encontrada: " + fluxo.getId() + "/" + idDaEtapa));
        Optional<Transicao> pulo = etapa.pularSeJaRespondida(dados);
        if (pulo.isPresent()) {
            return aplicar(conversa, fluxo, pulo.get().antecedidaPor(textoAntes), new LinkedHashMap<>(dados));
        }
        conversa.seguirFluxo(new ContextoDoFluxo(fluxo.getId(), etapa.getId(), dados, 0));
        return conversa.registrarMensagem(this, montarTexto(textoAntes, etapa));
    }

    /** Resolvido pelo Tino, sem ticket: a conversa entra na taxa de contencao. */
    private Mensagem encerrar(Conversa conversa, String despedida) {
        conversa.sairDoFluxo();
        Mensagem mensagem = conversa.registrarMensagem(this, despedida);
        conversa.encerrar();
        return mensagem;
    }

    /**
     * O caso vai para a fila com um resumo do que o Tino coletou, para o
     * atendente comecar de onde o bot parou. Sem fluxo, vai so com o historico.
     */
    private Mensagem passarParaAtendente(Conversa conversa, Fluxo fluxo, Map<String, String> dados,
            MotivoEscalonamento motivo, String textoAntes) {
        boolean comResumo = fluxo != null && !fluxo.getId().startsWith("_") && !FLUXO_MENU.equals(fluxo.getId());
        conversa.definirAssunto(comResumo ? fluxo.resumirEmUmaLinha(dados) : null);
        conversa.escalonar(motivo);

        List<String> partes = new ArrayList<>();
        if (motivo == MotivoEscalonamento.TENTATIVAS_ESGOTADAS) {
            partes.add(TEXTO_FALLBACK);
        } else {
            if (textoAntes != null && !textoAntes.isBlank()) {
                partes.add(textoAntes);
            }
            partes.add(comResumo ? TEXTO_PASSAGEM_COM_RESUMO : TEXTO_PASSAGEM);
        }
        if (comResumo) {
            partes.add("Resumo para o atendente:\n" + fluxo.resumir(dados));
        }
        return conversa.registrarMensagem(this, String.join("\n\n", partes));
    }

    /** A pergunta da etapa, com as opcoes numeradas: assim funciona ate num canal so de texto. */
    private static String montarTexto(String textoAntes, Etapa etapa) {
        StringBuilder texto = new StringBuilder();
        if (textoAntes != null && !textoAntes.isBlank()) {
            texto.append(textoAntes).append("\n\n");
        }
        texto.append(etapa.getPergunta());
        List<String> opcoes = etapa.getOpcoes();
        if (!opcoes.isEmpty()) {
            texto.append("\n");
            for (int i = 0; i < opcoes.size(); i++) {
                texto.append("\n").append(i + 1).append(". ").append(opcoes.get(i));
            }
        }
        return texto.toString();
    }

    private Optional<Fluxo> buscarFluxo(String id) {
        Fluxo fluxo = fluxos.get(id);
        return Optional.ofNullable(fluxo != null ? fluxo : fluxosDoTino.get(id));
    }

    private List<Fluxo> todosOsFluxos() {
        List<Fluxo> todos = new ArrayList<>(fluxos.values());
        todos.addAll(fluxosDoTino.values());
        return todos;
    }

    private static boolean pedeAtendente(String fala) {
        List<String> palavras = Linguagem.palavras(fala);
        return PEDIDOS_DE_ATENDENTE.stream().anyMatch(pedido -> Linguagem.posicaoDaFrase(palavras, pedido) >= 0);
    }

    private static boolean ehUmDos(String fala, Set<String> frases) {
        return frases.contains(Linguagem.normalizar(fala));
    }

    /**
     * Os dois fechamentos que todo atendimento guiado usa: "isso resolveu?",
     * depois de uma resposta da base, e "mais alguma coisa?", depois de um fluxo.
     */
    private static Map<String, Fluxo> fluxosDoTino() {
        Fluxo confirmacao = new Fluxo(FLUXO_CONFIRMACAO, "Confirmação", List.of(), List.of(), List.of(
                new EtapaDeEscolha("resolveu", "Isso resolveu a sua dúvida?", null, List.of(
                        new Opcao("Sim, resolveu", List.of("sim", "resolveu", "obrigado", "obrigada", "valeu"),
                                Transicao.encerrar(TEXTO_DESPEDIDA)),
                        new Opcao("Não, preciso de mais ajuda", List.of("nao", "ajuda", "preciso"),
                                Transicao.iniciarFluxo(FLUXO_MENU, "Vamos por outro caminho."))))),
                Map.of());
        Fluxo encerramento = new Fluxo(FLUXO_ENCERRAMENTO, "Encerramento", List.of(), List.of(), List.of(
                new EtapaDeEscolha("mais-algo", "Posso ajudar em mais alguma coisa?", null, List.of(
                        new Opcao("Sim, outro assunto", List.of("sim", "outro", "outra"),
                                Transicao.iniciarFluxo(FLUXO_MENU)),
                        new Opcao("Não, era só isso", List.of("nao", "obrigado", "obrigada", "valeu", "so isso"),
                                Transicao.encerrar(TEXTO_DESPEDIDA))))),
                Map.of());
        return Map.of(confirmacao.getId(), confirmacao, encerramento.getId(), encerramento);
    }

    private static void exigirGestor(Atendente responsavel) {
        if (responsavel == null || !responsavel.podeGerenciarBaseDeConhecimento()) {
            throw new IllegalStateException("Apenas um gestor pode alterar a base de conhecimento.");
        }
    }

    // ---------- consentimento ----------

    /**
     * Sem consentimento, o bot so trata do aviso de privacidade (LGPD): manda
     * o aviso e, depois dele, entende SIM como aceite e NAO como recusa.
     * Qualquer outra fala, ou um "sim" antes do aviso, recebe o aviso de novo:
     * ninguem consente sem ter lido o que esta aceitando.
     */
    private Mensagem tratarConsentimento(Conversa conversa, String fala, boolean avisoJaEnviado) {
        if (avisoJaEnviado) {
            String primeira = primeiraPalavra(fala);
            if (ACEITES.contains(primeira)) {
                conversa.getCliente().registrarConsentimento(VERSAO_DA_POLITICA);
                conversa.liberarAposConsentimento();
                if (modoGuiado()) {
                    return iniciarFluxo(conversa, FLUXO_MENU, TEXTO_ACEITE_GUIADO, Map.of());
                }
                return conversa.registrarMensagem(this, TEXTO_APOS_ACEITE);
            }
            if (RECUSAS.contains(primeira)) {
                return conversa.registrarMensagem(this, TEXTO_APOS_RECUSA);
            }
        }
        return conversa.registrarMensagem(this, AVISO_PRIVACIDADE);
    }

    private static boolean jaEnviouOAviso(Conversa conversa) {
        return conversa.getMensagens().stream()
                .anyMatch(m -> m.getRemetente() == TipoRemetente.CHATBOT
                        && AVISO_PRIVACIDADE.equals(m.getConteudo()));
    }

    /** "Sim, pode!" vira "sim"; "Não." vira "nao". */
    private static String primeiraPalavra(String texto) {
        if (texto == null) {
            return "";
        }
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .strip();
        int espaco = normalizado.indexOf(' ');
        return espaco == -1 ? normalizado : normalizado.substring(0, espaco);
    }

    public String getNome() {
        return nome;
    }

    public String getVersao() {
        return versao;
    }

    public double getLimiteConfianca() {
        return limiteConfianca;
    }

    public List<ArtigoConhecimento> getBaseDeConhecimento() {
        return Collections.unmodifiableList(baseDeConhecimento);
    }

    public List<Fluxo> getFluxos() {
        return List.copyOf(fluxos.values());
    }

    @Override
    public String getIdentificacao() {
        return nome + " v" + versao;
    }

    @Override
    public TipoRemetente getTipoRemetente() {
        return TipoRemetente.CHATBOT;
    }
}
