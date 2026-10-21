package br.com.gestaosupermercado.modules.core.auth.repository;

import br.com.gestaosupermercado.modules.core.auth.model.RefreshToken;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @EntityGraph(attributePaths = "usuario")
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken r set r.revogadoEm = :agora where r.usuario.id = :usuarioId and r.revogadoEm is null")
    int revokeAllByUsuarioId(@Param("usuarioId") Long usuarioId, @Param("agora") OffsetDateTime agora);
}
