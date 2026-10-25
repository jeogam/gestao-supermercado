package br.com.gestaosupermercado.modules.core.auth.dto.request;

import br.com.gestaosupermercado.shared.validation.SenhaForte;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para criar a própria conta pela tela pública; a conta nasce sem perfil")
public record CadastroRequest(
        @Schema(description = "Nome completo", example = "Marcos Oliveira", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 255)
        String nome,

        @Schema(description = "E-mail usado no login", example = "marcos@mercado.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @Schema(description = "Senha: 8 a 72 caracteres, com letra e número", example = "Mercado123",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @SenhaForte
        String senha
) {
}
