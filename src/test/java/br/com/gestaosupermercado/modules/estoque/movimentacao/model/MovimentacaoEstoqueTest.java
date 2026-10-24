package br.com.gestaosupermercado.modules.estoque.movimentacao.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.OrigemMovimentacao;
import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.TipoMovimentacao;
import br.com.gestaosupermercado.shared.exception.ErrorMessage;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MovimentacaoEstoqueTest {

    @Test
    void shouldRequireMotivoForAjuste() {
        assertThatThrownBy(() -> new MovimentacaoEstoque(1L, TipoMovimentacao.SAIDA, OrigemMovimentacao.AJUSTE, BigDecimal.ONE,
                BigDecimal.TEN, "   ", null, 1L))
                .hasMessage(ErrorMessage.MOTIVO_AJUSTE_OBRIGATORIO.getDescricao());
        assertThatThrownBy(() -> new MovimentacaoEstoque(1L, TipoMovimentacao.SAIDA, OrigemMovimentacao.AJUSTE, BigDecimal.ONE,
                BigDecimal.TEN, null, null, 1L))
                .hasMessage(ErrorMessage.MOTIVO_AJUSTE_OBRIGATORIO.getDescricao());
    }

    @Test
    void shouldAcceptVendaWithoutMotivo() {
        final MovimentacaoEstoque movimentacao = new MovimentacaoEstoque(1L, TipoMovimentacao.SAIDA, OrigemMovimentacao.VENDA,
                BigDecimal.ONE, BigDecimal.TEN, "  ", 48L, 1L);

        assertThat(movimentacao.getMotivo()).isNull();
        assertThat(movimentacao.getReferenciaId()).isEqualTo(48L);
    }

    @Test
    void shouldStripMotivo() {
        final MovimentacaoEstoque movimentacao = new MovimentacaoEstoque(1L, TipoMovimentacao.SAIDA, OrigemMovimentacao.AJUSTE,
                BigDecimal.ONE, BigDecimal.TEN, "  Avaria  ", null, 1L);

        assertThat(movimentacao.getMotivo()).isEqualTo("Avaria");
    }
}
