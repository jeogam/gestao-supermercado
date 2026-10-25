package br.com.gestaosupermercado.modules.relatorio.relatorio.repository.projection;

import java.math.BigDecimal;

/**
 * Totais de um produto. O custo vem sem arredondar (quantidade com 3 casas vezes custo com 2).
 */
public record TotalProduto(Long produtoId, BigDecimal quantidadeVendida, BigDecimal faturamento, BigDecimal custo) {
}
