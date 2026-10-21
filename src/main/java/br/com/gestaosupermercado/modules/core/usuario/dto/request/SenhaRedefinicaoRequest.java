package br.com.gestaosupermercado.modules.core.usuario.dto.request;

import br.com.gestaosupermercado.shared.validation.SenhaForte;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Redefinição da senha de outro usuário por quem tem permissão")
public record SenhaRedefinicaoRequest(
        @Schema(description = "Nova senha: 8 a 72 caracteres, com letra e número", example = "NovaSenha1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @SenhaForte
        String novaSenha
) {
}
