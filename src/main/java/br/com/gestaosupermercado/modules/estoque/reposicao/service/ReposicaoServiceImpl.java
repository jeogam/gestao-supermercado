package br.com.gestaosupermercado.modules.estoque.reposicao.service;

import br.com.gestaosupermercado.modules.estoque.reposicao.dto.response.ReposicaoResponse;
import br.com.gestaosupermercado.modules.produto.produto.dto.request.ProdutoFiltroRequest;
import br.com.gestaosupermercado.modules.produto.produto.dto.response.ProdutoResponse;
import br.com.gestaosupermercado.modules.produto.produto.service.ProdutoService;
import br.com.gestaosupermercado.shared.dto.PaginaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alerta de reposição: produtos ativos com estoque abaixo do mínimo. Produto inativo fica de fora, porque não
 * será mais vendido nem comprado.
 *
 * @since 0.0.1
 */
@Service
@RequiredArgsConstructor
public class ReposicaoServiceImpl implements ReposicaoService {

    private static final ProdutoFiltroRequest FILTRO_REPOSICAO = new ProdutoFiltroRequest(null, null, true, true);

    private final ProdutoService produtoService;

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<ReposicaoResponse> findAllProdutosParaRepor(final Pageable pageable) {
        final PaginaResponse<ProdutoResponse> produtos = produtoService.findAllProdutos(FILTRO_REPOSICAO, pageable, false);
        return new PaginaResponse<>(produtos.conteudo().stream().map(ReposicaoServiceImpl::toReposicaoResponse).toList(),
                produtos.pagina(), produtos.tamanho(), produtos.totalItens(), produtos.totalPaginas());
    }

    private static ReposicaoResponse toReposicaoResponse(final ProdutoResponse produto) {
        return new ReposicaoResponse(produto.id(), produto.nome(), produto.codigoBarras(), produto.unidadeMedida(), produto.quantidadeEstoque(),
                produto.estoqueMinimo(), produto.estoqueMinimo().subtract(produto.quantidadeEstoque()));
    }
}
