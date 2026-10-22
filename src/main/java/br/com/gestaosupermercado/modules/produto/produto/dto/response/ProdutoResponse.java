package br.com.gestaosupermercado.modules.produto.produto.dto.response;

import br.com.gestaosupermercado.modules.produto.produto.enums.UnidadeMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "Produto")
public record ProdutoResponse(
        @Schema(description = "Identificador", example = "12")
        Long id,

        @Schema(description = "Nome do produto", example = "Arroz Tipo 1 5kg")
        String nome,

        @Schema(description = "Código de barras", example = "7891234567890")
        String codigoBarras,

        @Schema(description = "Unidade de venda", example = "UN")
        UnidadeMedida unidadeMedida,

        @Schema(description = "Preço de custo; null para quem não tem PRODUTO_GERENCIAR", example = "18.50")
        BigDecimal precoCusto,

        @Schema(description = "Preço de venda", example = "24.90")
        BigDecimal precoVenda,

        @Schema(description = "Quantidade disponível em estoque", example = "42")
        BigDecimal quantidadeEstoque,

        @Schema(description = "Estoque mínimo antes do alerta de reposição", example = "10")
        BigDecimal estoqueMinimo,

        @Schema(description = "Se está abaixo do estoque mínimo", example = "false")
        boolean abaixoEstoqueMinimo,

        @Schema(description = "Se o produto pode ser vendido", example = "true")
        boolean ativo,

        @Schema(description = "Data/hora de cadastro")
        OffsetDateTime criadoEm
) {
}
