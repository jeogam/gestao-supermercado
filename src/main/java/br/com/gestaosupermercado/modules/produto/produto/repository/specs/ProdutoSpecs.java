package br.com.gestaosupermercado.modules.produto.produto.repository.specs;

import br.com.gestaosupermercado.modules.produto.produto.dto.request.ProdutoFiltroRequest;
import br.com.gestaosupermercado.modules.produto.produto.model.Produto;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class ProdutoSpecs {

    private static final char ESCAPE = '\\';

    private ProdutoSpecs() {
    }

    public static Specification<Produto> byFiltro(final ProdutoFiltroRequest filtro) {
        return (root, query, builder) -> {
            final List<Predicate> predicates = new ArrayList<>();
            if (filtro.nome() != null && !filtro.nome().isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("nome")), toLikePattern(filtro.nome()), ESCAPE));
            }
            // Igualdade exata: é a busca do leitor de código de barras no caixa.
            if (filtro.codigoBarras() != null && !filtro.codigoBarras().isBlank()) {
                predicates.add(builder.equal(root.get("codigoBarras"), filtro.codigoBarras().strip()));
            }
            if (filtro.ativo() != null) {
                predicates.add(builder.equal(root.get("ativo"), filtro.ativo()));
            }
            if (Boolean.TRUE.equals(filtro.abaixoEstoqueMinimo())) {
                predicates.add(builder.lessThan(root.get("quantidadeEstoque"), root.get("estoqueMinimo")));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String toLikePattern(final String valor) {
        final String escapado = valor.strip().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escapado + "%";
    }
}
