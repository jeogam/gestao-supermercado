package br.com.gestaosupermercado.modules.estoque.movimentacao.repository;

import br.com.gestaosupermercado.modules.estoque.movimentacao.model.MovimentacaoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long>, JpaSpecificationExecutor<MovimentacaoEstoque> {
}
