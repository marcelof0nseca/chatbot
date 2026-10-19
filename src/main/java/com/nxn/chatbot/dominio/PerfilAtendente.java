package com.nxn.chatbot.dominio;

/** O gestor acumula tudo o que o atendente pode fazer, e mais a base de conhecimento. */
public enum PerfilAtendente {

    ATENDENTE(false),
    GESTOR(true);

    private final boolean gerenciaBaseDeConhecimento;

    PerfilAtendente(boolean gerenciaBaseDeConhecimento) {
        this.gerenciaBaseDeConhecimento = gerenciaBaseDeConhecimento;
    }

    public boolean gerenciaBaseDeConhecimento() {
        return gerenciaBaseDeConhecimento;
    }
}
