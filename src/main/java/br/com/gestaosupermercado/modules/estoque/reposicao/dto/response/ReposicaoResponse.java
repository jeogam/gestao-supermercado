package br.com.gestaosupermercado.modules.estoque.reposicao.dto.response;

import br.com.gestaosupermercado.modules.produto.produto.enums.UnidadeMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Produto abaixo do estoque mínimo (alerta de reposição)")
public record ReposicaoResponse(
        @Schema(description = "Produto", example = "12")
        Long produtoId,

        @Schema(description = "Nome do produto", example = "Arroz Tipo 1 5kg")
        String nome,

        @Schema(description = "Código de barras", example = "7891234567890")
        String codigoBarras,

        @Schema(description = "Unidade de venda", example = "UN")
        UnidadeMedida unidadeMedida,

        @Schema(description = "Quantidade disponível", example = "4")
        BigDecimal quantidadeEstoque,

        @Schema(description = "Estoque mínimo", example = "10")
        BigDecimal estoqueMinimo,

        @Schema(description = "Quanto falta para chegar ao estoque mínimo", example = "6")
        BigDecimal quantidadeFaltante
) {
}
