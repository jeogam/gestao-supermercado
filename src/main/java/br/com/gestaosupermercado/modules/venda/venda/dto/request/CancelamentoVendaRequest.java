package br.com.gestaosupermercado.modules.venda.venda.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Cancelamento (ou \"desfazer\") de uma venda")
public record CancelamentoVendaRequest(
        @Schema(description = "Motivo do cancelamento", example = "Cliente desistiu da compra")
        @Size(max = 255)
        String motivo
) {
}
