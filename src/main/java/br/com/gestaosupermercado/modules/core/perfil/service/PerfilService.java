package br.com.gestaosupermercado.modules.core.perfil.service;

import br.com.gestaosupermercado.modules.core.perfil.dto.request.PerfilRequest;
import br.com.gestaosupermercado.modules.core.perfil.dto.response.PerfilResponse;
import br.com.gestaosupermercado.modules.core.perfil.model.Perfil;
import java.util.List;
import java.util.Set;

public interface PerfilService {

    List<PerfilResponse> findAllPerfis(Boolean ativo);

    PerfilResponse findPerfilById(Long id);

    PerfilResponse savePerfil(PerfilRequest request);

    PerfilResponse updatePerfil(Long id, PerfilRequest request);

    void updateStatusPerfil(Long id, boolean ativo);

    /**
     * Resolve os perfis a vincular em um usuário. Falha se algum id não existir ou estiver inativo.
     */
    Set<Perfil> findPerfisAtivosForVinculo(Set<Long> ids);
}
