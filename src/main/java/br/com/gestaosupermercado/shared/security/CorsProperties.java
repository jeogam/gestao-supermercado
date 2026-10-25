package br.com.gestaosupermercado.shared.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Origens (front) que podem chamar a API pelo navegador. Lista vazia desliga o CORS: só chamadas da mesma origem.
 *
 * @since 0.0.1
 */
@ConfigurationProperties(prefix = "app.security.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins.stream().map(String::strip).filter(origem -> !origem.isEmpty()).toList();
    }
}
