package br.com.gestaosupermercado.modules.produto.produto.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.gestaosupermercado.modules.produto.produto.enums.UnidadeMedida;
import br.com.gestaosupermercado.shared.exception.BusinessException;
import br.com.gestaosupermercado.shared.exception.ErrorMessage;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProdutoTest {

    private static Produto novoProduto(final UnidadeMedida unidadeMedida) {
        return new Produto("Arroz", "7891234567890", unidadeMedida, new BigDecimal("18.50"), new BigDecimal("24.90"), BigDecimal.TEN);
    }

    @Test
    void shouldAddAndRemoveEstoque() {
        final Produto produto = novoProduto(UnidadeMedida.UN);

        produto.addEstoque(new BigDecimal("12"));
        produto.removeEstoque(new BigDecimal("5"));

        assertThat(produto.getQuantidadeEstoque()).isEqualByComparingTo("7");
        assertThat(produto.isAbaixoEstoqueMinimo()).isTrue();
    }

    @Test
    void shouldRejectRemoveEstoqueAboveDisponivel() {
        final Produto produto = novoProduto(UnidadeMedida.UN);
        produto.addEstoque(BigDecimal.ONE);

        assertThatThrownBy(() -> produto.removeEstoque(new BigDecimal("2")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorMessage.ESTOQUE_INSUFICIENTE.getDescricao());
        assertThat(produto.getQuantidadeEstoque()).isEqualByComparingTo("1");
    }

    @Test
    void shouldRejectZeroNegativeOrFracionadaForUnidade() {
        final Produto produto = novoProduto(UnidadeMedida.UN);

        assertThatThrownBy(() -> produto.addEstoque(BigDecimal.ZERO)).hasMessage(ErrorMessage.QUANTIDADE_INVALIDA.getDescricao());
        assertThatThrownBy(() -> produto.addEstoque(new BigDecimal("-1"))).hasMessage(ErrorMessage.QUANTIDADE_INVALIDA.getDescricao());
        assertThatThrownBy(() -> produto.addEstoque(new BigDecimal("1.5"))).hasMessage(ErrorMessage.QUANTIDADE_INVALIDA.getDescricao());
        produto.addEstoque(new BigDecimal("2.000"));
        assertThat(produto.getQuantidadeEstoque()).isEqualByComparingTo("2");
    }

    @Test
    void shouldRejectMoreThanTresCasasDecimais() {
        final Produto produto = novoProduto(UnidadeMedida.KG);

        assertThatThrownBy(() -> produto.addEstoque(new BigDecimal("0.3456"))).hasMessage(ErrorMessage.QUANTIDADE_INVALIDA.getDescricao());
        produto.addEstoque(new BigDecimal("0.3450"));
        assertThat(produto.getQuantidadeEstoque()).isEqualByComparingTo("0.345");
    }

    @Test
    void shouldRejectChangeToUnidadeWithEstoqueFracionado() {
        final Produto produto = novoProduto(UnidadeMedida.KG);
        produto.addEstoque(new BigDecimal("1.5"));

        assertThatThrownBy(() -> produto.updateUnidadeMedida(UnidadeMedida.UN))
                .hasMessage(ErrorMessage.UNIDADE_INCOMPATIVEL_COM_ESTOQUE.getDescricao());

        produto.addEstoque(new BigDecimal("0.5"));
        produto.updateUnidadeMedida(UnidadeMedida.UN);
        assertThat(produto.getUnidadeMedida()).isEqualTo(UnidadeMedida.UN);
    }

    @Test
    void shouldAcceptFracionadaForKg() {
        final Produto produto = novoProduto(UnidadeMedida.KG);

        produto.addEstoque(new BigDecimal("1.250"));
        produto.removeEstoque(new BigDecimal("0.750"));

        assertThat(produto.getQuantidadeEstoque()).isEqualByComparingTo("0.5");
    }
}
