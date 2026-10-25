package br.com.gestaosupermercado.modules.produto.produto.service;

import br.com.gestaosupermercado.modules.produto.produto.dto.request.ProdutoFiltroRequest;
import br.com.gestaosupermercado.modules.produto.produto.dto.request.ProdutoRequest;
import br.com.gestaosupermercado.modules.produto.produto.dto.response.ProdutoResponse;
import br.com.gestaosupermercado.modules.produto.produto.model.Produto;
import br.com.gestaosupermercado.shared.dto.PaginaResponse;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import org.springframework.data.domain.Pageable;

public interface ProdutoService {

    PaginaResponse<ProdutoResponse> findAllProdutos(ProdutoFiltroRequest filtro, Pageable pageable, boolean incluirPrecoCusto);

    ProdutoResponse findProdutoById(Long id, boolean incluirPrecoCusto);

    ProdutoResponse saveProduto(ProdutoRequest request);

    ProdutoResponse updateProduto(Long id, ProdutoRequest request);

    void updateStatusProduto(Long id, boolean ativo);

    /**
     * Ponto de entrada para Estoque, Vendas e Compras: devolve a entidade para movimentar o estoque
     * ({@link Produto#addEstoque}/{@link Produto#removeEstoque}) dentro da transação de quem chama.
     * Falha se o produto não existir ou estiver inativo, ou se for chamado fora de uma transação.
     */
    Produto findProdutoAtivoForMovimentacao(Long id);

    /**
     * Como {@link #findProdutoAtivoForMovimentacao}, mas aceita produto inativo: devolver ao estoque o que saiu numa
     * venda cancelada não depende de o produto ainda estar à venda.
     */
    Produto findProdutoForEstorno(Long id);

    /**
     * Nome de cada produto por id, para as listagens de outros módulos que guardam só o id do produto.
     * Ids inexistentes ficam fora do mapa.
     */
    Map<Long, String> findNomesProdutosByIds(Collection<Long> ids);

    /**
     * Atualiza o preço de custo com o valor pago na compra mais recente, base da margem nos relatórios.
     * Chamado pelo módulo de compras dentro da transação da compra.
     */
    void updatePrecoCustoProduto(Long id, BigDecimal precoCusto);
}
