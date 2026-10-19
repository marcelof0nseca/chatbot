package com.nxn.chatbot.dominio;

public class Cliente extends Pessoa implements Remetente {

    private static final int TAMANHO_MINIMO_TELEFONE = 10;

    private String telefone;
    private String email;
    private final TipoCliente tipo;
    private Consentimento consentimento;

    /** Cliente que chega pelo WhatsApp: so temos o telefone, e ainda nao consentiu. */
    public Cliente(Long id, String nome, String telefone, TipoCliente tipo) {
        this(id, nome, telefone, null, tipo, null);
    }

    public Cliente(Long id, String nome, String telefone, String email,
                   TipoCliente tipo, Consentimento consentimento) {
        super(id, nome);
        this.telefone = validarTelefone(telefone);
        this.email = validarEmailOpcional(email);
        if (tipo == null) {
            throw new IllegalArgumentException("O tipo de cliente é obrigatório.");
        }
        this.tipo = tipo;
        this.consentimento = consentimento;
    }

    @Override
    public String getIdentificacao() {
        return getTelefoneMascarado();
    }

    @Override
    public TipoRemetente getTipoRemetente() {
        return TipoRemetente.CLIENTE;
    }

    public String getTelefoneMascarado() {
        int visiveis = 4;
        String ocultos = "*".repeat(telefone.length() - visiveis);
        return ocultos + telefone.substring(telefone.length() - visiveis);
    }

    /** O aceite do aviso de privacidade (LGPD) acontece na primeira conversa. */
    public void registrarConsentimento(String versaoPolitica) {
        if (consentimento != null) {
            throw new IllegalStateException("Consentimento já registrado para este cliente.");
        }
        this.consentimento = new Consentimento(versaoPolitica);
    }

    /** Direito do titular: o aceite pode ser retirado a qualquer momento. */
    public void revogarConsentimento() {
        this.consentimento = null;
    }

    public boolean consentiu() {
        return consentimento != null;
    }

    public boolean ehInterno() {
        return tipo == TipoCliente.INTERNO;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = validarTelefone(telefone);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = validarEmailOpcional(email);
    }

    public TipoCliente getTipo() {
        return tipo;
    }

    public Consentimento getConsentimento() {
        return consentimento;
    }

    private static String validarTelefone(String telefone) {
        if (telefone == null) {
            throw new IllegalArgumentException("O telefone é obrigatório.");
        }
        String digitos = telefone.replaceAll("\\D", "");
        if (digitos.length() < TAMANHO_MINIMO_TELEFONE) {
            throw new IllegalArgumentException(
                    "Telefone muito curto: informe DDD + número (mínimo " + TAMANHO_MINIMO_TELEFONE + " dígitos).");
        }
        return digitos;
    }

    /** No WhatsApp o e-mail nao e informado; quando vier, continua validado. */
    private static String validarEmailOpcional(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return validarEmail(email);
    }
}
