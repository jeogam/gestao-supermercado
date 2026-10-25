package br.com.gestaosupermercado.modules.compra.compra.model;

import br.com.gestaosupermercado.modules.compra.fornecedor.model.Fornecedor;
import br.com.gestaosupermercado.shared.exception.BusinessException;
import br.com.gestaosupermercado.shared.exception.ErrorMessage;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Compra de mercadoria de um fornecedor. É imutável depois de registrada: cada item já deu entrada no estoque, e
 * um erro de lançamento se corrige com ajuste de estoque.
 * <p>
 * Produto e usuário são guardados pelo id; o fornecedor é do próprio módulo de compras.
 *
 * @since 0.0.1
 */
@Getter
@Entity
@Table(name = "compra")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fornecedor_id", nullable = false, updatable = false)
    private Fornecedor fornecedor;

    @Column(name = "numero_nota_fiscal", updatable = false)
    private String numeroNotaFiscal;

    @Column(name = "valor_total", nullable = false)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(name = "observacao", updatable = false)
    private String observacao;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Getter(AccessLevel.NONE)
    @OrderBy("id")
    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemCompra> itens = new ArrayList<>();

    public Compra(final Fornecedor fornecedor, final String numeroNotaFiscal, final String observacao, final Long usuarioId) {
        this.fornecedor = fornecedor;
        this.numeroNotaFiscal = numeroNotaFiscal;
        this.observacao = observacao;
        this.usuarioId = usuarioId;
    }

    /**
     * Recusa o mesmo produto em dois itens: a entrada no estoque e o custo do produto ficariam ambíguos.
     */
    public void addItem(final Long produtoId, final BigDecimal quantidade, final BigDecimal precoCustoUnitario) {
        if (itens.stream().anyMatch(item -> item.getProdutoId().equals(produtoId))) {
            throw BusinessException.unprocessable(ErrorMessage.PRODUTO_REPETIDO_NA_COMPRA);
        }
        final ItemCompra item = new ItemCompra(this, produtoId, quantidade, precoCustoUnitario);
        itens.add(item);
        valorTotal = valorTotal.add(item.getSubtotal());
    }

    public List<ItemCompra> getItens() {
        return Collections.unmodifiableList(itens);
    }
}
