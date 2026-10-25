-- Vendas: registradas no caixa. Registrar dá baixa no estoque de cada item (movimentacao_estoque com origem VENDA);
-- cancelar devolve os itens ao estoque (origem ESTORNO_VENDA). Venda nunca é excluída, só cancelada.

CREATE TABLE venda (
    id                       BIGSERIAL     NOT NULL,
    status                   VARCHAR(20)   NOT NULL,
    forma_pagamento          VARCHAR(20)   NOT NULL,
    valor_total              NUMERIC(12,2) NOT NULL,
    -- Só no pagamento em dinheiro, quando o caixa informa quanto recebeu.
    valor_recebido           NUMERIC(12,2),
    troco                    NUMERIC(12,2),
    usuario_id               BIGINT        NOT NULL,
    criado_em                TIMESTAMPTZ   NOT NULL DEFAULT now(),
    cancelado_em             TIMESTAMPTZ,
    cancelado_por_usuario_id BIGINT,
    motivo_cancelamento      VARCHAR(255),
    -- Lock otimista: dois cancelamentos simultâneos não podem devolver o estoque duas vezes.
    versao                   BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_venda PRIMARY KEY (id),
    CONSTRAINT fk_venda_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_venda_cancelado_por FOREIGN KEY (cancelado_por_usuario_id) REFERENCES usuario (id),
    CONSTRAINT ck_venda_status CHECK (status IN ('CONCLUIDA', 'CANCELADA')),
    CONSTRAINT ck_venda_forma_pagamento CHECK (forma_pagamento IN ('DINHEIRO', 'CARTAO_DEBITO', 'CARTAO_CREDITO', 'PIX')),
    CONSTRAINT ck_venda_valores CHECK (valor_total >= 0 AND (valor_recebido IS NULL OR valor_recebido >= valor_total)),
    CONSTRAINT ck_venda_cancelamento CHECK ((status = 'CANCELADA') = (cancelado_em IS NOT NULL AND cancelado_por_usuario_id IS NOT NULL))
);

CREATE INDEX idx_venda_criado_em ON venda (criado_em DESC);
CREATE INDEX idx_venda_usuario ON venda (usuario_id, criado_em DESC);

CREATE TABLE item_venda (
    id                   BIGSERIAL     NOT NULL,
    venda_id             BIGINT        NOT NULL,
    produto_id           BIGINT        NOT NULL,
    quantidade           NUMERIC(12,3) NOT NULL,
    -- Preço e custo do momento da venda: mudanças posteriores no produto não alteram vendas antigas nem a margem.
    preco_unitario       NUMERIC(12,2) NOT NULL,
    preco_custo_unitario NUMERIC(12,2) NOT NULL,
    subtotal             NUMERIC(12,2) NOT NULL,
    CONSTRAINT pk_item_venda PRIMARY KEY (id),
    CONSTRAINT fk_item_venda_venda FOREIGN KEY (venda_id) REFERENCES venda (id),
    CONSTRAINT fk_item_venda_produto FOREIGN KEY (produto_id) REFERENCES produto (id),
    CONSTRAINT uk_item_venda_produto UNIQUE (venda_id, produto_id),
    CONSTRAINT ck_item_venda_valores CHECK (quantidade > 0 AND preco_unitario >= 0 AND preco_custo_unitario >= 0 AND subtotal >= 0)
);

CREATE INDEX idx_item_venda_produto ON item_venda (produto_id);
