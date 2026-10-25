package br.com.gestaosupermercado.modules.venda.venda.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.gestaosupermercado.modules.venda.venda.enums.FormaPagamento;
import br.com.gestaosupermercado.modules.venda.venda.enums.StatusVenda;
import br.com.gestaosupermercado.shared.exception.ErrorMessage;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class VendaTest {

    private static Venda newVenda(final FormaPagamento formaPagamento) {
        final Venda venda = new Venda(formaPagamento, 1L);
        venda.addItem(1L, new BigDecimal("2"), new BigDecimal("23.90"), new BigDecimal("18.50"));
        venda.addItem(2L, new BigDecimal("0.345"), new BigDecimal("39.90"), new BigDecimal("30.00"));
        return venda;
    }

    @Test
    void shouldSumSubtotaisRoundedToCentavos() {
        final Venda venda = newVenda(FormaPagamento.PIX);

        assertThat(venda.getItens().get(1).getSubtotal()).isEqualByComparingTo("13.77");
        assertThat(venda.getValorTotal()).isEqualByComparingTo("61.57");
        assertThat(venda.getStatus()).isEqualTo(StatusVenda.CONCLUIDA);
    }

    @Test
    void shouldCalculateTrocoForDinheiro() {
        final Venda venda = newVenda(FormaPagamento.DINHEIRO);

        venda.registerValorRecebido(new BigDecimal("100.00"));

        assertThat(venda.getValorRecebido()).isEqualByComparingTo("100.00");
        assertThat(venda.getTroco()).isEqualByComparingTo("38.43");
    }

    @Test
    void shouldRejectValorRecebidoMenorQueTotal() {
        final Venda venda = newVenda(FormaPagamento.DINHEIRO);

        assertThatThrownBy(() -> venda.registerValorRecebido(new BigDecimal("50.00")))
                .hasMessage(ErrorMessage.VALOR_RECEBIDO_INSUFICIENTE.getDescricao());
    }

    @Test
    void shouldIgnoreValorRecebidoForOtherFormasPagamento() {
        final Venda venda = newVenda(FormaPagamento.CARTAO_DEBITO);

        venda.registerValorRecebido(new BigDecimal("1.00"));

        assertThat(venda.getValorRecebido()).isNull();
        assertThat(venda.getTroco()).isNull();
    }

    @Test
    void shouldRejectProdutoRepetido() {
        final Venda venda = newVenda(FormaPagamento.PIX);

        assertThatThrownBy(() -> venda.addItem(1L, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE))
                .hasMessage(ErrorMessage.PRODUTO_REPETIDO_NA_VENDA.getDescricao());
    }

    @Test
    void shouldCancelOnlyOnce() {
        final Venda venda = newVenda(FormaPagamento.PIX);
        final OffsetDateTime agora = OffsetDateTime.parse("2026-10-25T12:00:00Z");

        venda.cancel(7L, "  Cliente desistiu  ", agora);

        assertThat(venda.getStatus()).isEqualTo(StatusVenda.CANCELADA);
        assertThat(venda.getCanceladoEm()).isEqualTo(agora);
        assertThat(venda.getCanceladoPorUsuarioId()).isEqualTo(7L);
        assertThat(venda.getMotivoCancelamento()).isEqualTo("Cliente desistiu");
        assertThatThrownBy(() -> venda.cancel(7L, null, agora))
                .hasMessage(ErrorMessage.VENDA_JA_CANCELADA.getDescricao());
    }
}
