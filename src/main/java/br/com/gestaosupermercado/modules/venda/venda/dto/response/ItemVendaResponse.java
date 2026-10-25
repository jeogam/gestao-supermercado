package br.com.gestaosupermercado.modules.venda.venda.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * O custo do item não aparece aqui: com custo e preço o Caixa calcularia a margem, que a ADR-001 reserva aos relatórios.
 */
@Schema(description = "Item de uma venda")
public record ItemVendaResponse(
        @Schema(description = "Produto", example = "12")
        Long produtoId,

        @Schema(description = "Nome do produto", example = "Arroz Tipo 1 5kg")
        String produtoNome,

        @Schema(description = "Quantidade", example = "2")
        BigDecimal quantidade,

        @Schema(description = "Preço unitário no momento da venda", example = "23.90")
        BigDecimal precoUnitario,

        @Schema(description = "Quantidade x preço unitário", example = "47.80")
        BigDecimal subtotal
) {
}
