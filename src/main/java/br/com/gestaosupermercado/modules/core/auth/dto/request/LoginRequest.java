package br.com.gestaosupermercado.modules.core.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Credenciais de acesso")
public record LoginRequest(
        @Schema(description = "E-mail do usuário", example = "admin@mercado.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 255)
        String email,

        @Schema(description = "Senha", example = "SenhaForte1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 72)
        String senha
) {
}
