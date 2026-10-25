package br.com.gestaosupermercado.modules.compra.compra.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Compra com seus itens")
public record CompraResponse(
        @Schema(description = "Identificador", example = "48")
        Long id,

        @Schema(description = "Fornecedor", example = "4")
        Long fornecedorId,

        @Schema(description = "Nome do fornecedor", example = "Atacadão Irecê Ltda")
        String fornecedorNome,

        @Schema(description = "Número da nota fiscal", example = "000123456")
        String numeroNotaFiscal,

        @Schema(description = "Soma dos subtotais dos itens", example = "444.00")
        BigDecimal valorTotal,

        @Schema(description = "Observação livre")
        String observacao,

        @Schema(description = "Usuário que lançou a compra", example = "3")
        Long usuarioId,

        @Schema(description = "Data/hora do lançamento")
        OffsetDateTime criadoEm,

        @Schema(description = "Itens da compra")
        List<ItemCompraResponse> itens
) {
}
