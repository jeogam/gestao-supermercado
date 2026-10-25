package br.com.gestaosupermercado.modules.venda.venda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.OrigemMovimentacao;
import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.TipoMovimentacao;
import br.com.gestaosupermercado.modules.estoque.movimentacao.service.MovimentacaoEstoqueService;
import br.com.gestaosupermercado.modules.produto.produto.enums.UnidadeMedida;
import br.com.gestaosupermercado.modules.produto.produto.model.Produto;
import br.com.gestaosupermercado.modules.produto.produto.service.ProdutoService;
import br.com.gestaosupermercado.modules.venda.venda.dto.request.CancelamentoVendaRequest;
import br.com.gestaosupermercado.modules.venda.venda.dto.request.ItemVendaRequest;
import br.com.gestaosupermercado.modules.venda.venda.dto.request.VendaRequest;
import br.com.gestaosupermercado.modules.venda.venda.dto.response.VendaResponse;
import br.com.gestaosupermercado.modules.venda.venda.enums.FormaPagamento;
import br.com.gestaosupermercado.modules.venda.venda.enums.StatusVenda;
import br.com.gestaosupermercado.modules.venda.venda.model.Venda;
import br.com.gestaosupermercado.modules.venda.venda.repository.VendaRepository;
import br.com.gestaosupermercado.shared.exception.ErrorMessage;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VendaServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-25T15:00:00Z"), ZoneOffset.UTC);
    private static final Long CAIXA_ID = 3L;
    private static final Long VENDA_ID = 305L;

    @Mock
    private VendaRepository vendaRepository;

    @Mock
    private MovimentacaoEstoqueService movimentacaoEstoqueService;

    @Mock
    private ProdutoService produtoService;

    private VendaServiceImpl vendaService;

    @BeforeEach
    void setUp() {
        vendaService = new VendaServiceImpl(vendaRepository, movimentacaoEstoqueService, produtoService, CLOCK);
    }

    @Test
    void shouldUsePrecoDoCadastroAndGiveSaidaInEstoque() {
        when(produtoService.findProdutoAtivoForMovimentacao(1L)).thenReturn(newProduto(1L, "23.90", "18.50"));
        when(vendaRepository.save(any())).thenAnswer(invocation -> {
            final Venda venda = invocation.getArgument(0);
            ReflectionTestUtils.setField(venda, "id", VENDA_ID);
            return venda;
        });
        when(produtoService.findNomesProdutosByIds(anyCollection())).thenReturn(Map.of(1L, "Arroz"));

        final VendaResponse resposta = vendaService.saveVenda(new VendaRequest(FormaPagamento.DINHEIRO, new BigDecimal("50.00"),
                List.of(new ItemVendaRequest(1L, new BigDecimal("2")))), CAIXA_ID);

        verify(movimentacaoEstoqueService).saveMovimentacao(1L, TipoMovimentacao.SAIDA, new BigDecimal("2"), OrigemMovimentacao.VENDA,
                VENDA_ID, CAIXA_ID);
        assertThat(resposta.valorTotal()).isEqualByComparingTo("47.80");
        assertThat(resposta.troco()).isEqualByComparingTo("2.20");
        assertThat(resposta.itens().getFirst().precoUnitario()).isEqualByComparingTo("23.90");
        assertThat(resposta.itens().getFirst().produtoNome()).isEqualTo("Arroz");
    }

    @Test
    void shouldNotSaveWhenValorRecebidoInsuficiente() {
        when(produtoService.findProdutoAtivoForMovimentacao(1L)).thenReturn(newProduto(1L, "23.90", "18.50"));

        assertThatThrownBy(() -> vendaService.saveVenda(new VendaRequest(FormaPagamento.DINHEIRO, new BigDecimal("20.00"),
                List.of(new ItemVendaRequest(1L, new BigDecimal("2")))), CAIXA_ID))
                .hasMessage(ErrorMessage.VALOR_RECEBIDO_INSUFICIENTE.getDescricao());
        verifyNoInteractions(vendaRepository, movimentacaoEstoqueService);
    }

    @Test
    void shouldReturnItensToEstoqueWhenCancelled() {
        final Venda venda = new Venda(FormaPagamento.PIX, CAIXA_ID);
        venda.addItem(1L, new BigDecimal("2"), new BigDecimal("23.90"), new BigDecimal("18.50"));
        ReflectionTestUtils.setField(venda, "id", VENDA_ID);
        when(vendaRepository.findById(VENDA_ID)).thenReturn(Optional.of(venda));

        final VendaResponse resposta = vendaService.cancelVenda(VENDA_ID, new CancelamentoVendaRequest("Desistência"), 9L);

        verify(movimentacaoEstoqueService).saveMovimentacao(1L, TipoMovimentacao.ENTRADA, new BigDecimal("2"),
                OrigemMovimentacao.ESTORNO_VENDA, VENDA_ID, 9L);
        assertThat(resposta.status()).isEqualTo(StatusVenda.CANCELADA);
        assertThat(resposta.canceladoEm()).isEqualTo(OffsetDateTime.now(CLOCK));
        assertThat(resposta.canceladoPorUsuarioId()).isEqualTo(9L);
    }

    @Test
    void shouldNotReturnEstoqueTwiceWhenAlreadyCancelled() {
        final Venda venda = new Venda(FormaPagamento.PIX, CAIXA_ID);
        venda.addItem(1L, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE);
        venda.cancel(CAIXA_ID, null, OffsetDateTime.now(CLOCK));
        when(vendaRepository.findById(VENDA_ID)).thenReturn(Optional.of(venda));

        assertThatThrownBy(() -> vendaService.cancelVenda(VENDA_ID, new CancelamentoVendaRequest(null), CAIXA_ID))
                .hasMessage(ErrorMessage.VENDA_JA_CANCELADA.getDescricao());
        verifyNoInteractions(movimentacaoEstoqueService);
    }

    private static Produto newProduto(final Long id, final String precoVenda, final String precoCusto) {
        final Produto produto = new Produto("Arroz", null, UnidadeMedida.UN, new BigDecimal(precoCusto), new BigDecimal(precoVenda), BigDecimal.ZERO);
        produto.setId(id);
        return produto;
    }
}
