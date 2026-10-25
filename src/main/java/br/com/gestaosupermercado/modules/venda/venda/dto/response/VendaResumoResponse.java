package br.com.gestaosupermercado.modules.venda.venda.dto.response;

import br.com.gestaosupermercado.modules.venda.venda.enums.FormaPagamento;
import br.com.gestaosupermercado.modules.venda.venda.enums.StatusVenda;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "Venda na listagem (sem os itens)")
public record VendaResumoResponse(
        @Schema(description = "Identificador", example = "305")
        Long id,

        @Schema(description = "Concluída ou cancelada", example = "CONCLUIDA")
        StatusVenda status,

        @Schema(description = "Forma de pagamento", example = "PIX")
        FormaPagamento formaPagamento,

        @Schema(description = "Valor total", example = "47.80")
        BigDecimal valorTotal,

        @Schema(description = "Usuário que registrou a venda", example = "3")
        Long usuarioId,

        @Schema(description = "Data/hora da venda")
        OffsetDateTime criadoEm
) {
}
