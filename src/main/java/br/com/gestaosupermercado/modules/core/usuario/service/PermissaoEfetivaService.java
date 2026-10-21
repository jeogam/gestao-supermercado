package br.com.gestaosupermercado.modules.core.usuario.service;

import br.com.gestaosupermercado.modules.core.permissao.enums.Permissao;
import java.util.Set;

public interface PermissaoEfetivaService {

    /**
     * Permissões que o usuário realmente tem agora: todas, se tiver o perfil Superadmin ativo; senão, a união
     * das permissões dos seus perfis ativos.
     */
    Set<Permissao> findPermissoesEfetivasByUsuarioId(Long usuarioId);
}
