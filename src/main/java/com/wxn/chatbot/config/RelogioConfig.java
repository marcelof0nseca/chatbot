package com.wxn.chatbot.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.wxn.chatbot.dominio.Relogio;

/**
 * Relogio da aplicacao, com a zona fixa em app.zona. O mesmo Clock e injetado
 * nos servicos e entregue ao dominio, para que tudo use o mesmo "agora".
 */
@Configuration
public class RelogioConfig {

    @Bean
    public Clock relogio(@Value("${app.zona}") String zona) {
        Clock clock = Clock.system(ZoneId.of(zona));
        Relogio.usar(clock);
        return clock;
    }
}
