package br.com.gestaosupermercado.modules.compra.compra.repository.specs;

import br.com.gestaosupermercado.modules.compra.compra.dto.request.CompraFiltroRequest;
import br.com.gestaosupermercado.modules.compra.compra.model.Compra;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class CompraSpecs {

    private CompraSpecs() {
    }

    public static Specification<Compra> byFiltro(final CompraFiltroRequest filtro) {
        return (root, query, builder) -> {
            final List<Predicate> predicates = new ArrayList<>();
            if (filtro.fornecedorId() != null) {
                predicates.add(builder.equal(root.get("fornecedor").get("id"), filtro.fornecedorId()));
            }
            if (filtro.numeroNotaFiscal() != null && !filtro.numeroNotaFiscal().isBlank()) {
                predicates.add(builder.equal(root.get("numeroNotaFiscal"), filtro.numeroNotaFiscal().strip()));
            }
            if (filtro.dataInicio() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("criadoEm"), filtro.dataInicio()));
            }
            if (filtro.dataFim() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("criadoEm"), filtro.dataFim()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
