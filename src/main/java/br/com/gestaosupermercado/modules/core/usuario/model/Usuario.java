package br.com.gestaosupermercado.modules.core.usuario.model;

import br.com.gestaosupermercado.modules.core.perfil.model.Perfil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Conta de acesso ao sistema. Concentra aqui as regras de estado da credencial (bloqueio por tentativas,
 * troca de senha) para que login e gestão de usuários não as reimplementem de formas diferentes.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Entity
@Table(name = "usuario")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "email", nullable = false)
    private String email;

    @Setter(AccessLevel.NONE)
    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Setter(AccessLevel.NONE)
    @Column(name = "tentativas_login_falhas", nullable = false)
    private int tentativasLoginFalhas;

    @Setter(AccessLevel.NONE)
    @Column(name = "bloqueado_ate")
    private OffsetDateTime bloqueadoAte;

    @Setter(AccessLevel.NONE)
    @Column(name = "senha_alterada_em", nullable = false)
    private OffsetDateTime senhaAlteradaEm;

    @Setter(AccessLevel.NONE)
    @Column(name = "ultimo_login_em")
    private OffsetDateTime ultimoLoginEm;

    @ManyToMany
    @JoinTable(name = "usuario_perfil",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "perfil_id"))
    private Set<Perfil> perfis = new HashSet<>();

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    public Usuario(final String nome, final String email, final String senhaHash, final Set<Perfil> perfis, final OffsetDateTime agora) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.perfis = new HashSet<>(perfis);
        this.senhaAlteradaEm = agora;
    }

    public boolean isBloqueado(final OffsetDateTime agora) {
        return bloqueadoAte != null && bloqueadoAte.isAfter(agora);
    }

    /**
     * Ao atingir o limite, bloqueia por {@code tempoBloqueio} e zera o contador: após o bloqueio o usuário
     * volta a ter o mesmo número de tentativas.
     */
    public void registerFalhaLogin(final int maxTentativas, final Duration tempoBloqueio, final OffsetDateTime agora) {
        tentativasLoginFalhas++;
        if (tentativasLoginFalhas >= maxTentativas) {
            bloqueadoAte = agora.plus(tempoBloqueio);
            tentativasLoginFalhas = 0;
        }
    }

    public void registerLoginSucesso(final OffsetDateTime agora) {
        tentativasLoginFalhas = 0;
        bloqueadoAte = null;
        ultimoLoginEm = agora;
    }

    /**
     * Troca o hash e marca o momento da troca, o que invalida todo access token emitido antes dela.
     */
    public void updateSenha(final String novoSenhaHash, final OffsetDateTime agora) {
        senhaHash = novoSenhaHash;
        senhaAlteradaEm = agora;
        tentativasLoginFalhas = 0;
        bloqueadoAte = null;
    }

    public void replacePerfis(final Set<Perfil> novosPerfis) {
        perfis.clear();
        perfis.addAll(novosPerfis);
    }
}
