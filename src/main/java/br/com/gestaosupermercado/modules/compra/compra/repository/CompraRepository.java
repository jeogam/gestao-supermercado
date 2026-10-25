package br.com.gestaosupermercado.modules.compra.compra.repository;

import br.com.gestaosupermercado.modules.compra.compra.model.Compra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CompraRepository extends JpaRepository<Compra, Long>, JpaSpecificationExecutor<Compra> {

    // Traz o fornecedor junto: a listagem mostra o nome dele em cada linha.
    @Override
    @EntityGraph(attributePaths = "fornecedor")
    Page<Compra> findAll(Specification<Compra> spec, Pageable pageable);
}
