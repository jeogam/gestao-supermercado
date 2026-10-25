package br.com.gestaosupermercado.modules.venda.venda.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Item de uma {@link Venda}. Criado só por {@link Venda#addItem}, que mantém o total em dia.
 * <p>
 * Guarda preço e custo do momento da venda: reajustes posteriores do produto não mudam vendas antigas nem a margem
 * calculada nos relatórios.
 *
 * @since 0.0.1
 */
@Getter
@Entity
@Table(name = "item_venda")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venda_id", nullable = false, updatable = false)
    private Venda venda;

    @Column(name = "produto_id", nullable = false, updatable = false)
    private Long produtoId;

    @Column(name = "quantidade", nullable = false, updatable = false)
    private BigDecimal quantidade;

    @Column(name = "preco_unitario", nullable = false, updatable = false)
    private BigDecimal precoUnitario;

    @Column(name = "preco_custo_unitario", nullable = false, updatable = false)
    private BigDecimal precoCustoUnitario;

    @Column(name = "subtotal", nullable = false, updatable = false)
    private BigDecimal subtotal;

    // Subtotal em centavos: 0,345 kg a R$ 39,90 dá R$ 13,7655 e a coluna guarda 2 casas.
    ItemVenda(final Venda venda, final Long produtoId, final BigDecimal quantidade, final BigDecimal precoUnitario,
              final BigDecimal precoCustoUnitario) {
        this.venda = venda;
        this.produtoId = produtoId;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.precoCustoUnitario = precoCustoUnitario;
        this.subtotal = quantidade.multiply(precoUnitario).setScale(2, RoundingMode.HALF_EVEN);
    }
}
