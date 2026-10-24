package br.com.gestaosupermercado.modules.estoque.movimentacao.dto.request;

import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.OrigemMovimentacao;
import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.TipoMovimentacao;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "Filtros do histórico de movimentações de estoque")
public record MovimentacaoEstoqueFiltroRequest(
        @Schema(description = "Somente movimentações deste produto", example = "12")
        Long produtoId,

        @Schema(description = "Entrada ou saída", example = "SAIDA")
        TipoMovimentacao tipo,

        @Schema(description = "O que gerou a movimentação", example = "AJUSTE")
        OrigemMovimentacao origem,

        @Schema(description = "A partir deste momento (inclusive)", example = "2026-10-01T00:00:00-03:00")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime dataInicio,

        @Schema(description = "Até este momento (inclusive)", example = "2026-10-31T23:59:59-03:00")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime dataFim
) {
}
