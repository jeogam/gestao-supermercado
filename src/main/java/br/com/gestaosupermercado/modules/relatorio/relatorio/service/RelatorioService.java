package br.com.gestaosupermercado.modules.relatorio.relatorio.service;

import br.com.gestaosupermercado.modules.relatorio.relatorio.dto.request.RelatorioFiltroRequest;
import br.com.gestaosupermercado.modules.relatorio.relatorio.dto.response.FaturamentoResponse;
import br.com.gestaosupermercado.modules.relatorio.relatorio.dto.response.MargemProdutoResponse;
import java.util.List;

public interface RelatorioService {

    FaturamentoResponse findFaturamento(RelatorioFiltroRequest filtro);

    List<MargemProdutoResponse> findMargemProdutos(RelatorioFiltroRequest filtro);

    /**
     * Faturamento e margem por produto do período, em PDF.
     */
    byte[] generateRelatorioPdf(RelatorioFiltroRequest filtro);
}
