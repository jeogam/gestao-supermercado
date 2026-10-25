package br.com.gestaosupermercado.modules.produto.produto.service;

import br.com.gestaosupermercado.modules.produto.produto.dto.request.ProdutoFiltroRequest;
import br.com.gestaosupermercado.modules.produto.produto.dto.request.ProdutoRequest;
import br.com.gestaosupermercado.modules.produto.produto.dto.response.ProdutoResponse;
import br.com.gestaosupermercado.modules.produto.produto.model.Produto;
import br.com.gestaosupermercado.modules.produto.produto.repository.ProdutoRepository;
import br.com.gestaosupermercado.modules.produto.produto.repository.specs.ProdutoSpecs;
import br.com.gestaosupermercado.shared.dto.PaginaResponse;
import br.com.gestaosupermercado.shared.exception.BusinessException;
import br.com.gestaosupermercado.shared.exception.ErrorMessage;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro de produtos. A quantidade em estoque não é editada aqui: ela muda só pelas movimentações
 * (compra, venda, ajuste) feitas nos módulos que usam {@link #findProdutoAtivoForMovimentacao}.
 *
 * @since 0.0.1
 */
@Service
@RequiredArgsConstructor
public class ProdutoServiceImpl implements ProdutoService {

    private static final Set<String> CAMPOS_ORDENACAO = Set.of("nome", "codigoBarras", "precoVenda", "quantidadeEstoque", "ativo",
            "criadoEm");

    private final ProdutoRepository produtoRepository;

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<ProdutoResponse> findAllProdutos(final ProdutoFiltroRequest filtro, final Pageable pageable,
                                                           final boolean incluirPrecoCusto) {
        return PaginaResponse.of(produtoRepository.findAll(ProdutoSpecs.byFiltro(filtro), toPageableSeguro(pageable))
                .map(produto -> toProdutoResponse(produto, incluirPrecoCusto)));
    }

    @Override
    @Transactional(readOnly = true)
    public ProdutoResponse findProdutoById(final Long id, final boolean incluirPrecoCusto) {
        return toProdutoResponse(findProdutoEntityById(id), incluirPrecoCusto);
    }

    @Override
    @Transactional
    public ProdutoResponse saveProduto(final ProdutoRequest request) {
        final String codigoBarras = normalizeCodigoBarras(request.codigoBarras());
        if (codigoBarras != null && produtoRepository.existsByCodigoBarras(codigoBarras)) {
            throw BusinessException.conflict(ErrorMessage.CODIGO_BARRAS_JA_CADASTRADO);
        }
        final Produto produto = new Produto(request.nome().strip(), codigoBarras, request.unidadeMedida(), request.precoCusto(),
                request.precoVenda(), request.estoqueMinimo());
        return toProdutoResponse(produtoRepository.save(produto), true);
    }

    @Override
    @Transactional
    public ProdutoResponse updateProduto(final Long id, final ProdutoRequest request) {
        final Produto produto = findProdutoEntityById(id);
        final String codigoBarras = normalizeCodigoBarras(request.codigoBarras());
        if (codigoBarras != null && produtoRepository.existsByCodigoBarrasAndIdNot(codigoBarras, id)) {
            throw BusinessException.conflict(ErrorMessage.CODIGO_BARRAS_JA_CADASTRADO);
        }
        produto.setNome(request.nome().strip());
        produto.setCodigoBarras(codigoBarras);
        produto.updateUnidadeMedida(request.unidadeMedida());
        produto.setPrecoCusto(request.precoCusto());
        produto.setPrecoVenda(request.precoVenda());
        produto.setEstoqueMinimo(request.estoqueMinimo());
        return toProdutoResponse(produto, true);
    }

    /**
     * Inativar em vez de excluir: vendas e relatórios antigos continuam apontando para o produto.
     */
    @Override
    @Transactional
    public void updateStatusProduto(final Long id, final boolean ativo) {
        findProdutoEntityById(id).setAtivo(ativo);
    }

    // MANDATORY: fora de uma transação o produto voltaria desanexado e a movimentação não seria salva.
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Produto findProdutoAtivoForMovimentacao(final Long id) {
        final Produto produto = findProdutoEntityById(id);
        if (!produto.isAtivo()) {
            throw BusinessException.unprocessable(ErrorMessage.PRODUTO_INATIVO);
        }
        return produto;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> findNomesProdutosByIds(final Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return produtoRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Produto::getId, Produto::getNome));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void updatePrecoCustoProduto(final Long id, final BigDecimal precoCusto) {
        findProdutoEntityById(id).setPrecoCusto(precoCusto);
    }

    private Produto findProdutoEntityById(final Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorMessage.PRODUTO_NAO_ENCONTRADO));
    }

    private static Pageable toPageableSeguro(final Pageable pageable) {
        final Sort sort = Sort.by(pageable.getSort().stream()
                .filter(order -> CAMPOS_ORDENACAO.contains(order.getProperty()))
                .toList());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort.isSorted() ? sort : Sort.by("nome"));
    }

    // Código em branco vira null: a constraint UNIQUE do banco permite vários nulls, mas não vários "".
    private static String normalizeCodigoBarras(final String codigoBarras) {
        return codigoBarras == null || codigoBarras.isBlank() ? null : codigoBarras.strip();
    }

    /**
     * Custo só para quem gerencia produtos: com custo e preço de venda o Caixa calcularia a margem, que a
     * ADR-001 reserva aos relatórios.
     */
    private static ProdutoResponse toProdutoResponse(final Produto produto, final boolean incluirPrecoCusto) {
        return new ProdutoResponse(produto.getId(), produto.getNome(), produto.getCodigoBarras(), produto.getUnidadeMedida(),
                incluirPrecoCusto ? produto.getPrecoCusto() : null, produto.getPrecoVenda(), produto.getQuantidadeEstoque(), produto.getEstoqueMinimo(),
                produto.isAbaixoEstoqueMinimo(), produto.isAtivo(), produto.getCriadoEm());
    }
}
