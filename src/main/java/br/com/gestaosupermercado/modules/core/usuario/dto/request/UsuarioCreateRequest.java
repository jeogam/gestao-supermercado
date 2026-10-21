package br.com.gestaosupermercado.modules.core.usuario.dto.request;

import br.com.gestaosupermercado.shared.validation.SenhaForte;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Set;

@Schema(description = "Dados para cadastro de usuário")
public record UsuarioCreateRequest(
        @Schema(description = "Nome completo", example = "Maria Souza", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 255)
        String nome,

        @Schema(description = "E-mail usado no login", example = "maria@mercado.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @Schema(description = "Senha inicial: 8 a 72 caracteres, com letra e número", example = "Troque123",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @SenhaForte
        String senha,

        @Schema(description = "Perfis (cargos) do usuário", example = "[3]", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty
        Set<@NotNull @Positive Long> perfilIds
) {
}
