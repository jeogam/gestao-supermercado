package br.com.gestaosupermercado.modules.core.auth.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.login")
public record LoginProperties(
        @Positive
        int maxTentativas,

        @NotNull
        Duration tempoBloqueio
) {
}
