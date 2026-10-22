package br.com.gestaosupermercado.modules.produto.produto.model;

import br.com.gestaosupermercado.modules.produto.produto.enums.UnidadeMedida;
import br.com.gestaosupermercado.shared.exception.BusinessException;
import br.com.gestaosupermercado.shared.exception.ErrorMessage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Produto vendido pelo mercado. É a entidade compartilhada por Estoque, Vendas, Compras e Relatórios.
 * <p>
 * A quantidade em estoque só muda por {@link #addEstoque} e {@link #removeEstoque}, para que compra, venda e
 * ajuste apliquem as mesmas regras (nunca negativo, inteiro para {@link UnidadeMedida#UN}).
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Entity
@Table(name = "produto")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "codigo_barras")
    private String codigoBarras;

    @Setter(AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_medida", nullable = false)
    private UnidadeMedida unidadeMedida;

    @Column(name = "preco_custo", nullable = false)
    private BigDecimal precoCusto;

    @Column(name = "preco_venda", nullable = false)
    private BigDecimal precoVenda;

    @Setter(AccessLevel.NONE)
    @Column(name = "quantidade_estoque", nullable = false)
    private BigDecimal quantidadeEstoque = BigDecimal.ZERO;

    @Column(name = "estoque_minimo", nullable = false)
    private BigDecimal estoqueMinimo = BigDecimal.ZERO;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    // Duas vendas simultâneas do mesmo produto não podem sobrescrever o estoque uma da outra.
    @Version
    @Setter(AccessLevel.NONE)
    @Column(name = "versao", nullable = false)
    private long versao;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    public Produto(final String nome, final String codigoBarras, final UnidadeMedida unidadeMedida, final BigDecimal precoCusto,
                   final BigDecimal precoVenda, final BigDecimal estoqueMinimo) {
        this.nome = nome;
        this.codigoBarras = codigoBarras;
        this.unidadeMedida = unidadeMedida;
        this.precoCusto = precoCusto;
        this.precoVenda = precoVenda;
        this.estoqueMinimo = estoqueMinimo;
    }

    /**
     * Entrada de mercadoria (compra, devolução, ajuste positivo).
     */
    public void addEstoque(final BigDecimal quantidade) {
        ensureQuantidadeValida(quantidade);
        quantidadeEstoque = quantidadeEstoque.add(quantidade);
    }

    /**
     * Saída de mercadoria (venda, perda, ajuste negativo). Recusa deixar o estoque negativo.
     */
    public void removeEstoque(final BigDecimal quantidade) {
        ensureQuantidadeValida(quantidade);
        if (quantidade.compareTo(quantidadeEstoque) > 0) {
            throw BusinessException.unprocessable(ErrorMessage.ESTOQUE_INSUFICIENTE);
        }
        quantidadeEstoque = quantidadeEstoque.subtract(quantidade);
    }

    public boolean isAbaixoEstoqueMinimo() {
        return quantidadeEstoque.compareTo(estoqueMinimo) < 0;
    }

    /**
     * Recusa passar para uma unidade inteira (UN) com estoque fracionado: a sobra nunca poderia ser baixada.
     */
    public void updateUnidadeMedida(final UnidadeMedida novaUnidadeMedida) {
        if (!novaUnidadeMedida.isFracionavel() && isFracionada(quantidadeEstoque)) {
            throw BusinessException.unprocessable(ErrorMessage.UNIDADE_INCOMPATIVEL_COM_ESTOQUE);
        }
        unidadeMedida = novaUnidadeMedida;
    }

    // Mais de 3 casas seria arredondado em silêncio pela coluna NUMERIC(12,3).
    private void ensureQuantidadeValida(final BigDecimal quantidade) {
        if (quantidade.signum() <= 0 || quantidade.stripTrailingZeros().scale() > 3
                || (isFracionada(quantidade) && !unidadeMedida.isFracionavel())) {
            throw BusinessException.unprocessable(ErrorMessage.QUANTIDADE_INVALIDA);
        }
    }

    private static boolean isFracionada(final BigDecimal quantidade) {
        return quantidade.stripTrailingZeros().scale() > 0;
    }
}
