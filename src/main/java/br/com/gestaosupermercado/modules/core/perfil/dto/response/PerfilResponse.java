package br.com.gestaosupermercado.modules.core.perfil.dto.response;

import br.com.gestaosupermercado.modules.core.permissao.enums.Permissao;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Perfil de acesso")
public record PerfilResponse(
        @Schema(description = "Identificador", example = "2")
        Long id,

        @Schema(description = "Nome", example = "Gerente")
        String nome,

        @Schema(description = "Descrição", example = "Gerência da loja")
        String descricao,

        @Schema(description = "Se o perfil está ativo", example = "true")
        boolean ativo,

        @Schema(description = "Perfil do sistema: não pode ser editado nem inativado", example = "false")
        boolean sistema,

        @Schema(description = "Permissões concedidas, agrupadas por área do sistema")
        List<Permissao> permissoes
) {
}
