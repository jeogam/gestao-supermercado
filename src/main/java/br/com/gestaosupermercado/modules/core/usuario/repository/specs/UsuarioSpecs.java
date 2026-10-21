package br.com.gestaosupermercado.modules.core.usuario.repository.specs;

import br.com.gestaosupermercado.modules.core.perfil.model.Perfil;
import br.com.gestaosupermercado.modules.core.usuario.dto.request.UsuarioFiltroRequest;
import br.com.gestaosupermercado.modules.core.usuario.model.Usuario;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class UsuarioSpecs {

    private static final char ESCAPE = '\\';

    private UsuarioSpecs() {
    }

    public static Specification<Usuario> byFiltro(final UsuarioFiltroRequest filtro) {
        return (root, query, builder) -> {
            final List<Predicate> predicates = new ArrayList<>();
            if (filtro.nome() != null && !filtro.nome().isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("nome")), toLikePattern(filtro.nome()), ESCAPE));
            }
            if (filtro.email() != null && !filtro.email().isBlank()) {
                predicates.add(builder.like(root.get("email"), toLikePattern(filtro.email()), ESCAPE));
            }
            if (filtro.ativo() != null) {
                predicates.add(builder.equal(root.get("ativo"), filtro.ativo()));
            }
            if (filtro.perfilId() != null) {
                // Subquery em vez de join para não duplicar linhas nem quebrar a contagem da paginação.
                final Subquery<Long> subquery = query.subquery(Long.class);
                final var subRoot = subquery.from(Usuario.class);
                final Join<Usuario, Perfil> perfil = subRoot.join("perfis");
                subquery.select(subRoot.get("id")).where(builder.equal(perfil.get("id"), filtro.perfilId()));
                predicates.add(root.get("id").in(subquery));
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
