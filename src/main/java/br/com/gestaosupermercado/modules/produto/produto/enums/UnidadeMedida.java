package br.com.gestaosupermercado.modules.produto.produto.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Como o produto é vendido e contado no estoque. Só {@link #UN} exige quantidade inteira.
 *
 * @since 0.0.1
 */
@Getter
@RequiredArgsConstructor
public enum UnidadeMedida {

    UN(false),
    KG(true),
    L(true);

    private final boolean fracionavel;
}
