package br.com.gestaosupermercado.modules.venda.venda.service;

import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.OrigemMovimentacao;
import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.TipoMovimentacao;
import br.com.gestaosupermercado.modules.estoque.movimentacao.service.MovimentacaoEstoqueService;
import br.com.gestaosupermercado.modules.produto.produto.model.Produto;
import br.com.gestaosupermercado.modules.produto.produto.service.ProdutoService;
import br.com.gestaosupermercado.modules.venda.venda.dto.request.CancelamentoVendaRequest;
import br.com.gestaosupermercado.modules.venda.venda.dto.request.ItemVendaRequest;
import br.com.gestaosupermercado.modules.venda.venda.dto.request.VendaFiltroRequest;
import br.com.gestaosupermercado.modules.venda.venda.dto.request.VendaRequest;
import br.com.gestaosupermercado.modules.venda.venda.dto.response.ItemVendaResponse;
import br.com.gestaosupermercado.modules.venda.venda.dto.response.VendaResponse;
import br.com.gestaosupermercado.modules.venda.venda.dto.response.VendaResumoResponse;
import br.com.gestaosupermercado.modules.venda.venda.model.ItemVenda;
import br.com.gestaosupermercado.modules.venda.venda.model.Venda;
import br.com.gestaosupermercado.modules.venda.venda.repository.VendaRepository;
import br.com.gestaosupermercado.modules.venda.venda.repository.specs.VendaSpecs;
import br.com.gestaosupermercado.shared.dto.PaginaResponse;
import br.com.gestaosupermercado.shared.exception.BusinessException;
import br.com.gestaosupermercado.shared.exception.ErrorMessage;
import java.time.Clock;
import java.time.OffsetDateTime;
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
 * Vendas do caixa. Registrar dá baixa no estoque de cada item; cancelar (ou "desfazer") devolve os itens ao estoque.
 * As duas operações gravam venda e estoque na mesma transação: ou tudo é gravado, ou nada.
 *
 * @since 0.0.1
 */
@Service
@RequiredArgsConstructor
public class VendaServiceImpl implements VendaService {

    private static final Set<String> CAMPOS_ORDENACAO = Set.of("criadoEm", "valorTotal", "status", "formaPagamento");

    private final VendaRepository vendaRepository;
    private final MovimentacaoEstoqueService movimentacaoEstoqueService;
    private final ProdutoService produtoService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<VendaResumoResponse> findAllVendas(final VendaFiltroRequest filtro, final Pageable pageable) {
        if (filtro.dataInicio() != null && filtro.dataFim() != null && filtro.dataInicio().isAfter(filtro.dataFim())) {
            throw BusinessException.unprocessable(ErrorMessage.PERIODO_INVALIDO);
        }
        return PaginaResponse.of(vendaRepository.findAll(VendaSpecs.byFiltro(filtro), toPageableSeguro(pageable))
                .map(VendaServiceImpl::toVendaResumoResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public VendaResponse findVendaById(final Long id) {
        return toVendaResponse(findVendaEntityById(id));
    }

    @Override
    @Transactional
    public VendaResponse saveVenda(final VendaRequest request, final Long usuarioId) {
        final Venda venda = new Venda(request.formaPagamento(), usuarioId);
        for (final ItemVendaRequest item : request.itens()) {
            // Preço e custo vêm do cadastro: o caixa não digita preço.
            final Produto produto = produtoService.findProdutoAtivoForMovimentacao(item.produtoId());
            venda.addItem(produto.getId(), item.quantidade(), produto.getPrecoVenda(), produto.getPrecoCusto());
        }
        venda.registerValorRecebido(request.valorRecebido());
        // Salva antes de movimentar: o histórico de estoque guarda o id da venda.
        vendaRepository.save(venda);
        for (final ItemVenda item : venda.getItens()) {
            movimentacaoEstoqueService.saveMovimentacao(item.getProdutoId(), TipoMovimentacao.SAIDA, item.getQuantidade(),
                    OrigemMovimentacao.VENDA, venda.getId(), usuarioId);
        }
        return toVendaResponse(venda);
    }

    /**
     * Serve tanto para cancelar uma venda antiga quanto para o "desfazer" logo após a confirmação (ADR-001, item 3).
     */
    @Override
    @Transactional
    public VendaResponse cancelVenda(final Long id, final CancelamentoVendaRequest request, final Long usuarioId) {
        final Venda venda = findVendaEntityById(id);
        venda.cancel(usuarioId, request.motivo(), OffsetDateTime.now(clock));
        for (final ItemVenda item : venda.getItens()) {
            movimentacaoEstoqueService.saveMovimentacao(item.getProdutoId(), TipoMovimentacao.ENTRADA, item.getQuantidade(),
                    OrigemMovimentacao.ESTORNO_VENDA, venda.getId(), usuarioId);
        }
        return toVendaResponse(venda);
    }

    private Venda findVendaEntityById(final Long id) {
        return vendaRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorMessage.VENDA_NAO_ENCONTRADA));
    }

    // Desempate por id: vendas registradas no mesmo instante mantêm ordem estável entre páginas.
    private static Pageable toPageableSeguro(final Pageable pageable) {
        final Sort sort = Sort.by(pageable.getSort().stream()
                .filter(order -> CAMPOS_ORDENACAO.contains(order.getProperty()))
                .toList());
        final Sort ordenacao = (sort.isSorted() ? sort : Sort.by(Sort.Direction.DESC, "criadoEm")).and(Sort.by(Sort.Direction.DESC, "id"));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ordenacao);
    }

    private VendaResponse toVendaResponse(final Venda venda) {
        final Map<Long, String> nomesProdutos = produtoService.findNomesProdutosByIds(venda.getItens().stream()
                .map(ItemVenda::getProdutoId)
                .collect(Collectors.toSet()));
        return new VendaResponse(venda.getId(), venda.getStatus(), venda.getFormaPagamento(), venda.getValorTotal(), venda.getValorRecebido(),
                venda.getTroco(), venda.getUsuarioId(), venda.getCriadoEm(), venda.getCanceladoEm(), venda.getCanceladoPorUsuarioId(),
                venda.getMotivoCancelamento(),
                venda.getItens().stream()
                        .map(item -> new ItemVendaResponse(item.getProdutoId(), nomesProdutos.get(item.getProdutoId()), item.getQuantidade(),
                                item.getPrecoUnitario(), item.getSubtotal()))
                        .toList());
    }

    private static VendaResumoResponse toVendaResumoResponse(final Venda venda) {
        return new VendaResumoResponse(venda.getId(), venda.getStatus(), venda.getFormaPagamento(), venda.getValorTotal(),
                venda.getUsuarioId(), venda.getCriadoEm());
    }
}
