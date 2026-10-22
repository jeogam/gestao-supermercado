package br.com.gestaosupermercado.modules.core.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.gestaosupermercado.modules.core.auth.config.LoginProperties;
import br.com.gestaosupermercado.modules.core.auth.dto.request.LoginRequest;
import br.com.gestaosupermercado.modules.core.auth.dto.response.TokenResponse;
import br.com.gestaosupermercado.modules.core.usuario.model.Usuario;
import br.com.gestaosupermercado.modules.core.usuario.repository.UsuarioRepository;
import br.com.gestaosupermercado.shared.exception.BusinessException;
import br.com.gestaosupermercado.shared.exception.ErrorMessage;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SessaoService sessaoService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(any())).thenReturn("hashFicticio");
        authService = new AuthServiceImpl(usuarioRepository, sessaoService, passwordEncoder,
                new LoginProperties(2, Duration.ofMinutes(15)), CLOCK);
    }

    @Test
    void shouldNormalizeEmailAndCreateSessaoWhenCredenciaisValidas() {
        final Usuario usuario = newUsuario();
        final TokenResponse tokenResponse = new TokenResponse("access", "Bearer", 900, "refresh");
        when(usuarioRepository.findByEmail("maria@mercado.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Senha123", "hash")).thenReturn(true);
        when(sessaoService.createSessao(usuario)).thenReturn(tokenResponse);

        final TokenResponse resultado = authService.login(new LoginRequest("  Maria@Mercado.com ", "Senha123"));

        assertThat(resultado).isEqualTo(tokenResponse);
        assertThat(usuario.getUltimoLoginEm()).isEqualTo(OffsetDateTime.now(CLOCK));
    }

    @Test
    void shouldReturnGenericErrorAndStillHashWhenEmailNaoExiste() {
        when(usuarioRepository.findByEmail("ninguem@mercado.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ninguem@mercado.com", "Senha123")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorMessage.CREDENCIAIS_INVALIDAS.getDescricao());
        verify(passwordEncoder).matches("Senha123", "hashFicticio");
    }

    @Test
    void shouldBlockUsuarioAfterMaxTentativasAndRejectEvenWithSenhaCorreta() {
        final Usuario usuario = newUsuario();
        when(usuarioRepository.findByEmail("maria@mercado.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        for (int tentativa = 0; tentativa < 2; tentativa++) {
            assertThatThrownBy(() -> authService.login(new LoginRequest("maria@mercado.com", "errada")))
                    .isInstanceOf(BusinessException.class);
        }

        assertThat(usuario.isBloqueado(OffsetDateTime.now(CLOCK))).isTrue();
        assertThatThrownBy(() -> authService.login(new LoginRequest("maria@mercado.com", "Senha123")))
                .hasMessage(ErrorMessage.CREDENCIAIS_INVALIDAS.getDescricao());
        verify(sessaoService, never()).createSessao(any());
    }

    @Test
    void shouldRejectUsuarioInativoWithGenericError() {
        final Usuario usuario = newUsuario();
        usuario.setAtivo(false);
        when(usuarioRepository.findByEmail("maria@mercado.com")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.login(new LoginRequest("maria@mercado.com", "Senha123")))
                .hasMessage(ErrorMessage.CREDENCIAIS_INVALIDAS.getDescricao());
        verify(sessaoService, never()).createSessao(any());
    }

    private static Usuario newUsuario() {
        return new Usuario("Maria", "maria@mercado.com", "hash", Set.of(), OffsetDateTime.now(CLOCK));
    }
}
