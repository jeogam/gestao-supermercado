package br.com.gestaosupermercado.modules.core.usuario.dto.response;

import br.com.gestaosupermercado.modules.core.perfil.dto.response.PerfilResumoResponse;
import br.com.gestaosupermercado.modules.core.permissao.enums.Permissao;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Dados do usuário autenticado, usados pelo front para montar menus e telas")
public record UsuarioLogadoResponse(
        @Schema(description = "Identificador", example = "7")
        Long id,

        @Schema(description = "Nome completo", example = "Maria Souza")
        String nome,

        @Schema(description = "E-mail de login", example = "maria@mercado.com")
        String email,

        @Schema(description = "Perfis (cargos) vinculados")
        List<PerfilResumoResponse> perfis,

        @Schema(description = "Permissões efetivas (somente de perfis ativos)")
        List<Permissao> permissoes
) {
}
