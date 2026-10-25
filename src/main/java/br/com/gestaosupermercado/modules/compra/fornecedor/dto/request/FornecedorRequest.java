package br.com.gestaosupermercado.modules.compra.fornecedor.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para cadastro e edição de fornecedor")
public record FornecedorRequest(
        @Schema(description = "Nome ou razão social", example = "Atacadão Irecê Ltda", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 255)
        String nome,

        @Schema(description = "CNPJ com ou sem pontuação; opcional para fornecedores sem CNPJ", example = "12.345.678/0001-90")
        @Pattern(regexp = "^\\s*$|^\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}$", message = "deve ser um CNPJ com 14 dígitos")
        String cnpj,

        @Schema(description = "Telefone de contato", example = "(74) 99999-0000")
        @Size(max = 20)
        String telefone,

        @Schema(description = "E-mail de contato", example = "vendas@atacadao.com")
        @Email
        @Size(max = 255)
        String email
) {
}
