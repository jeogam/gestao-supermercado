package br.com.gestaosupermercado.modules.venda.venda.dto.response;

import br.com.gestaosupermercado.modules.venda.venda.enums.FormaPagamento;
import br.com.gestaosupermercado.modules.venda.venda.enums.StatusVenda;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Venda com seus itens")
public record VendaResponse(
        @Schema(description = "Identificador", example = "305")
        Long id,

        @Schema(description = "Concluída ou cancelada", example = "CONCLUIDA")
        StatusVenda status,

        @Schema(description = "Forma de pagamento", example = "DINHEIRO")
        FormaPagamento formaPagamento,

        @Schema(description = "Soma dos subtotais dos itens", example = "47.80")
        BigDecimal valorTotal,

        @Schema(description = "Valor entregue pelo cliente (dinheiro)", example = "50.00")
        BigDecimal valorRecebido,

        @Schema(description = "Troco (dinheiro)", example = "2.20")
        BigDecimal troco,

        @Schema(description = "Usuário que registrou a venda", example = "3")
        Long usuarioId,

        @Schema(description = "Data/hora da venda")
        OffsetDateTime criadoEm,

        @Schema(description = "Data/hora do cancelamento")
        OffsetDateTime canceladoEm,

        @Schema(description = "Usuário que cancelou", example = "3")
        Long canceladoPorUsuarioId,

        @Schema(description = "Motivo do cancelamento", example = "Cliente desistiu da compra")
        String motivoCancelamento,

        @Schema(description = "Itens da venda")
        List<ItemVendaResponse> itens
) {
}
