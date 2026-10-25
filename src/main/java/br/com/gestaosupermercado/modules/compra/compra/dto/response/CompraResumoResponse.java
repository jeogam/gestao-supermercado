package br.com.gestaosupermercado.modules.compra.compra.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "Compra na listagem (sem os itens)")
public record CompraResumoResponse(
        @Schema(description = "Identificador", example = "48")
        Long id,

        @Schema(description = "Fornecedor", example = "4")
        Long fornecedorId,

        @Schema(description = "Nome do fornecedor", example = "Atacadão Irecê Ltda")
        String fornecedorNome,

        @Schema(description = "Número da nota fiscal", example = "000123456")
        String numeroNotaFiscal,

        @Schema(description = "Valor total", example = "444.00")
        BigDecimal valorTotal,

        @Schema(description = "Usuário que lançou a compra", example = "3")
        Long usuarioId,

        @Schema(description = "Data/hora do lançamento")
        OffsetDateTime criadoEm
) {
}
