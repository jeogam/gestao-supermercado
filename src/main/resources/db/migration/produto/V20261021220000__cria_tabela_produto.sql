-- Produto: cadastro central usado por Estoque, Vendas, Compras e Relatórios.
-- Valores monetários em NUMERIC(12,2); quantidades em NUMERIC(12,3) para aceitar itens vendidos a granel (KG, L).

CREATE TABLE produto (
    id                 BIGSERIAL     NOT NULL,
    nome               VARCHAR(255)  NOT NULL,
    -- Opcional: itens a granel ou de produção própria podem não ter código de barras.
    codigo_barras      VARCHAR(50),
    unidade_medida     VARCHAR(10)   NOT NULL,
    -- Preço de custo é a base da margem nos relatórios.
    preco_custo        NUMERIC(12,2) NOT NULL,
    preco_venda        NUMERIC(12,2) NOT NULL,
    quantidade_estoque NUMERIC(12,3) NOT NULL DEFAULT 0,
    estoque_minimo     NUMERIC(12,3) NOT NULL DEFAULT 0,
    ativo              BOOLEAN       NOT NULL DEFAULT TRUE,
    -- Lock otimista: movimentações concorrentes de estoque falham em vez de se sobrescreverem.
    versao             BIGINT        NOT NULL DEFAULT 0,
    criado_em          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    atualizado_em      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_produto PRIMARY KEY (id),
    CONSTRAINT uk_produto_codigo_barras UNIQUE (codigo_barras),
    CONSTRAINT ck_produto_unidade_medida CHECK (unidade_medida IN ('UN', 'KG', 'L')),
    CONSTRAINT ck_produto_precos CHECK (preco_custo >= 0 AND preco_venda >= 0),
    CONSTRAINT ck_produto_estoque CHECK (quantidade_estoque >= 0 AND estoque_minimo >= 0)
);
