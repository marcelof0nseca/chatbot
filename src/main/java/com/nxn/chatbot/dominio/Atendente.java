package com.nxn.chatbot.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "atendente")
public class Atendente extends Pessoa implements Remetente {

    @Column(name = "email", nullable = false, unique = true, length = 160)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false, length = 10)
    private PerfilAtendente perfil;

    @Column(name = "disponivel", nullable = false)
    private boolean disponivel;

    /** Exigido pelo JPA; não usar diretamente. */
    protected Atendente() {
    }

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