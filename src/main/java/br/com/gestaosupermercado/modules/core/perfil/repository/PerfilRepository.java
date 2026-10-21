package br.com.gestaosupermercado.modules.core.perfil.repository;

import br.com.gestaosupermercado.modules.core.perfil.model.Perfil;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PerfilRepository extends JpaRepository<Perfil, Long> {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    Optional<Perfil> findFirstBySistemaTrue();

    @EntityGraph(attributePaths = "permissoes")
    @Query("select p from Perfil p where (:ativo is null or p.ativo = :ativo) order by p.nome")
    List<Perfil> findAllPerfisByAtivo(@Param("ativo") Boolean ativo);

    @EntityGraph(attributePaths = "permissoes")
    Optional<Perfil> findWithPermissoesById(Long id);

    List<Perfil> findAllByIdIn(Collection<Long> ids);
}
