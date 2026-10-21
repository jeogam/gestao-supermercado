package br.com.gestaosupermercado.modules.core.auth.service;

import br.com.gestaosupermercado.modules.core.auth.dto.response.TokenResponse;
import br.com.gestaosupermercado.modules.core.usuario.model.Usuario;

/**
 * Emissão e revogação de sessões (access token + refresh token). Separado do {@link AuthService} para que
 * a gestão de usuários possa encerrar sessões (inativação, troca de senha) sem depender do fluxo de login.
 */
public interface SessaoService {

    TokenResponse createSessao(Usuario usuario);

    TokenResponse rotateSessao(String refreshToken);

    void revokeSessao(String refreshToken);

    void revokeAllSessoes(Long usuarioId);
}
