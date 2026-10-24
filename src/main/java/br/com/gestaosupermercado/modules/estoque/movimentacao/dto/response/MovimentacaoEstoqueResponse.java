package br.com.gestaosupermercado.modules.estoque.movimentacao.dto.response;

import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.OrigemMovimentacao;
import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.TipoMovimentacao;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "Movimentação de estoque")
public record MovimentacaoEstoqueResponse(
        @Schema(description = "Identificador", example = "105")
        Long id,

        @Schema(description = "Produto movimentado", example = "12")
        Long produtoId,

        @Schema(description = "Nome do produto", example = "Arroz Tipo 1 5kg")
        String produtoNome,

        @Schema(description = "Entrada ou saída", example = "SAIDA")
        TipoMovimentacao tipo,

        @Schema(description = "O que gerou a movimentação", example = "AJUSTE")
        OrigemMovimentacao origem,

        @Schema(description = "Quantidade movimentada", example = "3")
        BigDecimal quantidade,

        @Schema(description = "Estoque do produto logo após a movimentação", example = "39")
        BigDecimal saldoApos,

        @Schema(description = "Motivo (obrigatório no ajuste manual)", example = "Embalagens avariadas no recebimento")
        String motivo,

        @Schema(description = "Id da compra ou venda que gerou a movimentação", example = "48")
        Long referenciaId,

        @Schema(description = "Usuário que fez a movimentação", example = "3")
        Long usuarioId,

        @Schema(description = "Data/hora da movimentação")
        OffsetDateTime criadoEm
) {
}
