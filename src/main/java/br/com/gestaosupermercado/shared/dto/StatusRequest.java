package br.com.gestaosupermercado.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Alteração de status ativo/inativo de um recurso")
public record StatusRequest(
        @Schema(description = "true para ativar, false para inativar", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Boolean ativo
) {
}
