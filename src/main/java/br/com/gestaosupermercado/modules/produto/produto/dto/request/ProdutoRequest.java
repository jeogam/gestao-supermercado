package br.com.gestaosupermercado.modules.produto.produto.dto.request;

import br.com.gestaosupermercado.modules.produto.produto.enums.UnidadeMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Dados para cadastro e edição de produto (a quantidade em estoque é movimentada pelo módulo de estoque)")
public record ProdutoRequest(
        @Schema(description = "Nome do produto", example = "Arroz Tipo 1 5kg", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 255)
        String nome,

        @Schema(description = "Código de barras (EAN); opcional para itens a granel", example = "7891234567890")
        @Size(max = 50)
        String codigoBarras,

        @Schema(description = "Unidade de venda", example = "UN", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        UnidadeMedida unidadeMedida,

        @Schema(description = "Preço de custo", example = "18.50", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @PositiveOrZero
        @Digits(integer = 10, fraction = 2)
        BigDecimal precoCusto,

        @Schema(description = "Preço de venda", example = "24.90", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @PositiveOrZero
        @Digits(integer = 10, fraction = 2)
        BigDecimal precoVenda,

        @Schema(description = "Abaixo desta quantidade o produto entra no alerta de reposição", example = "10",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @PositiveOrZero
        @Digits(integer = 9, fraction = 3)
        BigDecimal estoqueMinimo
) {
}
