package br.com.gestaosupermercado.modules.core.auth.service;

import br.com.gestaosupermercado.modules.core.auth.config.LoginProperties;
import br.com.gestaosupermercado.modules.core.auth.dto.request.CadastroRequest;
import br.com.gestaosupermercado.modules.core.auth.dto.request.LoginRequest;
import br.com.gestaosupermercado.modules.core.auth.dto.response.TokenResponse;
import br.com.gestaosupermercado.modules.core.usuario.model.Usuario;
import br.com.gestaosupermercado.modules.core.usuario.repository.UsuarioRepository;
import br.com.gestaosupermercado.shared.exception.BusinessException;
import br.com.gestaosupermercado.shared.exception.ErrorMessage;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro público e login por e-mail e senha.
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
     * Cria a conta sem nenhum perfil (ADR-002): o usuário consegue entrar, mas não tem permissão até o
     * Superadmin vincular um perfil. Assim a rota pública não concede acesso a vendas, estoque ou relatórios.
     * <p>
     * O {@code saveAndFlush} faz a constraint de e-mail único estourar aqui dentro: dois cadastros simultâneos
     * com o mesmo e-mail passam juntos pelo {@code existsByEmail}, e o segundo precisa virar 409, não 500.
     */
    @Override
    @Transactional
    public void cadastrar(final CadastroRequest request) {
        final String email = normalizeEmail(request.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw BusinessException.conflict(ErrorMessage.EMAIL_JA_CADASTRADO);
        }
        try {
            usuarioRepository.saveAndFlush(new Usuario(request.nome().strip(), email, passwordEncoder.encode(request.senha()),
                    Set.of(), OffsetDateTime.now(clock)));
        } catch (final DataIntegrityViolationException exception) {
            throw BusinessException.conflict(ErrorMessage.EMAIL_JA_CADASTRADO);
        }
    }

    /**
     * Sem rollback em {@link BusinessException}: a contagem de tentativas falhas precisa ser gravada
     * justamente quando o login é recusado.
     */
    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public TokenResponse login(final LoginRequest request) {
        final OffsetDateTime agora = OffsetDateTime.now(clock);
        final Optional<Usuario> usuarioEncontrado = usuarioRepository.findByEmail(normalizeEmail(request.email()));

        if (usuarioEncontrado.isEmpty()) {
            passwordEncoder.matches(request.senha(), senhaHashFicticia);
            throw BusinessException.unauthorized(ErrorMessage.CREDENCIAIS_INVALIDAS);
        }

        final Usuario usuario = usuarioEncontrado.get();
        if (!usuario.isAtivo() || usuario.isBloqueado(agora)) {
            throw BusinessException.unauthorized(ErrorMessage.CREDENCIAIS_INVALIDAS);
        }
        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            usuario.registerFalhaLogin(loginProperties.maxTentativas(), loginProperties.tempoBloqueio(), agora);
            throw BusinessException.unauthorized(ErrorMessage.CREDENCIAIS_INVALIDAS);
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

    private static String normalizeEmail(final String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
