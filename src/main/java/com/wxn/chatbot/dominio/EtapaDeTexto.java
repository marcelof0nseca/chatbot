package com.wxn.chatbot.dominio;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Uma pergunta aberta: o numero do pedido, a descricao do problema. Com um
 * formato, so aceita a resposta que o segue e guarda so a parte que importa
 * ("é o pedido 123456!" guarda "123456").
 */
public class EtapaDeTexto extends Etapa {

    /** Respostas abertas sao cortadas aqui, para o resumo caber na fila. */
    static final int TAMANHO_MAXIMO = 300;

    private final String chave;
    private final Pattern formato;
    private final String ajuda;
    private final Transicao proxima;

    /**
     * @param formato a resposta aceita; o primeiro grupo, se houver, e o que se
     *                guarda. Nulo aceita qualquer texto.
     * @param ajuda   o que o Tino diz quando a resposta nao segue o formato
     */
    public EtapaDeTexto(String id, String pergunta, String chave, Pattern formato, String ajuda, Transicao proxima) {
        super(id, pergunta);
        if (chave == null || chave.isBlank()) {
            throw new IllegalArgumentException("A etapa de texto precisa de uma chave para guardar a resposta.");
        }
        if (formato != null && (ajuda == null || ajuda.isBlank())) {
            throw new IllegalArgumentException("Com formato, a etapa precisa de um texto de ajuda.");
        }
        if (proxima == null || proxima.getTipo() == Transicao.Tipo.REPETIR) {
            throw new IllegalArgumentException("A etapa de texto precisa de um próximo passo.");
        }
        this.chave = chave;
        this.formato = formato;
        this.ajuda = ajuda;
        this.proxima = proxima;
    }

    @Override
    public Transicao responder(String fala, Map<String, String> dados) {
        String texto = fala == null ? "" : fala.strip();
        if (texto.isEmpty()) {
            return Transicao.repetir(ajuda != null ? ajuda : "Pode escrever a sua resposta?");
        }
        String valor = texto;
        if (formato != null) {
            Matcher achado = formato.matcher(texto);
            if (!achado.find()) {
                return Transicao.repetir(ajuda);
            }
            valor = achado.groupCount() >= 1 ? achado.group(1) : achado.group();
        }
        if (valor.length() > TAMANHO_MAXIMO) {
            valor = valor.substring(0, TAMANHO_MAXIMO - 1).strip() + "…";
        }
        dados.put(chave, valor);
        return proxima;
    }

    @Override
    public Optional<Transicao> pularSeJaRespondida(Map<String, String> dados) {
        return dados.containsKey(chave) ? Optional.of(proxima) : Optional.empty();
    }

    @Override
    public List<Transicao> getDestinos() {
        return List.of(proxima);
    }

    public String getChave() {
        return chave;
    }
}
