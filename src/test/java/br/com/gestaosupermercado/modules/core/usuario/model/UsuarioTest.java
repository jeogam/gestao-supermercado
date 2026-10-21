package br.com.gestaosupermercado.modules.core.usuario.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UsuarioTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.of(2026, 10, 21, 12, 0, 0, 0, ZoneOffset.UTC);
    private static final Duration BLOQUEIO = Duration.ofMinutes(15);

    @Test
    void shouldBlockAfterMaxTentativasAndResetCounter() {
        final Usuario usuario = new Usuario("Maria", "maria@mercado.com", "hash", Set.of(), AGORA);

        usuario.registerFalhaLogin(3, BLOQUEIO, AGORA);
        usuario.registerFalhaLogin(3, BLOQUEIO, AGORA);
        assertThat(usuario.isBloqueado(AGORA)).isFalse();

        usuario.registerFalhaLogin(3, BLOQUEIO, AGORA);
        assertThat(usuario.isBloqueado(AGORA)).isTrue();
        assertThat(usuario.getTentativasLoginFalhas()).isZero();
        assertThat(usuario.isBloqueado(AGORA.plus(BLOQUEIO))).isFalse();
    }

    @Test
    void shouldClearBloqueioOnLoginSucesso() {
        final Usuario usuario = new Usuario("Maria", "maria@mercado.com", "hash", Set.of(), AGORA);
        usuario.registerFalhaLogin(1, BLOQUEIO, AGORA);

        usuario.registerLoginSucesso(AGORA);

        assertThat(usuario.isBloqueado(AGORA)).isFalse();
        assertThat(usuario.getUltimoLoginEm()).isEqualTo(AGORA);
    }

    @Test
    void shouldUpdateSenhaAlteradaEmAndUnblockOnUpdateSenha() {
        final Usuario usuario = new Usuario("Maria", "maria@mercado.com", "hash", Set.of(), AGORA);
        usuario.registerFalhaLogin(1, BLOQUEIO, AGORA);
        final OffsetDateTime depois = AGORA.plusMinutes(1);

        usuario.updateSenha("novoHash", depois);

        assertThat(usuario.getSenhaHash()).isEqualTo("novoHash");
        assertThat(usuario.getSenhaAlteradaEm()).isEqualTo(depois);
        assertThat(usuario.isBloqueado(depois)).isFalse();
    }
}
