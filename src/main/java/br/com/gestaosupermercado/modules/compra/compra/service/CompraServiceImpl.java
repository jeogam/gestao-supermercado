package br.com.gestaosupermercado.modules.compra.compra.service;

import br.com.gestaosupermercado.modules.compra.compra.dto.request.CompraFiltroRequest;
import br.com.gestaosupermercado.modules.compra.compra.dto.request.CompraRequest;
import br.com.gestaosupermercado.modules.compra.compra.dto.request.ItemCompraRequest;
import br.com.gestaosupermercado.modules.compra.compra.dto.response.CompraResponse;
import br.com.gestaosupermercado.modules.compra.compra.dto.response.CompraResumoResponse;
import br.com.gestaosupermercado.modules.compra.compra.dto.response.ItemCompraResponse;
import br.com.gestaosupermercado.modules.compra.compra.model.Compra;
import br.com.gestaosupermercado.modules.compra.compra.model.ItemCompra;
import br.com.gestaosupermercado.modules.compra.compra.repository.CompraRepository;
import br.com.gestaosupermercado.modules.compra.compra.repository.specs.CompraSpecs;
import br.com.gestaosupermercado.modules.compra.fornecedor.model.Fornecedor;
import br.com.gestaosupermercado.modules.compra.fornecedor.service.FornecedorService;
import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.OrigemMovimentacao;
import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.TipoMovimentacao;
import br.com.gestaosupermercado.modules.estoque.movimentacao.service.MovimentacaoEstoqueService;
import br.com.gestaosupermercado.modules.produto.produto.service.ProdutoService;
import br.com.gestaosupermercado.shared.dto.PaginaResponse;
import br.com.gestaosupermercado.shared.exception.BusinessException;
import br.com.gestaosupermercado.shared.exception.ErrorMessage;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compras de fornecedores. Registrar uma compra dá entrada no estoque de cada item e atualiza o preço de custo do
 * produto, tudo na mesma transação: se um item falhar (produto inativo, quantidade inválida...), nada é gravado.
 *
 * @since 0.0.1
 */
@Service
@RequiredArgsConstructor
public class CompraServiceImpl implements CompraService {

    private static final Set<String> CAMPOS_ORDENACAO = Set.of("criadoEm", "valorTotal", "numeroNotaFiscal");

    private final CompraRepository compraRepository;
    private final FornecedorService fornecedorService;
    private final MovimentacaoEstoqueService movimentacaoEstoqueService;
    private final ProdutoService produtoService;

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<CompraResumoResponse> findAllCompras(final CompraFiltroRequest filtro, final Pageable pageable) {
        if (filtro.dataInicio() != null && filtro.dataFim() != null && filtro.dataInicio().isAfter(filtro.dataFim())) {
            throw BusinessException.unprocessable(ErrorMessage.PERIODO_INVALIDO);
        }
        return PaginaResponse.of(compraRepository.findAll(CompraSpecs.byFiltro(filtro), toPageableSeguro(pageable))
                .map(CompraServiceImpl::toCompraResumoResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public CompraResponse findCompraById(final Long id) {
        final Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorMessage.COMPRA_NAO_ENCONTRADA));
        return toCompraResponse(compra);
    }

    @Override
    @Transactional
    public CompraResponse saveCompra(final CompraRequest request, final Long usuarioId) {
        final Fornecedor fornecedor = fornecedorService.findFornecedorAtivoForCompra(request.fornecedorId());
        final Compra compra = new Compra(fornecedor, normalizeOpcional(request.numeroNotaFiscal()), normalizeOpcional(request.observacao()),
                usuarioId);
        for (final ItemCompraRequest item : request.itens()) {
            compra.addItem(item.produtoId(), item.quantidade(), item.precoCustoUnitario());
        }
        // Valida os produtos antes de gravar: um id inexistente responderia 404, e não erro de chave estrangeira.
        compra.getItens().forEach(item -> produtoService.findProdutoAtivoForMovimentacao(item.getProdutoId()));
        // Salva antes de movimentar: o histórico de estoque guarda o id da compra.
        compraRepository.save(compra);
        for (final ItemCompra item : compra.getItens()) {
            movimentacaoEstoqueService.saveMovimentacao(item.getProdutoId(), TipoMovimentacao.ENTRADA, item.getQuantidade(),
                    OrigemMovimentacao.COMPRA, compra.getId(), usuarioId);
            produtoService.updatePrecoCustoProduto(item.getProdutoId(), item.getPrecoCustoUnitario());
        }
        return toCompraResponse(compra);
    }

    // Desempate por id: duas compras lançadas no mesmo instante mantêm ordem estável entre páginas.
    private static Pageable toPageableSeguro(final Pageable pageable) {
        final Sort sort = Sort.by(pageable.getSort().stream()
                .filter(order -> CAMPOS_ORDENACAO.contains(order.getProperty()))
                .toList());
        final Sort ordenacao = (sort.isSorted() ? sort : Sort.by(Sort.Direction.DESC, "criadoEm")).and(Sort.by(Sort.Direction.DESC, "id"));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ordenacao);
    }

    private static String normalizeOpcional(final String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    private CompraResponse toCompraResponse(final Compra compra) {
        final Map<Long, String> nomesProdutos = produtoService.findNomesProdutosByIds(compra.getItens().stream()
                .map(ItemCompra::getProdutoId)
                .collect(Collectors.toSet()));
        return new CompraResponse(compra.getId(), compra.getFornecedor().getId(), compra.getFornecedor().getNome(), compra.getNumeroNotaFiscal(),
                compra.getValorTotal(), compra.getObservacao(), compra.getUsuarioId(), compra.getCriadoEm(),
                compra.getItens().stream()
                        .map(item -> new ItemCompraResponse(item.getProdutoId(), nomesProdutos.get(item.getProdutoId()), item.getQuantidade(),
                                item.getPrecoCustoUnitario(), item.getSubtotal()))
                        .toList());
    }

    private static CompraResumoResponse toCompraResumoResponse(final Compra compra) {
        return new CompraResumoResponse(compra.getId(), compra.getFornecedor().getId(), compra.getFornecedor().getNome(),
                compra.getNumeroNotaFiscal(), compra.getValorTotal(), compra.getUsuarioId(), compra.getCriadoEm());
    }
}
