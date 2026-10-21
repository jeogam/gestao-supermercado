package br.com.gestaosupermercado.modules.core.permissao.repository;

import br.com.gestaosupermercado.modules.core.permissao.model.PermissaoCatalogo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissaoCatalogoRepository extends JpaRepository<PermissaoCatalogo, String> {

    List<PermissaoCatalogo> findAllByOrderByGrupoAscCodigoAsc();
}
