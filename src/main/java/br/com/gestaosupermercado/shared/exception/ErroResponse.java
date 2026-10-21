package br.com.gestaosupermercado.shared.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Corpo padrão de erro da API")
public record ErroResponse(
        @Schema(description = "Status HTTP", example = "422")
        int status,

        @Schema(description = "Mensagem principal do erro", example = "Dados inválidos.")
        String mensagem,

        @Schema(description = "Erros por campo, quando houver validação")
        List<CampoErro> campos,

        @Schema(description = "Momento em que o erro ocorreu")
        OffsetDateTime timestamp
) {

    public static ErroResponse of(final int status, final String mensagem) {
        return new ErroResponse(status, mensagem, List.of(), OffsetDateTime.now());
    }

    @Schema(description = "Erro de validação de um campo")
    public record CampoErro(
            @Schema(description = "Nome do campo", example = "email")
            String campo,

            @Schema(description = "Mensagem de validação", example = "deve ser um endereço de e-mail bem formado")
            String mensagem
    ) {
    }
}
