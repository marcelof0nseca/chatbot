package com.wxn.chatbot.dominio;

import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Grava o ContextoDoFluxo como JSON na coluna conversa.contexto. Em
 * desenvolvimento e um VARCHAR no H2; em producao vira JSONB no PostgreSQL.
 *
 * Um contexto que nao se le mais (gravado por uma versao antiga, por exemplo)
 * volta nulo: a conversa recomeca pelo menu, sem derrubar o atendimento. O
 * historico, que fica na tabela mensagem, nao e afetado.
 */
@Converter
public class ContextoDoFluxoConverter implements AttributeConverter<ContextoDoFluxo, String> {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Override
    public String convertToDatabaseColumn(ContextoDoFluxo contexto) {
        return contexto == null ? null : JSON.writeValueAsString(contexto.paraMapa());
    }

    @Override
    public ContextoDoFluxo convertToEntityAttribute(String coluna) {
        if (coluna == null || coluna.isBlank()) {
            return null;
        }
        try {
            return ContextoDoFluxo.deMapa(JSON.readValue(coluna, Map.class));
        } catch (JacksonException | IllegalArgumentException | ClassCastException e) {
            return null;
        }
    }
}
