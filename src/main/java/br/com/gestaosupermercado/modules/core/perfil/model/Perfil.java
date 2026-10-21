package br.com.gestaosupermercado.modules.core.perfil.model;

import br.com.gestaosupermercado.modules.core.permissao.enums.Permissao;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
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
 * Perfil de acesso — no supermercado corresponde ao cargo (Gerente, Caixa, Estoquista...). O acesso do
 * usuário é a união das permissões dos seus perfis <b>ativos</b>; inativar um perfil retira o acesso de
 * todos que o possuem, sem desvinculá-los.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Entity
@Table(name = "perfil")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Perfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Setter(AccessLevel.NONE)
    @Column(name = "sistema", nullable = false, updatable = false)
    private boolean sistema;

    @Enumerated(EnumType.STRING)
    @ElementCollection
    @CollectionTable(name = "perfil_permissao", joinColumns = @JoinColumn(name = "perfil_id"))
    @Column(name = "permissao_codigo")
    private Set<Permissao> permissoes = new HashSet<>();

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    public Perfil(final String nome, final String descricao, final Set<Permissao> permissoes) {
        this.nome = nome;
        this.descricao = descricao;
        this.permissoes = new HashSet<>(permissoes);
    }

    public void replacePermissoes(final Set<Permissao> novasPermissoes) {
        permissoes.clear();
        permissoes.addAll(novasPermissoes);
    }
}
