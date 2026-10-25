package br.com.gestaosupermercado.modules.compra.compra.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Item de uma compra")
public record ItemCompraResponse(
        @Schema(description = "Produto", example = "12")
        Long produtoId,

        @Schema(description = "Nome do produto", example = "Arroz Tipo 1 5kg")
        String produtoNome,

        @Schema(description = "Quantidade recebida", example = "24")
        BigDecimal quantidade,

        @Schema(description = "Custo por unidade", example = "18.50")
        BigDecimal precoCustoUnitario,

        @Schema(description = "Quantidade x custo unitário", example = "444.00")
        BigDecimal subtotal
) {
}
