package br.com.gestaosupermercado.modules.core.usuario.dto.response;

import br.com.gestaosupermercado.modules.core.perfil.dto.response.PerfilResumoResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Usuário (nunca expõe hash de senha nem dados de bloqueio)")
public record UsuarioResponse(
        @Schema(description = "Identificador", example = "7")
        Long id,

        @Schema(description = "Nome completo", example = "Maria Souza")
        String nome,

        @Schema(description = "E-mail de login", example = "maria@mercado.com")
        String email,

        @Schema(description = "Se o usuário pode acessar o sistema", example = "true")
        boolean ativo,

        @Schema(description = "Data/hora do último login bem-sucedido")
        OffsetDateTime ultimoLoginEm,

        @Schema(description = "Data/hora de cadastro")
        OffsetDateTime criadoEm,

        @Schema(description = "Perfis (cargos) vinculados")
        List<PerfilResumoResponse> perfis
) {
}
