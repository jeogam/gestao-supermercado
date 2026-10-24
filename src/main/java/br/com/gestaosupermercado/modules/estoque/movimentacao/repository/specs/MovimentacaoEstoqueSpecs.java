package br.com.gestaosupermercado.modules.estoque.movimentacao.repository.specs;

import br.com.gestaosupermercado.modules.estoque.movimentacao.dto.request.MovimentacaoEstoqueFiltroRequest;
import br.com.gestaosupermercado.modules.estoque.movimentacao.model.MovimentacaoEstoque;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class MovimentacaoEstoqueSpecs {

    private MovimentacaoEstoqueSpecs() {
    }

    public static Specification<MovimentacaoEstoque> byFiltro(final MovimentacaoEstoqueFiltroRequest filtro) {
        return (root, query, builder) -> {
            final List<Predicate> predicates = new ArrayList<>();
            if (filtro.produtoId() != null) {
                predicates.add(builder.equal(root.get("produtoId"), filtro.produtoId()));
            }
            if (filtro.tipo() != null) {
                predicates.add(builder.equal(root.get("tipo"), filtro.tipo()));
            }
            if (filtro.origem() != null) {
                predicates.add(builder.equal(root.get("origem"), filtro.origem()));
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
