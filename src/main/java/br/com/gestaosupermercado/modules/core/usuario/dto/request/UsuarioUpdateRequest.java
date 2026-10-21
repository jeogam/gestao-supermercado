package br.com.gestaosupermercado.modules.core.usuario.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Set;

@Schema(description = "Dados para edição de usuário (a senha tem endpoint próprio)")
public record UsuarioUpdateRequest(
        @Schema(description = "Nome completo", example = "Maria Souza", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 255)
        String nome,

        @Schema(description = "E-mail usado no login", example = "maria@mercado.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @Schema(description = "Perfis (cargos) do usuário; substitui os atuais", example = "[2, 3]",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty
        Set<@NotNull @Positive Long> perfilIds
) {
}
