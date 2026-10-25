package br.com.gestaosupermercado.modules.relatorio.relatorio.repository;

import br.com.gestaosupermercado.modules.relatorio.relatorio.repository.projection.TotalFormaPagamento;
import br.com.gestaosupermercado.modules.relatorio.relatorio.repository.projection.TotalProduto;
import br.com.gestaosupermercado.modules.venda.venda.enums.StatusVenda;
import br.com.gestaosupermercado.modules.venda.venda.model.Venda;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Consultas agregadas, só de leitura, sobre as vendas. Os totais são somados no banco para não carregar cada venda.
 */
public interface RelatorioRepository extends Repository<Venda, Long> {

    @Query("""
            select new br.com.gestaosupermercado.modules.relatorio.relatorio.repository.projection.TotalFormaPagamento(
                v.formaPagamento, count(v), sum(v.valorTotal))
            from Venda v
            where v.status = :status and v.criadoEm between :dataInicio and :dataFim
            group by v.formaPagamento
            order by v.formaPagamento
            """)
    List<TotalFormaPagamento> sumVendasByFormaPagamento(StatusVenda status, OffsetDateTime dataInicio, OffsetDateTime dataFim);

    @Query("select count(v) from Venda v where v.status = :status and v.criadoEm between :dataInicio and :dataFim")
    long countVendas(StatusVenda status, OffsetDateTime dataInicio, OffsetDateTime dataFim);

    @Query("""
            select new br.com.gestaosupermercado.modules.relatorio.relatorio.repository.projection.TotalProduto(
                i.produtoId, sum(i.quantidade), sum(i.subtotal), sum(i.quantidade * i.precoCustoUnitario))
            from ItemVenda i join i.venda v
            where v.status = :status and v.criadoEm between :dataInicio and :dataFim
            group by i.produtoId
            """)
    List<TotalProduto> sumItensByProduto(StatusVenda status, OffsetDateTime dataInicio, OffsetDateTime dataFim);
}
