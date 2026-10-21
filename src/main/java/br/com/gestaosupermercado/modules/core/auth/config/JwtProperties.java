package br.com.gestaosupermercado.modules.core.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração dos tokens. O segredo é validado no start: sem ele (ou curto demais para HS256) a aplicação
 * não sobe, em vez de emitir tokens fracos.
 */
@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        @NotBlank(message = "defina JWT_SECRET (mínimo de 32 caracteres)")
        @Size(min = 32, message = "JWT_SECRET deve ter no mínimo 32 caracteres")
        String secret,

        @NotBlank
        String issuer,

        @NotNull
        Duration accessTokenTtl,

        @NotNull
        Duration refreshTokenTtl
) {
}
