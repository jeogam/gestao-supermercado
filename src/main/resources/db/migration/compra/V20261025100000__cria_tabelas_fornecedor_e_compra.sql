-- Compras: fornecedores e compras de mercadoria. Registrar uma compra dá entrada no estoque de cada item
-- (movimentacao_estoque com origem COMPRA e referencia_id = compra.id).

CREATE TABLE fornecedor (
    id            BIGSERIAL    NOT NULL,
    nome          VARCHAR(255) NOT NULL,
    -- Opcional: pequenos fornecedores (feira, produtor local) podem não ter CNPJ. Só dígitos.
    cnpj          VARCHAR(14),
    telefone      VARCHAR(20),
    email         VARCHAR(255),
    ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_fornecedor PRIMARY KEY (id),
    CONSTRAINT uk_fornecedor_cnpj UNIQUE (cnpj),
    CONSTRAINT ck_fornecedor_cnpj CHECK (cnpj ~ '^[0-9]{14}$')
);

-- Compra é imutável depois de registrada: um erro de lançamento se corrige com ajuste de estoque.
CREATE TABLE compra (
    id                 BIGSERIAL     NOT NULL,
    fornecedor_id      BIGINT        NOT NULL,
    numero_nota_fiscal VARCHAR(50),
    valor_total        NUMERIC(12,2) NOT NULL,
    observacao         VARCHAR(255),
    usuario_id         BIGINT        NOT NULL,
    criado_em          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_compra PRIMARY KEY (id),
    CONSTRAINT fk_compra_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES fornecedor (id),
    CONSTRAINT fk_compra_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT ck_compra_valor_total CHECK (valor_total >= 0)
);

CREATE INDEX idx_compra_fornecedor ON compra (fornecedor_id, criado_em DESC);
CREATE INDEX idx_compra_criado_em ON compra (criado_em DESC);

CREATE TABLE item_compra (
    id                    BIGSERIAL     NOT NULL,
    compra_id             BIGINT        NOT NULL,
    produto_id            BIGINT        NOT NULL,
    quantidade            NUMERIC(12,3) NOT NULL,
    preco_custo_unitario  NUMERIC(12,2) NOT NULL,
    subtotal              NUMERIC(12,2) NOT NULL,
    CONSTRAINT pk_item_compra PRIMARY KEY (id),
    CONSTRAINT fk_item_compra_compra FOREIGN KEY (compra_id) REFERENCES compra (id),
    CONSTRAINT fk_item_compra_produto FOREIGN KEY (produto_id) REFERENCES produto (id),
    CONSTRAINT uk_item_compra_produto UNIQUE (compra_id, produto_id),
    CONSTRAINT ck_item_compra_valores CHECK (quantidade > 0 AND preco_custo_unitario >= 0 AND subtotal >= 0)
);

CREATE INDEX idx_item_compra_produto ON item_compra (produto_id);
