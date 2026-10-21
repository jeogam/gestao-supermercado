package br.com.gestaosupermercado.modules.core.perfil.dto.request;

import br.com.gestaosupermercado.modules.core.permissao.enums.Permissao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

@Schema(description = "Dados para cadastro ou edição de perfil")
public record PerfilRequest(
        @Schema(description = "Nome único do perfil", example = "Fiscal de Caixa", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 255)
        String nome,

        @Schema(description = "Descrição do perfil", example = "Supervisiona os operadores de caixa")
        @Size(max = 255)
        String descricao,

        @Schema(description = "Permissões concedidas pelo perfil (substitui as atuais)", example = "[\"USUARIO_LISTAR\"]",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Set<@NotNull Permissao> permissoes
) {
}
