package br.com.gestaosupermercado.modules.compra.fornecedor.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Filtros da listagem de fornecedores")
public record FornecedorFiltroRequest(
        @Schema(description = "Parte do nome", example = "atacad")
        @Size(max = 255)
        String nome,

        @Schema(description = "CNPJ exato, com ou sem pontuação", example = "12.345.678/0001-90")
        @Size(max = 18)
        String cnpj,

        @Schema(description = "Status do fornecedor", example = "true")
        Boolean ativo
) {
}
