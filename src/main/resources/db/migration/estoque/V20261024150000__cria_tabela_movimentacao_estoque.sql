-- Estoque: histórico de toda entrada e saída de mercadoria.
-- O registro é imutável (nunca é editado nem excluído) e guarda o saldo resultante, para que seja possível
-- auditar como o estoque de um produto chegou ao valor atual.

CREATE TABLE movimentacao_estoque (
    id            BIGSERIAL     NOT NULL,
    produto_id    BIGINT        NOT NULL,
    tipo          VARCHAR(10)   NOT NULL,
    -- Quem originou a movimentação: ajuste manual ou os módulos de compras e vendas.
    origem        VARCHAR(20)   NOT NULL,
    quantidade    NUMERIC(12,3) NOT NULL,
    saldo_apos    NUMERIC(12,3) NOT NULL,
    -- Obrigatório no ajuste manual (perda, avaria, contagem de inventário...).
    motivo        VARCHAR(255),
    -- Id da compra ou venda que gerou a movimentação; nulo no ajuste manual.
    referencia_id BIGINT,
    usuario_id    BIGINT        NOT NULL,
    criado_em     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_movimentacao_estoque PRIMARY KEY (id),
    CONSTRAINT fk_movimentacao_estoque_produto FOREIGN KEY (produto_id) REFERENCES produto (id),
    CONSTRAINT fk_movimentacao_estoque_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT ck_movimentacao_estoque_tipo CHECK (tipo IN ('ENTRADA', 'SAIDA')),
    CONSTRAINT ck_movimentacao_estoque_origem CHECK (origem IN ('AJUSTE', 'COMPRA', 'VENDA', 'ESTORNO_VENDA')),
    CONSTRAINT ck_movimentacao_estoque_quantidade CHECK (quantidade > 0 AND saldo_apos >= 0),
    CONSTRAINT ck_movimentacao_estoque_motivo_ajuste CHECK (origem <> 'AJUSTE' OR motivo IS NOT NULL)
);

CREATE INDEX idx_movimentacao_estoque_produto ON movimentacao_estoque (produto_id, criado_em DESC);
CREATE INDEX idx_movimentacao_estoque_criado_em ON movimentacao_estoque (criado_em DESC);
