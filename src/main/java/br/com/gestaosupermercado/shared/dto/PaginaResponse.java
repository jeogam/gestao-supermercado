package br.com.gestaosupermercado.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Formato estável de paginação. Serializar {@link Page} direto expõe a estrutura interna do Spring Data,
 * que muda entre versões.
 *
 * @since 0.0.1
 */
@Schema(description = "Página de resultados")
public record PaginaResponse<T>(
        @Schema(description = "Itens da página")
        List<T> conteudo,

        @Schema(description = "Número da página (base 0)", example = "0")
        int pagina,

        @Schema(description = "Tamanho da página", example = "20")
        int tamanho,

        @Schema(description = "Total de itens", example = "57")
        long totalItens,

        @Schema(description = "Total de páginas", example = "3")
        int totalPaginas
) {

    public static <T> PaginaResponse<T> of(final Page<T> page) {
        return new PaginaResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
