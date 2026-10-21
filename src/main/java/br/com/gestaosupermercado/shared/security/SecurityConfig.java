package br.com.gestaosupermercado.shared.security;

import br.com.gestaosupermercado.modules.core.auth.config.JwtProperties;
import br.com.gestaosupermercado.shared.exception.ErroResponse;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

/**
 * API stateless: autenticação por Bearer JWT e autorização por permissão via {@code @PreAuthorize}.
 * Tudo exige autenticação, exceto os endpoints de login/refresh/logout e a documentação.
 *
 * @since 0.0.1
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] ROTAS_PUBLICAS = {
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout"
    };

    private static final String[] ROTAS_DOCUMENTACAO = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http,
                                                   final UsuarioJwtAuthenticationConverter usuarioJwtAuthenticationConverter,
                                                   final JsonMapper jsonMapper) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, ROTAS_PUBLICAS).permitAll()
                        .requestMatchers(ROTAS_DOCUMENTACAO).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(usuarioJwtAuthenticationConverter))
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            jsonMapper.writeValue(response.getOutputStream(),
                                    ErroResponse.of(HttpStatus.UNAUTHORIZED.value(), "Autenticação necessária ou sessão expirada."));
                        }))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder(final JwtProperties jwtProperties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(toSecretKey(jwtProperties)));
    }

    @Bean
    public JwtDecoder jwtDecoder(final JwtProperties jwtProperties) {
        final NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(toSecretKey(jwtProperties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(jwtProperties.issuer())));
        return jwtDecoder;
    }

    private static SecretKey toSecretKey(final JwtProperties jwtProperties) {
        return new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
