package br.com.gestaosupermercado.modules.relatorio.relatorio.service;

import br.com.gestaosupermercado.modules.relatorio.relatorio.dto.request.RelatorioFiltroRequest;
import br.com.gestaosupermercado.modules.relatorio.relatorio.dto.response.FaturamentoFormaPagamentoResponse;
import br.com.gestaosupermercado.modules.relatorio.relatorio.dto.response.FaturamentoResponse;
import br.com.gestaosupermercado.modules.relatorio.relatorio.dto.response.MargemProdutoResponse;
import br.com.gestaosupermercado.modules.venda.venda.enums.FormaPagamento;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Monta o PDF do relatório de vendas na identidade visual do front: cabeçalho escuro com a etiqueta amarela,
 * indicadores em destaque, uma seção por assunto e tabelas com números alinhados à direita e linha de total.
 * O rodapé de cada página repete o período e o número da página, para folhas soltas não perderem o contexto.
 *
 * @since 0.0.1
 */
final class RelatorioPdf {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    // Mesmos tokens do front (globals.css).
    private static final Color TINTA = new Color(0x17, 0x25, 0x1e);
    private static final Color VERDE = new Color(0x0e, 0x7a, 0x45);
    private static final Color ETIQUETA = new Color(0xff, 0xd6, 0x0a);
    private static final Color PERIGO = new Color(0xc0, 0x26, 0x2d);
    private static final Color APAGADO = new Color(0x5d, 0x6a, 0x63);
    private static final Color BORDA = new Color(0xdd, 0xe2, 0xdf);
    private static final Color FUNDO = new Color(0xf3, 0xf5, 0xf4);

    private static final Map<FormaPagamento, String> FORMAS = Map.of(
            FormaPagamento.DINHEIRO, "Dinheiro",
            FormaPagamento.CARTAO_DEBITO, "Cartão de débito",
            FormaPagamento.CARTAO_CREDITO, "Cartão de crédito",
            FormaPagamento.PIX, "Pix");

    private final NumberFormat moeda = NumberFormat.getCurrencyInstance(PT_BR);
    private final NumberFormat quantidade = NumberFormat.getNumberInstance(PT_BR);
    private final NumberFormat percentual = NumberFormat.getNumberInstance(PT_BR);

    private RelatorioPdf() {
        quantidade.setMaximumFractionDigits(3);
        percentual.setMinimumFractionDigits(1);
        percentual.setMaximumFractionDigits(1);
    }

    static byte[] gerar(final RelatorioFiltroRequest filtro, final FaturamentoResponse faturamento,
                        final List<MargemProdutoResponse> produtos) {
        return new RelatorioPdf().montar(filtro, faturamento, produtos);
    }

    private byte[] montar(final RelatorioFiltroRequest filtro, final FaturamentoResponse faturamento,
                          final List<MargemProdutoResponse> produtos) {
        // shortcut: o período sai só com as datas porque o front sempre pede dias inteiros; mostrar a hora se um dia vier um intervalo parcial.
        final String periodo = "%s a %s".formatted(filtro.dataInicio().format(DATA), filtro.dataFim().format(DATA));
        final ByteArrayOutputStream saida = new ByteArrayOutputStream();
        final Document documento = new Document(PageSize.A4, 40, 40, 40, 56);
        final PdfWriter writer = PdfWriter.getInstance(documento, saida);
        writer.setPageEvent(new Rodape(periodo));
        documento.open();

        documento.add(cabecalho(periodo, OffsetDateTime.now(filtro.dataInicio().getOffset())));
        documento.add(indicadores(faturamento));
        documento.add(new Paragraph("%s concluídas · %s".formatted(
                plural(faturamento.quantidadeVendas(), "venda", "vendas"),
                plural(faturamento.quantidadeVendasCanceladas(), "cancelada", "canceladas")), fonte(9, Font.NORMAL, APAGADO)));

        documento.add(tituloSecao("Faturamento por forma de pagamento",
                "Só vendas concluídas. A porcentagem é a parte de cada forma no faturamento do período."));
        documento.add(faturamento.formasPagamento().isEmpty() ? vazio() : tabelaFormas(faturamento));

        documento.add(tituloSecao("Margem por produto",
                "Do maior para o menor lucro. O custo é o de cada item no momento da venda."));
        documento.add(produtos.isEmpty() ? vazio() : tabelaProdutos(produtos, faturamento));

        documento.close();
        return saida.toByteArray();
    }

    private PdfPTable cabecalho(final String periodo, final OffsetDateTime geradoEm) {
        // Fundo numa célula só, com as duas colunas dentro: duas células pintadas deixavam uma emenda clara no meio.
        final PdfPTable colunas = new PdfPTable(new float[]{3, 2});
        colunas.setWidthPercentage(100);

        final Chunk marca = new Chunk(" Mercado ", fonte(10, Font.BOLD, TINTA));
        marca.setBackground(ETIQUETA, 1, 3, 1, 2);
        final PdfPCell esquerda = celulaSemBorda();
        esquerda.addElement(new Paragraph(marca));
        final Paragraph titulo = new Paragraph("Relatório de vendas", fonte(20, Font.BOLD, Color.WHITE));
        titulo.setSpacingBefore(8);
        esquerda.addElement(titulo);
        colunas.addCell(esquerda);

        final PdfPCell direita = celulaSemBorda();
        direita.setVerticalAlignment(Element.ALIGN_BOTTOM);
        direita.addElement(alinhadoDireita("PERÍODO", fonte(8, Font.BOLD, ETIQUETA)));
        direita.addElement(alinhadoDireita(periodo, fonte(12, Font.BOLD, Color.WHITE)));
        direita.addElement(alinhadoDireita("Gerado em " + geradoEm.format(DATA_HORA), fonte(8, Font.NORMAL, new Color(0xc3, 0xcb, 0xc6))));
        colunas.addCell(direita);

        final PdfPTable tabela = new PdfPTable(1);
        tabela.setWidthPercentage(100);
        final PdfPCell fundo = new PdfPCell(colunas);
        fundo.setBorder(Rectangle.NO_BORDER);
        fundo.setBackgroundColor(TINTA);
        fundo.setPadding(12);
        tabela.addCell(fundo);
        tabela.setSpacingAfter(16);
        return tabela;
    }

    private PdfPTable indicadores(final FaturamentoResponse faturamento) {
        // Colunas finas vazias entre os cartões: PdfPTable não tem espaçamento entre células.
        final PdfPTable tabela = new PdfPTable(new float[]{10, 0.6f, 10, 0.6f, 10, 0.6f, 10});
        tabela.setWidthPercentage(100);
        final boolean prejuizo = faturamento.lucro().signum() < 0;
        // O faturamento leva a etiqueta amarela, como no front: é o número que se procura primeiro.
        tabela.addCell(indicador("Faturamento", moeda.format(faturamento.faturamento()), TINTA, ETIQUETA));
        tabela.addCell(celulaSemBorda());
        tabela.addCell(indicador("Custo das mercadorias", moeda.format(faturamento.custo()), TINTA, null));
        tabela.addCell(celulaSemBorda());
        tabela.addCell(indicador(prejuizo ? "Prejuízo" : "Lucro", moeda.format(faturamento.lucro()), prejuizo ? PERIGO : VERDE, null));
        tabela.addCell(celulaSemBorda());
        tabela.addCell(indicador("Margem", porcento(faturamento.margemPercentual()), prejuizo ? PERIGO : TINTA, null));
        tabela.setSpacingAfter(6);
        return tabela;
    }

    private PdfPCell indicador(final String rotulo, final String valor, final Color cor, final Color destaque) {
        final PdfPCell celula = new PdfPCell();
        celula.setBorder(Rectangle.TOP);
        celula.setBorderWidthTop(3);
        celula.setBorderColorTop(destaque != null ? destaque : BORDA);
        celula.setBackgroundColor(FUNDO);
        celula.setPadding(10);
        celula.addElement(new Paragraph(rotulo, fonte(8, Font.NORMAL, APAGADO)));
        final Paragraph numero = new Paragraph(valor, fonte(15, Font.BOLD, cor));
        numero.setSpacingBefore(2);
        celula.addElement(numero);
        return celula;
    }

    private PdfPTable tituloSecao(final String titulo, final String descricao) {
        final PdfPTable tabela = new PdfPTable(1);
        tabela.setWidthPercentage(100);
        tabela.setSpacingBefore(20);
        tabela.setSpacingAfter(6);
        final PdfPCell celula = celulaSemBorda();
        celula.setBorder(Rectangle.BOTTOM);
        celula.setBorderWidthBottom(1.5f);
        celula.setBorderColorBottom(TINTA);
        celula.setPaddingBottom(5);
        celula.addElement(new Paragraph(titulo, fonte(13, Font.BOLD, TINTA)));
        celula.addElement(new Paragraph(descricao, fonte(8, Font.NORMAL, APAGADO)));
        tabela.addCell(celula);
        return tabela;
    }

    private PdfPTable tabelaFormas(final FaturamentoResponse faturamento) {
        final PdfPTable tabela = tabela(new float[]{4, 1.5f, 2, 2}, "Forma de pagamento", "Vendas", "Faturamento", "% do faturamento");
        int linha = 0;
        for (final FaturamentoFormaPagamentoResponse forma : faturamento.formasPagamento()) {
            final Color fundo = linha++ % 2 == 1 ? FUNDO : null;
            tabela.addCell(texto(FORMAS.getOrDefault(forma.formaPagamento(), forma.formaPagamento().name()), fundo));
            tabela.addCell(numero(String.valueOf(forma.quantidadeVendas()), fundo, TINTA, false));
            tabela.addCell(numero(moeda.format(forma.faturamento()), fundo, TINTA, false));
            tabela.addCell(numero(porcento(parte(forma.faturamento(), faturamento.faturamento())), fundo, TINTA, false));
        }
        tabela.addCell(total("Total"));
        tabela.addCell(numero(String.valueOf(faturamento.quantidadeVendas()), null, TINTA, true));
        tabela.addCell(numero(moeda.format(faturamento.faturamento()), null, TINTA, true));
        tabela.addCell(numero(porcento(BigDecimal.valueOf(100)), null, TINTA, true));
        return tabela;
    }

    private PdfPTable tabelaProdutos(final List<MargemProdutoResponse> produtos, final FaturamentoResponse faturamento) {
        final PdfPTable tabela = tabela(new float[]{4, 1.4f, 1.8f, 1.8f, 1.8f, 1.3f},
                "Produto", "Vendido", "Faturamento", "Custo", "Lucro", "Margem");
        int linha = 0;
        for (final MargemProdutoResponse produto : produtos) {
            final Color fundo = linha++ % 2 == 1 ? FUNDO : null;
            // Prejuízo em vermelho e com o sinal: a cor sozinha não basta para quem não distingue vermelho.
            final Color corLucro = produto.lucro().signum() < 0 ? PERIGO : TINTA;
            tabela.addCell(texto(produto.nome(), fundo));
            final String unidade = produto.unidadeMedida() == null ? "" : " " + produto.unidadeMedida().name().toLowerCase();
            tabela.addCell(numero(quantidade.format(produto.quantidadeVendida()) + unidade, fundo, TINTA, false));
            tabela.addCell(numero(moeda.format(produto.faturamento()), fundo, TINTA, false));
            tabela.addCell(numero(moeda.format(produto.custo()), fundo, TINTA, false));
            tabela.addCell(numero(moeda.format(produto.lucro()), fundo, corLucro, false));
            tabela.addCell(numero(porcento(produto.margemPercentual()), fundo, corLucro, false));
        }
        tabela.addCell(total("Total (%s)".formatted(plural(produtos.size(), "produto", "produtos"))));
        tabela.addCell(numero("", null, TINTA, true));
        tabela.addCell(numero(moeda.format(faturamento.faturamento()), null, TINTA, true));
        tabela.addCell(numero(moeda.format(faturamento.custo()), null, TINTA, true));
        final Color corTotal = faturamento.lucro().signum() < 0 ? PERIGO : TINTA;
        tabela.addCell(numero(moeda.format(faturamento.lucro()), null, corTotal, true));
        tabela.addCell(numero(porcento(faturamento.margemPercentual()), null, corTotal, true));
        return tabela;
    }

    /** Tabela com cabeçalho repetido em cada página; a primeira coluna é texto, as outras são números. */
    private PdfPTable tabela(final float[] larguras, final String... colunas) {
        final PdfPTable tabela = new PdfPTable(larguras);
        tabela.setWidthPercentage(100);
        tabela.setHeaderRows(1);
        for (int i = 0; i < colunas.length; i++) {
            final PdfPCell celula = new PdfPCell(new Phrase(colunas[i], fonte(8, Font.BOLD, APAGADO)));
            celula.setBorder(Rectangle.BOTTOM);
            celula.setBorderWidthBottom(1);
            celula.setBorderColorBottom(TINTA);
            celula.setPadding(6);
            celula.setHorizontalAlignment(i == 0 ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            tabela.addCell(celula);
        }
        return tabela;
    }

    private PdfPCell texto(final String valor, final Color fundo) {
        final PdfPCell celula = linha(new Phrase(valor, fonte(9, Font.NORMAL, TINTA)), fundo);
        celula.setHorizontalAlignment(Element.ALIGN_LEFT);
        return celula;
    }

    private PdfPCell numero(final String valor, final Color fundo, final Color cor, final boolean total) {
        final PdfPCell celula = total ? linhaTotal(new Phrase(valor, fonte(9, Font.BOLD, cor)))
                : linha(new Phrase(valor, fonte(9, Font.NORMAL, cor)), fundo);
        celula.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return celula;
    }

    private PdfPCell total(final String rotulo) {
        return linhaTotal(new Phrase(rotulo, fonte(9, Font.BOLD, TINTA)));
    }

    private static PdfPCell linha(final Phrase conteudo, final Color fundo) {
        final PdfPCell celula = new PdfPCell(conteudo);
        celula.setBorder(Rectangle.BOTTOM);
        celula.setBorderColorBottom(BORDA);
        celula.setPadding(6);
        if (fundo != null) {
            celula.setBackgroundColor(fundo);
        }
        return celula;
    }

    private static PdfPCell linhaTotal(final Phrase conteudo) {
        final PdfPCell celula = new PdfPCell(conteudo);
        celula.setBorder(Rectangle.TOP);
        celula.setBorderWidthTop(1.5f);
        celula.setBorderColorTop(TINTA);
        celula.setPadding(6);
        return celula;
    }

    private static Paragraph vazio() {
        return new Paragraph("Nenhuma venda concluída no período.", fonte(9, Font.ITALIC, APAGADO));
    }

    private static Paragraph alinhadoDireita(final String texto, final Font fonte) {
        final Paragraph paragrafo = new Paragraph(texto, fonte);
        paragrafo.setAlignment(Element.ALIGN_RIGHT);
        return paragrafo;
    }

    private static PdfPCell celulaSemBorda() {
        final PdfPCell celula = new PdfPCell();
        celula.setBorder(Rectangle.NO_BORDER);
        return celula;
    }

    private static Font fonte(final float tamanho, final int estilo, final Color cor) {
        return FontFactory.getFont(FontFactory.HELVETICA, tamanho, estilo, cor);
    }

    private String porcento(final BigDecimal valor) {
        return percentual.format(valor) + "%";
    }

    private static BigDecimal parte(final BigDecimal valor, final BigDecimal total) {
        return total.signum() == 0 ? BigDecimal.ZERO : valor.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_EVEN);
    }

    private static String plural(final long quantidade, final String singular, final String plural) {
        return quantidade + " " + (quantidade == 1 ? singular : plural);
    }

    /** Rodapé de toda página: de qual relatório é a folha e em que página está. */
    private static final class Rodape extends PdfPageEventHelper {

        private final String periodo;

        private Rodape(final String periodo) {
            this.periodo = periodo;
        }

        @Override
        public void onEndPage(final PdfWriter writer, final Document documento) {
            final PdfContentByte canvas = writer.getDirectContent();
            final float y = documento.bottom() - 20;
            canvas.setColorStroke(BORDA);
            canvas.setLineWidth(0.5f);
            canvas.moveTo(documento.left(), y + 12);
            canvas.lineTo(documento.right(), y + 12);
            canvas.stroke();
            final Font fonte = fonte(8, Font.NORMAL, APAGADO);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase("Mercado · Relatório de vendas · " + periodo, fonte), documento.left(), y, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT,
                    new Phrase("Página " + writer.getPageNumber(), fonte), documento.right(), y, 0);
        }
    }
}
