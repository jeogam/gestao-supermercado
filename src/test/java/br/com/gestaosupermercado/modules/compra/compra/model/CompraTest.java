package br.com.gestaosupermercado.modules.compra.compra.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.gestaosupermercado.modules.compra.fornecedor.model.Fornecedor;
import br.com.gestaosupermercado.shared.exception.ErrorMessage;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CompraTest {

    private static Compra newCompra() {
        return new Compra(new Fornecedor("Atacadão", null, null, null), "123", null, 1L);
    }

    @Test
    void shouldSumSubtotaisRoundedToCentavos() {
        final Compra compra = newCompra();

        compra.addItem(1L, new BigDecimal("24"), new BigDecimal("18.50"));
        compra.addItem(2L, new BigDecimal("1.255"), new BigDecimal("9.99"));

        assertThat(compra.getItens()).hasSize(2);
        assertThat(compra.getItens().get(1).getSubtotal()).isEqualByComparingTo("12.54");
        assertThat(compra.getValorTotal()).isEqualByComparingTo("456.54");
    }

    @Test
    void shouldRejectProdutoRepetido() {
        final Compra compra = newCompra();
        compra.addItem(1L, BigDecimal.ONE, BigDecimal.TEN);

        assertThatThrownBy(() -> compra.addItem(1L, BigDecimal.TWO, BigDecimal.TEN))
                .hasMessage(ErrorMessage.PRODUTO_REPETIDO_NA_COMPRA.getDescricao());
        assertThat(compra.getValorTotal()).isEqualByComparingTo("10");
    }

    @Test
    void shouldNotExposeItensForModification() {
        final Compra compra = newCompra();

        assertThatThrownBy(() -> compra.getItens().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
