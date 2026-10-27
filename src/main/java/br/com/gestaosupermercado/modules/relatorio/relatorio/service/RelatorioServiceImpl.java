package br.com.gestaosupermercado.modules.relatorio.relatorio.service;

import br.com.gestaosupermercado.modules.produto.produto.model.Produto;
import br.com.gestaosupermercado.modules.produto.produto.service.ProdutoService;
import br.com.gestaosupermercado.modules.relatorio.relatorio.dto.request.RelatorioFiltroRequest;
import br.com.gestaosupermercado.modules.relatorio.relatorio.dto.response.FaturamentoFormaPagamentoResponse;
import br.com.gestaosupermercado.modules.relatorio.relatorio.dto.response.FaturamentoResponse;
import br.com.gestaosupermercado.modules.relatorio.relatorio.dto.response.MargemProdutoResponse;
import br.com.gestaosupermercado.modules.relatorio.relatorio.repository.RelatorioRepository;
import br.com.gestaosupermercado.modules.relatorio.relatorio.repository.projection.TotalProduto;
import br.com.gestaosupermercado.modules.venda.venda.enums.StatusVenda;
import br.com.gestaosupermercado.shared.exception.BusinessException;
import br.com.gestaosupermercado.shared.exception.ErrorMessage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relatórios de vendas: faturamento, custo e margem. Só vendas concluídas entram nos valores, e o custo é o
 * guardado em cada item no momento da venda, então reajustes posteriores não mudam relatórios passados.
 *
 * @since 0.0.1
 */
@Service
@RequiredArgsConstructor
public class RelatorioServiceImpl implements RelatorioService {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final RelatorioRepository relatorioRepository;
    private final ProdutoService produtoService;

    @Override
    @Transactional(readOnly = true)
    public FaturamentoResponse findFaturamento(final RelatorioFiltroRequest filtro) {
        ensurePeriodoValido(filtro);
        // Soma dos custos já arredondados por produto, para o total bater com a tabela de margem.
        final BigDecimal custo = relatorioRepository.sumItensByProduto(StatusVenda.CONCLUIDA, filtro.dataInicio(), filtro.dataFim()).stream()
                .map(total -> toCentavos(total.custo()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return toFaturamentoResponse(filtro, custo);
    }

    // shortcut: lista sem paginação (um mercado de bairro vende poucas centenas de produtos); paginar se o catálogo crescer muito.
    @Override
    @Transactional(readOnly = true)
    public List<MargemProdutoResponse> findMargemProdutos(final RelatorioFiltroRequest filtro) {
        ensurePeriodoValido(filtro);
        final List<TotalProduto> totais = relatorioRepository.sumItensByProduto(StatusVenda.CONCLUIDA, filtro.dataInicio(), filtro.dataFim());
        final Map<Long, Produto> produtos = produtoService.findProdutosByIds(totais.stream().map(TotalProduto::produtoId).toList());
        return totais.stream()
                .map(total -> toMargemProdutoResponse(total, produtos.get(total.produtoId())))
                .sorted(Comparator.comparing(MargemProdutoResponse::lucro).reversed().thenComparing(MargemProdutoResponse::produtoId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateRelatorioPdf(final RelatorioFiltroRequest filtro) {
        // A margem já traz o custo de cada produto: reaproveita para o resumo em vez de somar de novo no banco.
        final List<MargemProdutoResponse> produtos = findMargemProdutos(filtro);
        final FaturamentoResponse faturamento = toFaturamentoResponse(filtro,
                produtos.stream().map(MargemProdutoResponse::custo).reduce(BigDecimal.ZERO, BigDecimal::add));
        return RelatorioPdf.gerar(filtro, faturamento, produtos);
    }

    private FaturamentoResponse toFaturamentoResponse(final RelatorioFiltroRequest filtro, final BigDecimal custo) {
        final List<FaturamentoFormaPagamentoResponse> formasPagamento = relatorioRepository
                .sumVendasByFormaPagamento(StatusVenda.CONCLUIDA, filtro.dataInicio(), filtro.dataFim()).stream()
                .map(total -> new FaturamentoFormaPagamentoResponse(total.formaPagamento(), total.quantidadeVendas(), total.faturamento()))
                .toList();
        final long quantidadeVendas = formasPagamento.stream().mapToLong(FaturamentoFormaPagamentoResponse::quantidadeVendas).sum();
        final BigDecimal faturamento = formasPagamento.stream().map(FaturamentoFormaPagamentoResponse::faturamento)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        final long quantidadeCanceladas = relatorioRepository.countVendas(StatusVenda.CANCELADA, filtro.dataInicio(), filtro.dataFim());
        final BigDecimal lucro = faturamento.subtract(custo);
        return new FaturamentoResponse(filtro.dataInicio(), filtro.dataFim(), quantidadeVendas, quantidadeCanceladas, faturamento, custo, lucro,
                toMargemPercentual(lucro, faturamento), formasPagamento);
    }

    private static void ensurePeriodoValido(final RelatorioFiltroRequest filtro) {
        if (filtro.dataInicio().isAfter(filtro.dataFim())) {
            throw BusinessException.unprocessable(ErrorMessage.PERIODO_INVALIDO);
        }
    }

    private static MargemProdutoResponse toMargemProdutoResponse(final TotalProduto total, final Produto produto) {
        final BigDecimal custo = toCentavos(total.custo());
        final BigDecimal lucro = total.faturamento().subtract(custo);
        return new MargemProdutoResponse(total.produtoId(), produto == null ? null : produto.getNome(),
                produto == null ? null : produto.getUnidadeMedida(), total.quantidadeVendida(), total.faturamento(), custo, lucro,
                toMargemPercentual(lucro, total.faturamento()));
    }

    private static BigDecimal toCentavos(final BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal toMargemPercentual(final BigDecimal lucro, final BigDecimal faturamento) {
        if (faturamento.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return lucro.multiply(CEM).divide(faturamento, 2, RoundingMode.HALF_EVEN);
    }
}
