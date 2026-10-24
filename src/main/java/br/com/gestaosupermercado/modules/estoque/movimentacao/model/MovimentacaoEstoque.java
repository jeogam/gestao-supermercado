package br.com.gestaosupermercado.modules.estoque.movimentacao.model;

import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.OrigemMovimentacao;
import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.TipoMovimentacao;
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
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Entrada ou saída de mercadoria. É imutável: um erro se corrige com uma nova movimentação (ajuste ou estorno),
 * nunca editando a antiga, para o histórico continuar explicando o saldo atual.
 * <p>
 * Produto e usuário são guardados pelo id: o histórico não carrega as entidades de outros módulos.
 *
 * @since 0.0.1
 */
@Getter
@Entity
@Table(name = "movimentacao_estoque")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "produto_id", nullable = false, updatable = false)
    private Long produtoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, updatable = false)
    private TipoMovimentacao tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", nullable = false, updatable = false)
    private OrigemMovimentacao origem;

    @Column(name = "quantidade", nullable = false, updatable = false)
    private BigDecimal quantidade;

    @Column(name = "saldo_apos", nullable = false, updatable = false)
    private BigDecimal saldoApos;

    @Column(name = "motivo", updatable = false)
    private String motivo;

    @Column(name = "referencia_id", updatable = false)
    private Long referenciaId;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    public MovimentacaoEstoque(final Long produtoId, final TipoMovimentacao tipo, final OrigemMovimentacao origem, final BigDecimal quantidade,
                               final BigDecimal saldoApos, final String motivo, final Long referenciaId, final Long usuarioId) {
        if (origem == OrigemMovimentacao.AJUSTE && (motivo == null || motivo.isBlank())) {
            throw BusinessException.unprocessable(ErrorMessage.MOTIVO_AJUSTE_OBRIGATORIO);
        }
        this.produtoId = produtoId;
        this.tipo = tipo;
        this.origem = origem;
        this.quantidade = quantidade;
        this.saldoApos = saldoApos;
        this.motivo = motivo == null || motivo.isBlank() ? null : motivo.strip();
        this.referenciaId = referenciaId;
        this.usuarioId = usuarioId;
    }
}
