package com.wxn.chatbot.dominio;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * O Tino. Tambem e um Remetente, porque assina as mensagens que envia.
 *
 * Aqui mora a regra central do projeto: abaixo do limite de confianca o bot
 * nao responde conteudo nenhum, assume que nao sabe e escalona. "Nunca
 * inventar" e codigo, nao recomendacao de comportamento.
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

    /** Primeiras palavras que valem como aceite do aviso: "sim", "pode continuar", "aceito"... */
    private static final Set<String> ACEITES =
            Set.of("sim", "s", "aceito", "pode", "ok", "claro", "concordo", "autorizo", "yes");

    private static final Set<String> RECUSAS = Set.of("nao", "n", "recuso", "no");

    private final String nome;
    private final String versao;
    private double limiteConfianca;
    private final List<ArtigoConhecimento> baseDeConhecimento = new ArrayList<>();

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
        if (responsavel == null || !responsavel.podeGerenciarBaseDeConhecimento()) {
            throw new IllegalStateException("Apenas um gestor pode alterar a base de conhecimento.");
        }
        baseDeConhecimento.add(artigo);
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

    /**
     * Fluxo completo de um turno de conversa: registra a fala do cliente,
     * cobra consentimento, busca na base e decide entre responder e escalonar.
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

        Optional<RespostaCandidata> candidata =
                melhorResposta(perguntaDoCliente, conversa.getCliente().getTipo());

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

    @Override
    public String getIdentificacao() {
        return nome + " v" + versao;
    }

    @Override
    public TipoRemetente getTipoRemetente() {
        return TipoRemetente.CHATBOT;
    }
}
