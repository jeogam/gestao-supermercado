package br.com.gestaosupermercado.modules.core.usuario.listener;

import br.com.gestaosupermercado.modules.core.perfil.model.Perfil;
import br.com.gestaosupermercado.modules.core.perfil.repository.PerfilRepository;
import br.com.gestaosupermercado.modules.core.usuario.model.Usuario;
import br.com.gestaosupermercado.modules.core.usuario.repository.UsuarioRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o primeiro Administrador a partir de variáveis de ambiente ({@code ADMIN_EMAIL}, {@code ADMIN_SENHA})
 * quando o banco ainda não tem nenhum usuário. A senha nunca fica no código nem em migration.
 * <p>
 * Isto NÃO recria nem altera o administrador depois que existe qualquer usuário: com o banco populado,
 * as variáveis são ignoradas e podem ser removidas do ambiente.
 *
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdministradorInicialLoader implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Value("${app.bootstrap.admin.nome}")
    private String nome;

    @Value("${app.bootstrap.admin.email}")
    private String email;

    @Value("${app.bootstrap.admin.senha}")
    private String senha;

    @Override
    @Transactional
    public void run(final ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        if (email == null || email.isBlank() || senha == null || senha.isBlank()) {
            log.warn("Nenhum usuário cadastrado. Defina ADMIN_EMAIL e ADMIN_SENHA para criar o Administrador inicial.");
            return;
        }
        final Perfil administrador = perfilRepository.findFirstBySistemaTrue()
                .orElseThrow(() -> new IllegalStateException("Perfil do sistema (Administrador) não encontrado; verifique as migrations."));
        usuarioRepository.save(new Usuario(nome, email.strip().toLowerCase(Locale.ROOT), passwordEncoder.encode(senha),
                Set.of(administrador), OffsetDateTime.now(clock)));
        log.info("Administrador inicial criado.");
    }
}
