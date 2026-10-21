package br.com.gestaosupermercado.modules.core.usuario.dto.request;

import br.com.gestaosupermercado.shared.validation.SenhaForte;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Troca da própria senha")
public record SenhaAlteracaoRequest(
        @Schema(description = "Senha atual", example = "SenhaAntiga1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 72)
        String senhaAtual,

        @Schema(description = "Nova senha: 8 a 72 caracteres, com letra e número", example = "NovaSenha1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @SenhaForte
        String novaSenha
) {
}
