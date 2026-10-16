package com.nxn.chatbot.dominio;

public class Cliente extends Pessoa {

    private static final int TAMANHO_MINIMO_TELEFONE = 10;

    private String telefone;
    private String email;
    private final TipoCliente tipo;
    private final Consentimento consentimento;

    public Cliente(Long id, String nome, String telefone, String email,
                   TipoCliente tipo, Consentimento consentimento) {
        super(id, nome);
        this.telefone = validarTelefone(telefone);
        this.email = validarEmail(email);
        if (tipo == null) {
            throw new IllegalArgumentException("O tipo de cliente é obrigatório.");
        }
        if (consentimento == null) {
            throw new IllegalArgumentException("O consentimento (LGPD) é obrigatório.");
        }
        this.tipo = tipo;
        this.consentimento = consentimento;
    }

    @Override
    public String getIdentificacao() {
        return "Cliente " + getNome() + " (" + tipo + ")";
    }

    public String getTelefoneMascarado() {
        int visiveis = 4;
        String ocultos = "*".repeat(telefone.length() - visiveis);
        return ocultos + telefone.substring(telefone.length() - visiveis);
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
        this.email = validarEmail(email);
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
}