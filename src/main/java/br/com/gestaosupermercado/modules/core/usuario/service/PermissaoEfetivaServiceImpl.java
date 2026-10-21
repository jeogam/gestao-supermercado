package br.com.gestaosupermercado.modules.core.usuario.service;

import br.com.gestaosupermercado.modules.core.permissao.enums.Permissao;
import br.com.gestaosupermercado.modules.core.usuario.repository.UsuarioRepository;
import java.util.EnumSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Única fonte das permissões efetivas, usada tanto na autorização das requisições quanto no {@code /me}.
 * <p>
 * O Superadmin recebe todas as permissões do enum direto no código, e não pela tabela: assim, uma permissão
 * nova nunca fica de fora dele por esquecimento na migration.
 *
 * @since 0.0.1
 */
@Service
@RequiredArgsConstructor
public class PermissaoEfetivaServiceImpl implements PermissaoEfetivaService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public Set<Permissao> findPermissoesEfetivasByUsuarioId(final Long usuarioId) {
        if (usuarioRepository.existsSuperadminByUsuarioId(usuarioId)) {
            return EnumSet.allOf(Permissao.class);
        }
        return usuarioRepository.findPermissoesEfetivasByUsuarioId(usuarioId);
    }
}
