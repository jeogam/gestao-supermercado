package br.com.gestaosupermercado.modules.venda.venda.model;

import br.com.gestaosupermercado.modules.venda.venda.enums.FormaPagamento;
import br.com.gestaosupermercado.modules.venda.venda.enums.StatusVenda;
import br.com.gestaosupermercado.shared.exception.BusinessException;
import br.com.gestaosupermercado.shared.exception.ErrorMessage;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
 * Venda registrada no caixa. Nunca é excluída: o cancelamento muda o status e guarda quem cancelou, quando e por quê,
 * para a venda continuar aparecendo no histórico e nos relatórios.
 * <p>
 * Produto e usuários são guardados pelo id.
 *
 * @since 0.0.1
 */
@Getter
@Entity
@Table(name = "venda")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Venda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusVenda status = StatusVenda.CONCLUIDA;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, updatable = false)
    private FormaPagamento formaPagamento;

    @Column(name = "valor_total", nullable = false, updatable = false)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(name = "valor_recebido", updatable = false)
    private BigDecimal valorRecebido;

    @Column(name = "troco", updatable = false)
    private BigDecimal troco;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "cancelado_em")
    private OffsetDateTime canceladoEm;

    @Column(name = "cancelado_por_usuario_id")
    private Long canceladoPorUsuarioId;

    @Column(name = "motivo_cancelamento")
    private String motivoCancelamento;

    @Version
    @Column(name = "versao", nullable = false)
    private long versao;

    @Getter(AccessLevel.NONE)
    @OrderBy("id")
    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemVenda> itens = new ArrayList<>();

    public Venda(final FormaPagamento formaPagamento, final Long usuarioId) {
        this.formaPagamento = formaPagamento;
        this.usuarioId = usuarioId;
    }

    /**
     * Recusa o mesmo produto em dois itens: o caixa soma a quantidade no item que já existe.
     */
    public void addItem(final Long produtoId, final BigDecimal quantidade, final BigDecimal precoUnitario, final BigDecimal precoCustoUnitario) {
        if (itens.stream().anyMatch(item -> item.getProdutoId().equals(produtoId))) {
            throw BusinessException.unprocessable(ErrorMessage.PRODUTO_REPETIDO_NA_VENDA);
        }
        final ItemVenda item = new ItemVenda(this, produtoId, quantidade, precoUnitario, precoCustoUnitario);
        itens.add(item);
        valorTotal = valorTotal.add(item.getSubtotal());
    }

    /**
     * Registra o valor entregue pelo cliente e calcula o troco. Só faz sentido no pagamento em dinheiro; nas outras
     * formas o valor é ignorado. Deve ser chamado depois de adicionar todos os itens.
     */
    public void registerValorRecebido(final BigDecimal valorRecebido) {
        if (formaPagamento != FormaPagamento.DINHEIRO || valorRecebido == null) {
            return;
        }
        if (valorRecebido.compareTo(valorTotal) < 0) {
            throw BusinessException.unprocessable(ErrorMessage.VALOR_RECEBIDO_INSUFICIENTE);
        }
        this.valorRecebido = valorRecebido;
        this.troco = valorRecebido.subtract(valorTotal);
    }

    public void cancelar(final Long usuarioId, final String motivo, final OffsetDateTime agora) {
        if (status == StatusVenda.CANCELADA) {
            throw BusinessException.unprocessable(ErrorMessage.VENDA_JA_CANCELADA);
        }
        status = StatusVenda.CANCELADA;
        canceladoEm = agora;
        canceladoPorUsuarioId = usuarioId;
        motivoCancelamento = motivo == null || motivo.isBlank() ? null : motivo.strip();
    }

    public List<ItemVenda> getItens() {
        return Collections.unmodifiableList(itens);
    }
}
