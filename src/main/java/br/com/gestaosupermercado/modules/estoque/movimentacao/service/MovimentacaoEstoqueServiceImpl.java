package br.com.gestaosupermercado.modules.estoque.movimentacao.service;

import br.com.gestaosupermercado.modules.estoque.movimentacao.dto.request.AjusteEstoqueRequest;
import br.com.gestaosupermercado.modules.estoque.movimentacao.dto.request.MovimentacaoEstoqueFiltroRequest;
import br.com.gestaosupermercado.modules.estoque.movimentacao.dto.response.MovimentacaoEstoqueResponse;
import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.OrigemMovimentacao;
import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.TipoMovimentacao;
import br.com.gestaosupermercado.modules.estoque.movimentacao.model.MovimentacaoEstoque;
import br.com.gestaosupermercado.modules.estoque.movimentacao.repository.MovimentacaoEstoqueRepository;
import br.com.gestaosupermercado.modules.estoque.movimentacao.repository.specs.MovimentacaoEstoqueSpecs;
import br.com.gestaosupermercado.modules.produto.produto.model.Produto;
import br.com.gestaosupermercado.modules.produto.produto.service.ProdutoService;
import br.com.gestaosupermercado.shared.dto.PaginaResponse;
import br.com.gestaosupermercado.shared.exception.BusinessException;
import br.com.gestaosupermercado.shared.exception.ErrorMessage;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Movimentação de estoque. Toda alteração da quantidade de um produto passa por {@link #saveMovimentacao}, que
 * aplica as regras do {@link Produto} e grava o histórico com o saldo resultante.
 *
 * @since 0.0.1
 */
@Service
@RequiredArgsConstructor
public class MovimentacaoEstoqueServiceImpl implements MovimentacaoEstoqueService {

    private static final Set<String> CAMPOS_ORDENACAO = Set.of("criadoEm", "quantidade", "tipo", "origem", "produtoId");

    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final ProdutoService produtoService;

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<MovimentacaoEstoqueResponse> findAllMovimentacoes(final MovimentacaoEstoqueFiltroRequest filtro,
                                                                           final Pageable pageable) {
        if (filtro.dataInicio() != null && filtro.dataFim() != null && filtro.dataInicio().isAfter(filtro.dataFim())) {
            throw BusinessException.unprocessable(ErrorMessage.PERIODO_INVALIDO);
        }
        final Page<MovimentacaoEstoque> page = movimentacaoEstoqueRepository.findAll(MovimentacaoEstoqueSpecs.byFiltro(filtro),
                toPageableSeguro(pageable));
        // Uma consulta para os produtos da página inteira, em vez de uma por movimentação.
        final Map<Long, Produto> produtos = produtoService.findProdutosByIds(page.getContent().stream()
                .map(MovimentacaoEstoque::getProdutoId)
                .collect(Collectors.toSet()));
        return PaginaResponse.of(page.map(movimentacao ->
                toMovimentacaoEstoqueResponse(movimentacao, produtos.get(movimentacao.getProdutoId()))));
    }

    @Override
    @Transactional(readOnly = true)
    public MovimentacaoEstoqueResponse findMovimentacaoById(final Long id) {
        final MovimentacaoEstoque movimentacao = movimentacaoEstoqueRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorMessage.MOVIMENTACAO_NAO_ENCONTRADA));
        final Produto produto = produtoService.findProdutosByIds(Set.of(movimentacao.getProdutoId())).get(movimentacao.getProdutoId());
        return toMovimentacaoEstoqueResponse(movimentacao, produto);
    }

    @Override
    @Transactional
    public MovimentacaoEstoqueResponse saveAjuste(final AjusteEstoqueRequest request, final Long usuarioId) {
        final Produto produto = produtoService.findProdutoAtivoForMovimentacao(request.produtoId());
        final MovimentacaoEstoque movimentacao = movimentar(produto, request.tipo(), request.quantidade(), OrigemMovimentacao.AJUSTE,
                request.motivo(), null, usuarioId);
        return toMovimentacaoEstoqueResponse(movimentacao, produto);
    }

    // MANDATORY: o estoque só pode mudar junto com a compra/venda que o justifica.
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public MovimentacaoEstoque saveMovimentacao(final Long produtoId, final TipoMovimentacao tipo, final BigDecimal quantidade,
                                                final OrigemMovimentacao origem, final Long referenciaId, final Long usuarioId) {
        final Produto produto = origem == OrigemMovimentacao.ESTORNO_VENDA
                ? produtoService.findProdutoForEstorno(produtoId)
                : produtoService.findProdutoAtivoForMovimentacao(produtoId);
        return movimentar(produto, tipo, quantidade, origem, null, referenciaId, usuarioId);
    }

    private MovimentacaoEstoque movimentar(final Produto produto, final TipoMovimentacao tipo, final BigDecimal quantidade,
                                           final OrigemMovimentacao origem, final String motivo, final Long referenciaId,
                                           final Long usuarioId) {
        if (tipo == TipoMovimentacao.ENTRADA) {
            produto.addEstoque(quantidade);
        } else {
            produto.removeEstoque(quantidade);
        }
        return movimentacaoEstoqueRepository.save(new MovimentacaoEstoque(produto.getId(), tipo, origem, quantidade,
                produto.getQuantidadeEstoque(), motivo, referenciaId, usuarioId));
    }

    // Desempate por id: várias movimentações de uma mesma venda podem ter o mesmo criadoEm.
    private static Pageable toPageableSeguro(final Pageable pageable) {
        final Sort sort = Sort.by(pageable.getSort().stream()
                .filter(order -> CAMPOS_ORDENACAO.contains(order.getProperty()))
                .toList());
        final Sort ordenacao = (sort.isSorted() ? sort : Sort.by(Sort.Direction.DESC, "criadoEm")).and(Sort.by(Sort.Direction.DESC, "id"));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ordenacao);
    }

    private static MovimentacaoEstoqueResponse toMovimentacaoEstoqueResponse(final MovimentacaoEstoque movimentacao, final Produto produto) {
        return new MovimentacaoEstoqueResponse(movimentacao.getId(), movimentacao.getProdutoId(),
                produto == null ? null : produto.getNome(), produto == null ? null : produto.getUnidadeMedida(), movimentacao.getTipo(),
                movimentacao.getOrigem(), movimentacao.getQuantidade(), movimentacao.getSaldoApos(), movimentacao.getMotivo(),
                movimentacao.getReferenciaId(), movimentacao.getUsuarioId(), movimentacao.getCriadoEm());
    }
}
