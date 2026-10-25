package br.com.gestaosupermercado.modules.compra.fornecedor.repository.specs;

import br.com.gestaosupermercado.modules.compra.fornecedor.dto.request.FornecedorFiltroRequest;
import br.com.gestaosupermercado.modules.compra.fornecedor.model.Fornecedor;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class FornecedorSpecs {

    private static final char ESCAPE = '\\';

    private FornecedorSpecs() {
    }

    public static Specification<Fornecedor> byFiltro(final FornecedorFiltroRequest filtro) {
        return (root, query, builder) -> {
            final List<Predicate> predicates = new ArrayList<>();
            if (filtro.nome() != null && !filtro.nome().isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("nome")), toLikePattern(filtro.nome()), ESCAPE));
            }
            if (filtro.cnpj() != null && !filtro.cnpj().isBlank()) {
                predicates.add(builder.equal(root.get("cnpj"), filtro.cnpj().replaceAll("\\D", "")));
            }
            if (filtro.ativo() != null) {
                predicates.add(builder.equal(root.get("ativo"), filtro.ativo()));
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
