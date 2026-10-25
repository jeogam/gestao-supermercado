package br.com.gestaosupermercado.modules.compra.fornecedor.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "Fornecedor")
public record FornecedorResponse(
        @Schema(description = "Identificador", example = "4")
        Long id,

        @Schema(description = "Nome ou razão social", example = "Atacadão Irecê Ltda")
        String nome,

        @Schema(description = "CNPJ (só dígitos)", example = "12345678000190")
        String cnpj,

        @Schema(description = "Telefone de contato", example = "(74) 99999-0000")
        String telefone,

        @Schema(description = "E-mail de contato", example = "vendas@atacadao.com")
        String email,

        @Schema(description = "Se pode receber novas compras", example = "true")
        boolean ativo,

        @Schema(description = "Data/hora de cadastro")
        OffsetDateTime criadoEm
) {
}
