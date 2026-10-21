package br.com.gestaosupermercado.modules.core.auth.service;

import br.com.gestaosupermercado.modules.core.auth.config.LoginProperties;
import br.com.gestaosupermercado.modules.core.auth.dto.request.LoginRequest;
import br.com.gestaosupermercado.modules.core.auth.dto.response.TokenResponse;
import br.com.gestaosupermercado.modules.core.usuario.model.Usuario;
import br.com.gestaosupermercado.modules.core.usuario.repository.UsuarioRepository;
import br.com.gestaosupermercado.shared.exception.BusinessException;
import br.com.gestaosupermercado.shared.exception.MensagemErro;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login por e-mail e senha.
 * <p>
 * Toda falha responde a mesma mensagem genérica (usuário inexistente, inativo, bloqueado ou senha errada)
 * para não revelar quais e-mails têm conta. Pelo mesmo motivo, e-mail inexistente ainda executa um BCrypt
 * para que o tempo de resposta não denuncie a diferença.
 *
 * @since 0.0.1
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final SessaoService sessaoService;
    private final PasswordEncoder passwordEncoder;
    private final LoginProperties loginProperties;
    private final Clock clock;
    private final String senhaHashFicticia;

    public AuthServiceImpl(final UsuarioRepository usuarioRepository, final SessaoService sessaoService,
                           final PasswordEncoder passwordEncoder, final LoginProperties loginProperties, final Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.sessaoService = sessaoService;
        this.passwordEncoder = passwordEncoder;
        this.loginProperties = loginProperties;
        this.clock = clock;
        this.senhaHashFicticia = passwordEncoder.encode("senha-ficticia-para-equalizar-tempo");
    }

    /**
     * Sem rollback em {@link BusinessException}: a contagem de tentativas falhas precisa ser gravada
     * justamente quando o login é recusado.
     */
    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public TokenResponse login(final LoginRequest request) {
        final OffsetDateTime agora = OffsetDateTime.now(clock);
        final Optional<Usuario> usuarioEncontrado = usuarioRepository.findByEmail(request.email().strip().toLowerCase(Locale.ROOT));

        if (usuarioEncontrado.isEmpty()) {
            passwordEncoder.matches(request.senha(), senhaHashFicticia);
            throw BusinessException.unauthorized(MensagemErro.CREDENCIAIS_INVALIDAS);
        }

        final Usuario usuario = usuarioEncontrado.get();
        if (!usuario.isAtivo() || usuario.isBloqueado(agora)) {
            throw BusinessException.unauthorized(MensagemErro.CREDENCIAIS_INVALIDAS);
        }
        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            usuario.registerFalhaLogin(loginProperties.maxTentativas(), loginProperties.tempoBloqueio(), agora);
            throw BusinessException.unauthorized(MensagemErro.CREDENCIAIS_INVALIDAS);
        }

        usuario.registerLoginSucesso(agora);
        return sessaoService.createSessao(usuario);
    }

    @Override
    public TokenResponse refresh(final String refreshToken) {
        return sessaoService.rotateSessao(refreshToken);
    }

    @Override
    public void logout(final String refreshToken) {
        sessaoService.revokeSessao(refreshToken);
    }
}
