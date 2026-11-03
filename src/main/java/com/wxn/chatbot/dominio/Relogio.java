package com.wxn.chatbot.dominio;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Fonte unica de "agora" para o dominio, sempre no fuso de Recife.
 *
 * Servidor em nuvem roda em UTC: sem fixar a zona, o dia viraria as 21h do
 * horario local. As entidades sao criadas pelo Hibernate e nao recebem injecao
 * de dependencia, entao o relogio do dominio e configurado uma unica vez na
 * inicializacao (RelogioConfig) com o mesmo Clock injetado nos servicos.
 *
 * Nos testes, usar(Clock.fixed(...)) torna os carimbos de tempo previsiveis.
 */
public final class Relogio {

    public static final ZoneId ZONA_PADRAO = ZoneId.of("America/Recife");

    private static volatile Clock clock = Clock.system(ZONA_PADRAO);

    private Relogio() {
    }

    public static LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }

    public static LocalDate hoje() {
        return LocalDate.now(clock);
    }

    public static void usar(Clock novo) {
        clock = Objects.requireNonNull(novo, "O relógio é obrigatório.");
    }

    public static void restaurarPadrao() {
        clock = Clock.system(ZONA_PADRAO);
    }
}
