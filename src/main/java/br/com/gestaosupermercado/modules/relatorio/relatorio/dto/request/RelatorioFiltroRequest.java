package br.com.gestaosupermercado.modules.relatorio.relatorio.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "Período dos relatórios; só as vendas concluídas nele entram nos valores")
public record RelatorioFiltroRequest(
        @Schema(description = "A partir deste momento (inclusive)", example = "2026-10-01T00:00:00-03:00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime dataInicio,

        @Schema(description = "Até este momento (inclusive)", example = "2026-10-31T23:59:59-03:00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime dataFim
) {
}
