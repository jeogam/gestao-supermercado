package br.com.gestaosupermercado.modules.core.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Refresh token recebido no login ou na última renovação")
public record RefreshTokenRequest(
        @Schema(description = "Refresh token opaco", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 255)
        String refreshToken
) {
}
