package br.com.gestaosupermercado.modules.core.permissao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Permissão do catálogo")
public record PermissaoResponse(
        @Schema(description = "Código usado como authority", example = "USUARIO_LISTAR")
        String codigo,

        @Schema(description = "Descrição legível", example = "Listar e consultar usuários")
        String descricao,

        @Schema(description = "Agrupamento para exibição na tela de perfis", example = "Usuários")
        String grupo
) {
}
