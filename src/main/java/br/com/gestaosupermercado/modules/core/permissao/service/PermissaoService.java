package br.com.gestaosupermercado.modules.core.permissao.service;

import br.com.gestaosupermercado.modules.core.permissao.dto.response.PermissaoResponse;
import java.util.List;

public interface PermissaoService {

    List<PermissaoResponse> findAllPermissoes();
}
