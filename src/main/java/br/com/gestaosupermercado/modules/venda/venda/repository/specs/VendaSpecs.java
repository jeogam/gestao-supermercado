package br.com.gestaosupermercado.modules.venda.venda.repository.specs;

import br.com.gestaosupermercado.modules.venda.venda.dto.request.VendaFiltroRequest;
import br.com.gestaosupermercado.modules.venda.venda.model.Venda;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class VendaSpecs {

    private VendaSpecs() {
    }

    public static Specification<Venda> byFiltro(final VendaFiltroRequest filtro) {
        return (root, query, builder) -> {
            final List<Predicate> predicates = new ArrayList<>();
            if (filtro.status() != null) {
                predicates.add(builder.equal(root.get("status"), filtro.status()));
            }
            if (filtro.formaPagamento() != null) {
                predicates.add(builder.equal(root.get("formaPagamento"), filtro.formaPagamento()));
            }
            if (filtro.usuarioId() != null) {
                predicates.add(builder.equal(root.get("usuarioId"), filtro.usuarioId()));
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
