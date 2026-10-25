package br.com.gestaosupermercado.modules.venda.venda.dto.request;

import br.com.gestaosupermercado.modules.venda.venda.enums.FormaPagamento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Registro de venda no caixa; o preço de cada item vem do cadastro do produto")
public record VendaRequest(
        @Schema(description = "Forma de pagamento", example = "DINHEIRO", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        FormaPagamento formaPagamento,

        @Schema(description = "Valor entregue pelo cliente (só no pagamento em dinheiro); usado para calcular o troco", example = "50.00")
        @PositiveOrZero
        @Digits(integer = 10, fraction = 2)
        BigDecimal valorRecebido,

        @Schema(description = "Itens vendidos", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty
        @Size(max = 200)
        List<@Valid @NotNull ItemVendaRequest> itens
) {
}
