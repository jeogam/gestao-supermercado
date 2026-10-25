package br.com.gestaosupermercado.modules.relatorio.relatorio.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Margem de um produto nas vendas concluídas do período")
public record MargemProdutoResponse(
        @Schema(description = "Produto", example = "12")
        Long produtoId,

        @Schema(description = "Nome do produto", example = "Arroz Tipo 1 5kg")
        String nome,

        @Schema(description = "Quantidade vendida", example = "35")
        BigDecimal quantidadeVendida,

        @Schema(description = "Soma dos subtotais vendidos", example = "836.50")
        BigDecimal faturamento,

        @Schema(description = "Custo do que foi vendido, pelo custo do momento de cada venda", example = "630.00")
        BigDecimal custo,

        @Schema(description = "Faturamento menos custo", example = "206.50")
        BigDecimal lucro,

        @Schema(description = "Lucro sobre o faturamento, em %", example = "24.69")
        BigDecimal margemPercentual
) {
}
