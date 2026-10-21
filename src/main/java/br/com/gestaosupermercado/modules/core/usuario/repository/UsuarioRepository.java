package br.com.gestaosupermercado.modules.core.usuario.repository;

import br.com.gestaosupermercado.modules.core.permissao.enums.Permissao;
import br.com.gestaosupermercado.modules.core.usuario.model.Usuario;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    @EntityGraph(attributePaths = "perfis")
    Optional<Usuario> findWithPerfisById(Long id);

    /**
     * União das permissões dos perfis <b>ativos</b> do usuário. Para a permissão efetiva (que também trata o
     * Superadmin) use {@code PermissaoEfetivaService}.
     */
    @Query("""
            select distinct permissao
            from Usuario u
            join u.perfis perfil
            join perfil.permissoes permissao
            where u.id = :usuarioId and perfil.ativo = true
            """)
    Set<Permissao> findPermissoesEfetivasByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("""
            select count(u) > 0
            from Usuario u
            join u.perfis perfil
            where u.id = :usuarioId and perfil.sistema = true and perfil.ativo = true
            """)
    boolean existsSuperadminByUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     * Quantos usuários ativos, além de {@code usuarioIdIgnorado}, ainda têm o perfil do sistema ativo.
     * Usado para impedir que uma operação deixe a loja sem Superadmin.
     */
    @Query("""
            select count(distinct u)
            from Usuario u
            join u.perfis perfil
            where u.ativo = true and perfil.sistema = true and perfil.ativo = true and u.id <> :usuarioIdIgnorado
            """)
    long countSuperadminsAtivosExcept(@Param("usuarioIdIgnorado") Long usuarioIdIgnorado);
}
