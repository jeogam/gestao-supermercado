package br.com.gestaosupermercado.modules.relatorio.relatorio.service;

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
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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
        final Map<Long, String> nomesProdutos = produtoService.findNomesProdutosByIds(totais.stream().map(TotalProduto::produtoId).toList());
        return totais.stream()
                .map(total -> toMargemProdutoResponse(total, nomesProdutos.get(total.produtoId())))
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
        final NumberFormat moeda = NumberFormat.getCurrencyInstance(PT_BR);
        final Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        final Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        final Font texto = FontFactory.getFont(FontFactory.HELVETICA, 10);
        final Font cabecalho = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

        final ByteArrayOutputStream saida = new ByteArrayOutputStream();
        final Document documento = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(documento, saida);
        documento.open();
        documento.add(new Paragraph("Relatório de vendas", titulo));
        documento.add(new Paragraph("Período: %s a %s".formatted(filtro.dataInicio().format(FORMATO_DATA), filtro.dataFim().format(FORMATO_DATA)),
                texto));
        documento.add(new Paragraph(" "));

        documento.add(new Paragraph("Resumo", subtitulo));
        final PdfPTable resumo = new PdfPTable(2);
        resumo.setWidthPercentage(60);
        resumo.setHorizontalAlignment(Element.ALIGN_LEFT);
        addLinha(resumo, texto, "Vendas concluídas", String.valueOf(faturamento.quantidadeVendas()));
        addLinha(resumo, texto, "Vendas canceladas", String.valueOf(faturamento.quantidadeVendasCanceladas()));
        addLinha(resumo, texto, "Faturamento", moeda.format(faturamento.faturamento()));
        addLinha(resumo, texto, "Custo", moeda.format(faturamento.custo()));
        addLinha(resumo, texto, "Lucro", moeda.format(faturamento.lucro()));
        addLinha(resumo, texto, "Margem", faturamento.margemPercentual() + "%");
        faturamento.formasPagamento().forEach(forma -> addLinha(resumo, texto, "  " + forma.formaPagamento(),
                "%d venda(s) · %s".formatted(forma.quantidadeVendas(), moeda.format(forma.faturamento()))));
        documento.add(resumo);
        documento.add(new Paragraph(" "));

        documento.add(new Paragraph("Margem por produto", subtitulo));
        final PdfPTable tabela = new PdfPTable(new float[]{4, 1.4f, 1.6f, 1.6f, 1.6f, 1.2f});
        tabela.setWidthPercentage(100);
        tabela.setHeaderRows(1);
        for (final String coluna : List.of("Produto", "Qtd.", "Faturamento", "Custo", "Lucro", "Margem")) {
            tabela.addCell(new PdfPCell(new Phrase(coluna, cabecalho)));
        }
        for (final MargemProdutoResponse produto : produtos) {
            tabela.addCell(new Phrase(produto.nome(), texto));
            addNumero(tabela, texto, produto.quantidadeVendida().stripTrailingZeros().toPlainString());
            addNumero(tabela, texto, moeda.format(produto.faturamento()));
            addNumero(tabela, texto, moeda.format(produto.custo()));
            addNumero(tabela, texto, moeda.format(produto.lucro()));
            addNumero(tabela, texto, produto.margemPercentual() + "%");
        }
        documento.add(tabela);
        documento.close();
        return saida.toByteArray();
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

    private static MargemProdutoResponse toMargemProdutoResponse(final TotalProduto total, final String nome) {
        final BigDecimal custo = toCentavos(total.custo());
        final BigDecimal lucro = total.faturamento().subtract(custo);
        return new MargemProdutoResponse(total.produtoId(), nome, total.quantidadeVendida(), total.faturamento(), custo, lucro,
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

    private static void addLinha(final PdfPTable tabela, final Font fonte, final String rotulo, final String valor) {
        tabela.addCell(new Phrase(rotulo, fonte));
        addNumero(tabela, fonte, valor);
    }

    private static void addNumero(final PdfPTable tabela, final Font fonte, final String valor) {
        final PdfPCell celula = new PdfPCell(new Phrase(valor, fonte));
        celula.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabela.addCell(celula);
    }
}
