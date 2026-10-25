package br.com.gestaosupermercado.modules.compra.compra.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.gestaosupermercado.modules.compra.compra.dto.request.CompraRequest;
import br.com.gestaosupermercado.modules.compra.compra.dto.request.ItemCompraRequest;
import br.com.gestaosupermercado.modules.compra.compra.dto.response.CompraResponse;
import br.com.gestaosupermercado.modules.compra.compra.model.Compra;
import br.com.gestaosupermercado.modules.compra.compra.repository.CompraRepository;
import br.com.gestaosupermercado.modules.compra.fornecedor.model.Fornecedor;
import br.com.gestaosupermercado.modules.compra.fornecedor.service.FornecedorService;
import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.OrigemMovimentacao;
import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.TipoMovimentacao;
import br.com.gestaosupermercado.modules.estoque.movimentacao.service.MovimentacaoEstoqueService;
import br.com.gestaosupermercado.modules.produto.produto.service.ProdutoService;
import br.com.gestaosupermercado.shared.exception.BusinessException;
import br.com.gestaosupermercado.shared.exception.ErrorMessage;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CompraServiceImplTest {

    private static final Long USUARIO_ID = 3L;
    private static final Long COMPRA_ID = 48L;

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private FornecedorService fornecedorService;

    @Mock
    private MovimentacaoEstoqueService movimentacaoEstoqueService;

    @Mock
    private ProdutoService produtoService;

    private CompraServiceImpl compraService;

    @BeforeEach
    void setUp() {
        compraService = new CompraServiceImpl(compraRepository, fornecedorService, movimentacaoEstoqueService, produtoService);
    }

    @Test
    void shouldGiveEntradaInEstoqueAndUpdateCustoForEachItem() {
        final Fornecedor fornecedor = new Fornecedor("Atacadão", "12345678000190", null, null);
        ReflectionTestUtils.setField(fornecedor, "id", 4L);
        when(fornecedorService.findFornecedorAtivoForCompra(4L)).thenReturn(fornecedor);
        when(compraRepository.save(any())).thenAnswer(invocation -> {
            final Compra compra = invocation.getArgument(0);
            ReflectionTestUtils.setField(compra, "id", COMPRA_ID);
            return compra;
        });
        when(produtoService.findNomesProdutosByIds(anyCollection())).thenReturn(Map.of(1L, "Arroz", 2L, "Feijão"));

        final CompraResponse resposta = compraService.saveCompra(new CompraRequest(4L, " 000123 ", "  ", List.of(
                new ItemCompraRequest(1L, new BigDecimal("10"), new BigDecimal("18.50")),
                new ItemCompraRequest(2L, new BigDecimal("5"), new BigDecimal("6.00")))), USUARIO_ID);

        verify(movimentacaoEstoqueService).saveMovimentacao(1L, TipoMovimentacao.ENTRADA, new BigDecimal("10"), OrigemMovimentacao.COMPRA,
                COMPRA_ID, USUARIO_ID);
        verify(movimentacaoEstoqueService).saveMovimentacao(2L, TipoMovimentacao.ENTRADA, new BigDecimal("5"), OrigemMovimentacao.COMPRA,
                COMPRA_ID, USUARIO_ID);
        verify(produtoService).updatePrecoCustoProduto(1L, new BigDecimal("18.50"));
        verify(produtoService).updatePrecoCustoProduto(2L, new BigDecimal("6.00"));
        assertThat(resposta.id()).isEqualTo(COMPRA_ID);
        assertThat(resposta.valorTotal()).isEqualByComparingTo("215.00");
        assertThat(resposta.numeroNotaFiscal()).isEqualTo("000123");
        assertThat(resposta.observacao()).isNull();
        assertThat(resposta.fornecedorNome()).isEqualTo("Atacadão");
        assertThat(resposta.itens()).extracting("produtoNome").containsExactly("Arroz", "Feijão");
    }

    @Test
    void shouldNotTouchEstoqueWhenFornecedorInativo() {
        when(fornecedorService.findFornecedorAtivoForCompra(4L)).thenThrow(BusinessException.unprocessable(ErrorMessage.FORNECEDOR_INATIVO));

        assertThatThrownBy(() -> compraService.saveCompra(new CompraRequest(4L, null, null,
                List.of(new ItemCompraRequest(1L, BigDecimal.ONE, BigDecimal.TEN))), USUARIO_ID))
                .hasMessage(ErrorMessage.FORNECEDOR_INATIVO.getDescricao());
        verifyNoInteractions(compraRepository, movimentacaoEstoqueService, produtoService);
    }

    @Test
    void shouldValidateProdutosBeforeSavingCompra() {
        when(fornecedorService.findFornecedorAtivoForCompra(4L)).thenReturn(new Fornecedor("Atacadão", null, null, null));
        when(produtoService.findProdutoAtivoForMovimentacao(999L)).thenThrow(BusinessException.notFound(ErrorMessage.PRODUTO_NAO_ENCONTRADO));

        assertThatThrownBy(() -> compraService.saveCompra(new CompraRequest(4L, null, null,
                List.of(new ItemCompraRequest(999L, BigDecimal.ONE, BigDecimal.TEN))), USUARIO_ID))
                .hasMessage(ErrorMessage.PRODUTO_NAO_ENCONTRADO.getDescricao());
        verifyNoInteractions(compraRepository, movimentacaoEstoqueService);
    }

    @Test
    void shouldRejectProdutoRepetidoBeforeSaving() {
        when(fornecedorService.findFornecedorAtivoForCompra(4L)).thenReturn(new Fornecedor("Atacadão", null, null, null));

        assertThatThrownBy(() -> compraService.saveCompra(new CompraRequest(4L, null, null, List.of(
                new ItemCompraRequest(1L, BigDecimal.ONE, BigDecimal.TEN),
                new ItemCompraRequest(1L, BigDecimal.TWO, BigDecimal.TEN))), USUARIO_ID))
                .hasMessage(ErrorMessage.PRODUTO_REPETIDO_NA_COMPRA.getDescricao());
        verifyNoInteractions(compraRepository, movimentacaoEstoqueService);
    }
}
