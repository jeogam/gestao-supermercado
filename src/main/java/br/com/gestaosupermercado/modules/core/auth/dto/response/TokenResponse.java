package br.com.gestaosupermercado.modules.core.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Par de tokens da sessão")
public record TokenResponse(
        @Schema(description = "JWT para o header Authorization: Bearer")
        String accessToken,

        @Schema(description = "Tipo do token", example = "Bearer")
        String tokenType,

        @Schema(description = "Validade do access token em segundos", example = "900")
        long expiresIn,

        @Schema(description = "Token opaco para renovar a sessão; de uso único")
        String refreshToken
) {
}
