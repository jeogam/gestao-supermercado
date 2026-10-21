package br.com.gestaosupermercado.modules.core.auth.service;

import br.com.gestaosupermercado.modules.core.auth.config.JwtProperties;
import br.com.gestaosupermercado.modules.core.auth.dto.response.TokenResponse;
import br.com.gestaosupermercado.modules.core.auth.model.RefreshToken;
import br.com.gestaosupermercado.modules.core.auth.repository.RefreshTokenRepository;
import br.com.gestaosupermercado.modules.core.usuario.model.Usuario;
import br.com.gestaosupermercado.shared.exception.BusinessException;
import br.com.gestaosupermercado.shared.exception.MensagemErro;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Access token: JWT curto, só com o id do usuário. As permissões <b>não</b> vão no token; são lidas do
 * banco a cada requisição, então mudanças de perfil valem na hora.
 * <p>
 * Refresh token: valor aleatório opaco, de uso único (rotação). Reapresentar um token já rotacionado
 * indica vazamento, e todas as sessões do usuário são revogadas.
 *
 * @since 0.0.1
 */
@Service
@RequiredArgsConstructor
public class SessaoServiceImpl implements SessaoService {

    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    @Override
    @Transactional
    public TokenResponse createSessao(final Usuario usuario) {
        final Instant agora = clock.instant();
        final String refreshToken = generateRefreshToken();
        refreshTokenRepository.save(new RefreshToken(usuario, hashToken(refreshToken),
                OffsetDateTime.now(clock).plus(jwtProperties.refreshTokenTtl())));
        return new TokenResponse(generateAccessToken(usuario, agora), "Bearer",
                jwtProperties.accessTokenTtl().toSeconds(), refreshToken);
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public TokenResponse rotateSessao(final String refreshToken) {
        final OffsetDateTime agora = OffsetDateTime.now(clock);
        final RefreshToken atual = refreshTokenRepository.findByTokenHash(hashToken(refreshToken))
                .orElseThrow(() -> BusinessException.unauthorized(MensagemErro.REFRESH_TOKEN_INVALIDO));
        final Usuario usuario = atual.getUsuario();

        if (atual.isRevogado()) {
            refreshTokenRepository.revokeAllByUsuarioId(usuario.getId(), agora);
            throw BusinessException.unauthorized(MensagemErro.REFRESH_TOKEN_INVALIDO);
        }
        if (atual.isExpirado(agora) || !usuario.isAtivo()) {
            atual.revoke(agora);
            throw BusinessException.unauthorized(MensagemErro.REFRESH_TOKEN_INVALIDO);
        }
        atual.revoke(agora);
        return createSessao(usuario);
    }

    @Override
    @Transactional
    public void revokeSessao(final String refreshToken) {
        refreshTokenRepository.findByTokenHash(hashToken(refreshToken))
                .ifPresent(token -> token.revoke(OffsetDateTime.now(clock)));
    }

    @Override
    @Transactional
    public void revokeAllSessoes(final Long usuarioId) {
        refreshTokenRepository.revokeAllByUsuarioId(usuarioId, OffsetDateTime.now(clock));
    }

    private String generateAccessToken(final Usuario usuario, final Instant agora) {
        final JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(usuario.getId().toString())
                .issuedAt(agora)
                .expiresAt(agora.plus(jwtProperties.accessTokenTtl()))
                .claim("nome", usuario.getNome())
                .build();
        final JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static String generateRefreshToken() {
        final byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashToken(final String token) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", exception);
        }
    }
}
