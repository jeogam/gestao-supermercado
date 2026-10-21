package br.com.gestaosupermercado.modules.core.perfil.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo de perfil vinculado a um usuário")
public record PerfilResumoResponse(
        @Schema(description = "Identificador", example = "2")
        Long id,

        @Schema(description = "Nome", example = "Gerente")
        String nome,

        @Schema(description = "Se o perfil está ativo (inativo não concede permissões)", example = "true")
        boolean ativo
) {
}
