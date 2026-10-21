package br.com.gestaosupermercado.modules.core.permissao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Linha do catálogo de permissões. Somente leitura pela aplicação: o catálogo é mantido por migrations.
 *
 * @since 0.0.1
 */
@Getter
@Entity
@Table(name = "permissao")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PermissaoCatalogo {

    @Id
    @Column(name = "codigo")
    private String codigo;

    @Column(name = "descricao", nullable = false)
    private String descricao;

    @Column(name = "grupo", nullable = false)
    private String grupo;
}
