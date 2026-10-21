package br.com.gestaosupermercado.shared.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio injetável para que regras dependentes de tempo (bloqueio de login, expiração de token)
 * sejam testáveis sem esperas reais.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
