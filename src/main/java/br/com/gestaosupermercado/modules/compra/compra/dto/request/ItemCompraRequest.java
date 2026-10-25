package br.com.gestaosupermercado.modules.compra.compra.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

@Schema(description = "Item de uma compra")
public record ItemCompraRequest(
        @Schema(description = "Produto comprado", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Long produtoId,

        @Schema(description = "Quantidade recebida; inteira para produtos vendidos por unidade", example = "24",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Positive
        @Digits(integer = 9, fraction = 3)
        BigDecimal quantidade,

        @Schema(description = "Custo pago por unidade (ou por KG/L); vira o novo preço de custo do produto", example = "18.50",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @PositiveOrZero
        @Digits(integer = 10, fraction = 2)
        BigDecimal precoCustoUnitario
) {
}
