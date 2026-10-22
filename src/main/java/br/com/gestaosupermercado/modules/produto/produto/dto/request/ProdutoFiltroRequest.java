package br.com.gestaosupermercado.modules.produto.produto.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Filtros da listagem de produtos")
public record ProdutoFiltroRequest(
        @Schema(description = "Parte do nome", example = "arroz")
        @Size(max = 255)
        String nome,

        @Schema(description = "Código de barras exato", example = "7891234567890")
        @Size(max = 50)
        String codigoBarras,

        @Schema(description = "Status do produto", example = "true")
        Boolean ativo,

        @Schema(description = "Somente produtos abaixo do estoque mínimo (alerta de reposição)", example = "true")
        Boolean abaixoEstoqueMinimo
) {
}
