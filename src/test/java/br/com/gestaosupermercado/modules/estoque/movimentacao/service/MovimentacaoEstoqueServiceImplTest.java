package br.com.gestaosupermercado.modules.estoque.movimentacao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.gestaosupermercado.modules.estoque.movimentacao.dto.request.AjusteEstoqueRequest;
import br.com.gestaosupermercado.modules.estoque.movimentacao.dto.request.MovimentacaoEstoqueFiltroRequest;
import br.com.gestaosupermercado.modules.estoque.movimentacao.dto.response.MovimentacaoEstoqueResponse;
import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.OrigemMovimentacao;
import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.TipoMovimentacao;
import br.com.gestaosupermercado.modules.estoque.movimentacao.model.MovimentacaoEstoque;
import br.com.gestaosupermercado.modules.estoque.movimentacao.repository.MovimentacaoEstoqueRepository;
import br.com.gestaosupermercado.modules.produto.produto.enums.UnidadeMedida;
import br.com.gestaosupermercado.modules.produto.produto.model.Produto;
import br.com.gestaosupermercado.modules.produto.produto.service.ProdutoService;
import br.com.gestaosupermercado.shared.exception.BusinessException;
import br.com.gestaosupermercado.shared.exception.ErrorMessage;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class MovimentacaoEstoqueServiceImplTest {

    private static final Long PRODUTO_ID = 12L;
    private static final Long USUARIO_ID = 3L;

    @Mock
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    @Mock
    private ProdutoService produtoService;

    private MovimentacaoEstoqueServiceImpl movimentacaoEstoqueService;

    @BeforeEach
    void setUp() {
        movimentacaoEstoqueService = new MovimentacaoEstoqueServiceImpl(movimentacaoEstoqueRepository, produtoService);
    }

    @Test
    void shouldAddEstoqueAndRecordSaldoWhenAjusteEntrada() {
        final Produto produto = newProduto(new BigDecimal("5"));
        when(produtoService.findProdutoAtivoForMovimentacao(PRODUTO_ID)).thenReturn(produto);
        when(movimentacaoEstoqueRepository.save(any())).then(AdditionalAnswers.returnsFirstArg());

        final MovimentacaoEstoqueResponse resposta = movimentacaoEstoqueService.saveAjuste(
                new AjusteEstoqueRequest(PRODUTO_ID, TipoMovimentacao.ENTRADA, new BigDecimal("3"), "Contagem de inventário"), USUARIO_ID);

        assertThat(produto.getQuantidadeEstoque()).isEqualByComparingTo("8");
        assertThat(resposta.saldoApos()).isEqualByComparingTo("8");
        assertThat(resposta.origem()).isEqualTo(OrigemMovimentacao.AJUSTE);
        assertThat(resposta.produtoNome()).isEqualTo("Arroz");
        assertThat(resposta.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(resposta.motivo()).isEqualTo("Contagem de inventário");
    }

    @Test
    void shouldNotRecordMovimentacaoWhenEstoqueInsuficiente() {
        final Produto produto = newProduto(new BigDecimal("2"));
        when(produtoService.findProdutoAtivoForMovimentacao(PRODUTO_ID)).thenReturn(produto);

        assertThatThrownBy(() -> movimentacaoEstoqueService.saveAjuste(
                new AjusteEstoqueRequest(PRODUTO_ID, TipoMovimentacao.SAIDA, new BigDecimal("3"), "Avaria"), USUARIO_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorMessage.ESTOQUE_INSUFICIENTE.getDescricao());
        assertThat(produto.getQuantidadeEstoque()).isEqualByComparingTo("2");
        verify(movimentacaoEstoqueRepository, never()).save(any());
    }

    @Test
    void shouldRecordReferenciaAndOrigemWhenCalledByOtherModule() {
        final Produto produto = newProduto(new BigDecimal("10"));
        when(produtoService.findProdutoAtivoForMovimentacao(PRODUTO_ID)).thenReturn(produto);
        when(movimentacaoEstoqueRepository.save(any())).then(AdditionalAnswers.returnsFirstArg());

        final MovimentacaoEstoque movimentacao = movimentacaoEstoqueService.saveMovimentacao(PRODUTO_ID, TipoMovimentacao.SAIDA,
                new BigDecimal("4"), OrigemMovimentacao.VENDA, 48L, USUARIO_ID);

        assertThat(movimentacao.getOrigem()).isEqualTo(OrigemMovimentacao.VENDA);
        assertThat(movimentacao.getReferenciaId()).isEqualTo(48L);
        assertThat(movimentacao.getSaldoApos()).isEqualByComparingTo("6");
        assertThat(movimentacao.getMotivo()).isNull();
    }

    @Test
    void shouldPropagateProdutoInativo() {
        when(produtoService.findProdutoAtivoForMovimentacao(PRODUTO_ID))
                .thenThrow(BusinessException.unprocessable(ErrorMessage.PRODUTO_INATIVO));

        assertThatThrownBy(() -> movimentacaoEstoqueService.saveAjuste(
                new AjusteEstoqueRequest(PRODUTO_ID, TipoMovimentacao.ENTRADA, BigDecimal.ONE, "Inventário"), USUARIO_ID))
                .hasMessage(ErrorMessage.PRODUTO_INATIVO.getDescricao());
        verify(movimentacaoEstoqueRepository, never()).save(any());
    }

    @Test
    void shouldRejectPeriodoWithDataInicioAfterDataFim() {
        final OffsetDateTime agora = OffsetDateTime.now();
        final MovimentacaoEstoqueFiltroRequest filtro = new MovimentacaoEstoqueFiltroRequest(null, null, null, agora, agora.minusDays(1));

        assertThatThrownBy(() -> movimentacaoEstoqueService.findAllMovimentacoes(filtro, Pageable.ofSize(20)))
                .hasMessage(ErrorMessage.PERIODO_INVALIDO.getDescricao());
        verifyNoInteractions(movimentacaoEstoqueRepository);
    }

    private static Produto newProduto(final BigDecimal estoqueInicial) {
        final Produto produto = new Produto("Arroz", "7891234567890", UnidadeMedida.UN, new BigDecimal("18.50"), new BigDecimal("24.90"),
                BigDecimal.TEN);
        produto.setId(PRODUTO_ID);
        produto.addEstoque(estoqueInicial);
        return produto;
    }
}
