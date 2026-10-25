package br.com.gestaosupermercado.modules.compra.compra.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "Filtros da listagem de compras")
public record CompraFiltroRequest(
        @Schema(description = "Somente compras deste fornecedor", example = "4")
        Long fornecedorId,

        @Schema(description = "Número exato da nota fiscal", example = "000123456")
        @Size(max = 50)
        String numeroNotaFiscal,

        @Schema(description = "A partir deste momento (inclusive)", example = "2026-10-01T00:00:00-03:00")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime dataInicio,

        @Schema(description = "Até este momento (inclusive)", example = "2026-10-31T23:59:59-03:00")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime dataFim
) {
}
