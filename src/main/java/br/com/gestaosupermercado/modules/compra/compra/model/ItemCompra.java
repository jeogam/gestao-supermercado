package br.com.gestaosupermercado.modules.compra.compra.model;

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
 * Item de uma {@link Compra}. Criado só por {@link Compra#addItem}, que mantém o total da compra em dia.
 *
 * @since 0.0.1
 */
@Getter
@Entity
@Table(name = "item_compra")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id", nullable = false, updatable = false)
    private Compra compra;

    @Column(name = "produto_id", nullable = false, updatable = false)
    private Long produtoId;

    @Column(name = "quantidade", nullable = false, updatable = false)
    private BigDecimal quantidade;

    @Column(name = "preco_custo_unitario", nullable = false, updatable = false)
    private BigDecimal precoCustoUnitario;

    @Column(name = "subtotal", nullable = false, updatable = false)
    private BigDecimal subtotal;

    // Subtotal em centavos: 1,255 kg a R$ 9,99 dá R$ 12,537... e a coluna guarda 2 casas.
    ItemCompra(final Compra compra, final Long produtoId, final BigDecimal quantidade, final BigDecimal precoCustoUnitario) {
        this.compra = compra;
        this.produtoId = produtoId;
        this.quantidade = quantidade;
        this.precoCustoUnitario = precoCustoUnitario;
        this.subtotal = quantidade.multiply(precoCustoUnitario).setScale(2, RoundingMode.HALF_EVEN);
    }
}
