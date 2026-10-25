package br.com.gestaosupermercado.modules.venda.venda.dto.request;

import br.com.gestaosupermercado.modules.venda.venda.enums.FormaPagamento;
import br.com.gestaosupermercado.modules.venda.venda.enums.StatusVenda;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "Filtros da listagem de vendas")
public record VendaFiltroRequest(
        @Schema(description = "Concluída ou cancelada", example = "CONCLUIDA")
        StatusVenda status,

        @Schema(description = "Forma de pagamento", example = "PIX")
        FormaPagamento formaPagamento,

        @Schema(description = "Somente vendas registradas por este usuário", example = "3")
        Long usuarioId,

        @Schema(description = "A partir deste momento (inclusive)", example = "2026-10-01T00:00:00-03:00")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime dataInicio,

        @Schema(description = "Até este momento (inclusive)", example = "2026-10-31T23:59:59-03:00")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime dataFim
) {
}
