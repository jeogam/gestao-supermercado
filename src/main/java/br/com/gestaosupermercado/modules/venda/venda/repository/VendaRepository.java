package br.com.gestaosupermercado.modules.venda.venda.repository;

import br.com.gestaosupermercado.modules.venda.venda.model.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VendaRepository extends JpaRepository<Venda, Long>, JpaSpecificationExecutor<Venda> {
}
