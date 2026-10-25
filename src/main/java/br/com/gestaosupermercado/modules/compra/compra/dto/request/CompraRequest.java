package br.com.gestaosupermercado.modules.compra.compra.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Lançamento de compra de fornecedor; cada item dá entrada no estoque")
public record CompraRequest(
        @Schema(description = "Fornecedor da compra", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Long fornecedorId,

        @Schema(description = "Número da nota fiscal", example = "000123456")
        @Size(max = 50)
        String numeroNotaFiscal,

        @Schema(description = "Observação livre", example = "Entrega parcial; restante na próxima semana")
        @Size(max = 255)
        String observacao,

        @Schema(description = "Itens comprados", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty
        @Size(max = 200)
        List<@Valid @NotNull ItemCompraRequest> itens
) {
}
