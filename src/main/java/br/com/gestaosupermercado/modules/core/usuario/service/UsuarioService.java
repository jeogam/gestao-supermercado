package br.com.gestaosupermercado.modules.core.usuario.service;

import br.com.gestaosupermercado.modules.core.usuario.dto.request.SenhaAlteracaoRequest;
import br.com.gestaosupermercado.modules.core.usuario.dto.request.UsuarioCreateRequest;
import br.com.gestaosupermercado.modules.core.usuario.dto.request.UsuarioFiltroRequest;
import br.com.gestaosupermercado.modules.core.usuario.dto.request.UsuarioUpdateRequest;
import br.com.gestaosupermercado.modules.core.usuario.dto.response.UsuarioLogadoResponse;
import br.com.gestaosupermercado.modules.core.usuario.dto.response.UsuarioResponse;
import br.com.gestaosupermercado.shared.dto.PaginaResponse;
import org.springframework.data.domain.Pageable;

public interface UsuarioService {

    PaginaResponse<UsuarioResponse> findAllUsuarios(UsuarioFiltroRequest filtro, Pageable pageable);

    UsuarioResponse findUsuarioById(Long id);

    UsuarioResponse saveUsuario(UsuarioCreateRequest request);

    UsuarioResponse updateUsuario(Long id, UsuarioUpdateRequest request);

    void updateStatusUsuario(Long id, boolean ativo, Long usuarioLogadoId);

    void updateSenhaUsuario(Long id, String novaSenha);

    UsuarioLogadoResponse findUsuarioLogado(Long usuarioLogadoId);

    void updateSenhaUsuarioLogado(Long usuarioLogadoId, SenhaAlteracaoRequest request);
}
