package com.nxn.chatbot.dominio;

public class Atendente extends Pessoa implements Remetente {

    private String email;
    private PerfilAtendente perfil;
    private boolean disponivel;

    public Atendente(Long id, String nome, String email, PerfilAtendente perfil) {
        super(id, nome);
        this.email = validarEmail(email);
        this.perfil = validarPerfil(perfil);
        this.disponivel = false;
    }

    @Override
    public String getIdentificacao() {
        return perfil + " " + getNome();
    }

    @Override
    public TipoRemetente getTipoRemetente() {
        return TipoRemetente.ATENDENTE;
    }

    public void entrarEmServico() {
        this.disponivel = true;
    }

    public void sairDeServico() {
        this.disponivel = false;
    }

    public boolean estaDisponivel() {
        return disponivel;
    }

    /** Regra de autorizacao consultada antes de qualquer alteracao na base. */
    public boolean podeGerenciarBaseDeConhecimento() {
        return perfil.gerenciaBaseDeConhecimento();
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
