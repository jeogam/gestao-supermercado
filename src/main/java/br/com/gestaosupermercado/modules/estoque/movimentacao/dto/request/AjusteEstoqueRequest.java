package br.com.gestaosupermercado.modules.estoque.movimentacao.dto.request;

import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.TipoMovimentacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Ajuste manual de estoque (perda, avaria, vencimento, contagem de inventário...)")
public record AjusteEstoqueRequest(
        @Schema(description = "Produto ajustado", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Long produtoId,

        @Schema(description = "ENTRADA soma ao estoque; SAIDA subtrai", example = "SAIDA", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        TipoMovimentacao tipo,

        @Schema(description = "Quantidade movimentada; inteira para produtos vendidos por unidade", example = "3",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Positive
        @Digits(integer = 9, fraction = 3)
        BigDecimal quantidade,

        @Schema(description = "Motivo do ajuste", example = "Embalagens avariadas no recebimento", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 255)
        String motivo
) {
}
