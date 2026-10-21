package br.com.gestaosupermercado.shared.security;

import br.com.gestaosupermercado.modules.core.usuario.model.Usuario;
import br.com.gestaosupermercado.modules.core.usuario.repository.UsuarioRepository;
import br.com.gestaosupermercado.modules.core.usuario.service.PermissaoEfetivaService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Transforma um JWT já validado (assinatura, emissor, expiração) em autenticação com as permissões
 * <b>atuais</b> do usuário. Consultar o banco a cada requisição é o que torna o RBAC dinâmico: inativar o
 * usuário, trocar a senha ou mudar um perfil tem efeito imediato, sem esperar o token expirar.
 *
 * @since 0.0.1
 */
@Component
@RequiredArgsConstructor
public class UsuarioJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepository usuarioRepository;
    private final PermissaoEfetivaService permissaoEfetivaService;

    @Override
    public AbstractAuthenticationToken convert(final Jwt jwt) {
        final Long usuarioId = parseUsuarioId(jwt.getSubject());
        final Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new InvalidBearerTokenException("Usuário do token não existe"));

        if (!usuario.isAtivo()) {
            throw new InvalidBearerTokenException("Usuário inativo");
        }
        if (isEmitidoAntesDaTrocaDeSenha(jwt.getIssuedAt(), usuario)) {
            throw new InvalidBearerTokenException("Token emitido antes da última troca de senha");
        }

        final List<SimpleGrantedAuthority> authorities = permissaoEfetivaService.findPermissoesEfetivasByUsuarioId(usuarioId).stream()
                .map(permissao -> new SimpleGrantedAuthority(permissao.name()))
                .toList();
        return new JwtAuthenticationToken(jwt, authorities, usuarioId.toString());
    }

    /**
     * O {@code iat} do JWT tem precisão de segundos; truncamos a data da troca para não invalidar um token
     * emitido no mesmo segundo, logo após a troca.
     */
    private static boolean isEmitidoAntesDaTrocaDeSenha(final Instant emitidoEm, final Usuario usuario) {
        return emitidoEm == null || emitidoEm.isBefore(usuario.getSenhaAlteradaEm().toInstant().truncatedTo(ChronoUnit.SECONDS));
    }

    private static Long parseUsuarioId(final String subject) {
        try {
            return Long.valueOf(subject);
        } catch (final NumberFormatException exception) {
            throw new InvalidBearerTokenException("Subject inválido");
        }
    }
}
