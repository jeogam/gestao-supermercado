package br.com.gestaosupermercado.modules.relatorio.relatorio.dto.response;

import br.com.gestaosupermercado.modules.venda.venda.enums.FormaPagamento;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Faturamento de uma forma de pagamento no período")
public record FaturamentoFormaPagamentoResponse(
        @Schema(description = "Forma de pagamento", example = "PIX")
        FormaPagamento formaPagamento,

        @Schema(description = "Vendas concluídas", example = "42")
        long quantidadeVendas,

        @Schema(description = "Soma do total das vendas", example = "1530.40")
        BigDecimal faturamento
) {
}
