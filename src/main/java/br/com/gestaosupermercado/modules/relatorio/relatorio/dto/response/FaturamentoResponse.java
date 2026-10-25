package br.com.gestaosupermercado.modules.relatorio.relatorio.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Faturamento, custo e margem das vendas concluídas no período")
public record FaturamentoResponse(
        @Schema(description = "Início do período")
        OffsetDateTime dataInicio,

        @Schema(description = "Fim do período")
        OffsetDateTime dataFim,

        @Schema(description = "Vendas concluídas", example = "120")
        long quantidadeVendas,

        @Schema(description = "Vendas canceladas (ficam fora dos valores)", example = "3")
        long quantidadeVendasCanceladas,

        @Schema(description = "Soma do total das vendas concluídas", example = "4820.50")
        BigDecimal faturamento,

        @Schema(description = "Custo das mercadorias vendidas, pelo custo do momento de cada venda", example = "3410.20")
        BigDecimal custo,

        @Schema(description = "Faturamento menos custo", example = "1410.30")
        BigDecimal lucro,

        @Schema(description = "Lucro sobre o faturamento, em %", example = "29.26")
        BigDecimal margemPercentual,

        @Schema(description = "Faturamento por forma de pagamento")
        List<FaturamentoFormaPagamentoResponse> formasPagamento
) {
}
