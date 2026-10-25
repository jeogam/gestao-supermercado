package br.com.gestaosupermercado.modules.venda.venda.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Schema(description = "Item de uma venda")
public record ItemVendaRequest(
        @Schema(description = "Produto vendido", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Long produtoId,

        @Schema(description = "Quantidade; inteira para produtos vendidos por unidade", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Positive
        @Digits(integer = 9, fraction = 3)
        BigDecimal quantidade
) {
}
