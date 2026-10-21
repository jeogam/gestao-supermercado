package br.com.gestaosupermercado.modules.core.usuario.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Filtros da listagem de usuários")
public record UsuarioFiltroRequest(
        @Schema(description = "Parte do nome", example = "maria")
        @Size(max = 255)
        String nome,

        @Schema(description = "Parte do e-mail", example = "@mercado.com")
        @Size(max = 255)
        String email,

        @Schema(description = "Status do usuário", example = "true")
        Boolean ativo,

        @Schema(description = "Somente usuários com este perfil", example = "3")
        @Positive
        Long perfilId
) {
}
