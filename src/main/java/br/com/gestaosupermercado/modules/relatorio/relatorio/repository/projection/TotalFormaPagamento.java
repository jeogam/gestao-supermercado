package br.com.gestaosupermercado.modules.relatorio.relatorio.repository.projection;

import br.com.gestaosupermercado.modules.venda.venda.enums.FormaPagamento;
import java.math.BigDecimal;

public record TotalFormaPagamento(FormaPagamento formaPagamento, Long quantidadeVendas, BigDecimal faturamento) {
}
