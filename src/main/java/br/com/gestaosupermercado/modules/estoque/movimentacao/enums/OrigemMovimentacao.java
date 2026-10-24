package br.com.gestaosupermercado.modules.estoque.movimentacao.enums;

/**
 * O que gerou a movimentação de estoque. Valor novo exige migration que atualize a constraint
 * {@code ck_movimentacao_estoque_origem}.
 *
 * @since 0.0.1
 */
public enum OrigemMovimentacao {
    /** Correção manual pelo Estoquista/Gerente (perda, avaria, inventário). Exige motivo. */
    AJUSTE,
    COMPRA,
    VENDA,
    ESTORNO_VENDA
}
