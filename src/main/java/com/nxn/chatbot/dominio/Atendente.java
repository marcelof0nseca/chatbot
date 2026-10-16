package com.nxn.chatbot.dominio;

public class Atendente extends Pessoa {

    private String email;
    private PerfilAtendente perfil;

    public Atendente(Long id, String nome, String email, PerfilAtendente perfil) {
        super(id, nome);
        this.email = validarEmail(email);
        this.perfil = validarPerfil(perfil);
    }

    @Override
    public String getIdentificacao() {
        return perfil + " " + getNome();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = validarEmail(email);
    }

    public PerfilAtendente getPerfil() {
        return perfil;
    }

    public void setPerfil(PerfilAtendente perfil) {
        this.perfil = validarPerfil(perfil);
    }

    private static PerfilAtendente validarPerfil(PerfilAtendente perfil) {
        if (perfil == null) {
            throw new IllegalArgumentException("O perfil do atendente é obrigatório.");
        }
        return perfil;
    }
}