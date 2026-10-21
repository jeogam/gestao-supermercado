package br.com.gestaosupermercado.modules.core.permissao.service;

import br.com.gestaosupermercado.modules.core.permissao.dto.response.PermissaoResponse;
import br.com.gestaosupermercado.modules.core.permissao.repository.PermissaoCatalogoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PermissaoServiceImpl implements PermissaoService {

    private final PermissaoCatalogoRepository permissaoCatalogoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PermissaoResponse> findAllPermissoes() {
        return permissaoCatalogoRepository.findAllByOrderByGrupoAscCodigoAsc().stream()
                .map(permissao -> new PermissaoResponse(permissao.getCodigo(), permissao.getDescricao(), permissao.getGrupo()))
                .toList();
    }
}
