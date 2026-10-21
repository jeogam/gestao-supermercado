-- Core: identidade e controle de acesso (RBAC dinâmico).
-- Usuário possui N perfis; cada perfil agrega N permissões do catálogo `permissao`.
-- O catálogo é a fonte da verdade no banco e deve permanecer sincronizado com o enum Java `Permissao`.

CREATE TABLE permissao (
    codigo    VARCHAR(255) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    grupo     VARCHAR(255) NOT NULL,
    CONSTRAINT pk_permissao PRIMARY KEY (codigo)
);

CREATE TABLE perfil (
    id            BIGSERIAL    NOT NULL,
    nome          VARCHAR(255) NOT NULL,
    descricao     VARCHAR(255),
    ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
    -- Perfil do sistema (ex.: Administrador) não pode ser editado nem inativado pela API,
    -- evitando que alguém remova o próprio acesso de gestão.
    sistema       BOOLEAN      NOT NULL DEFAULT FALSE,
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_perfil PRIMARY KEY (id),
    CONSTRAINT uk_perfil_nome UNIQUE (nome)
);

CREATE TABLE perfil_permissao (
    perfil_id        BIGINT       NOT NULL,
    permissao_codigo VARCHAR(255) NOT NULL,
    CONSTRAINT pk_perfil_permissao PRIMARY KEY (perfil_id, permissao_codigo),
    CONSTRAINT fk_perfil_permissao_perfil FOREIGN KEY (perfil_id) REFERENCES perfil (id),
    CONSTRAINT fk_perfil_permissao_permissao FOREIGN KEY (permissao_codigo) REFERENCES permissao (codigo)
);

CREATE INDEX idx_perfil_permissao_permissao ON perfil_permissao (permissao_codigo);

CREATE TABLE usuario (
    id                      BIGSERIAL    NOT NULL,
    nome                    VARCHAR(255) NOT NULL,
    -- Sempre gravado em minúsculas pela aplicação; a unicidade é case-insensitive na prática.
    email                   VARCHAR(255) NOT NULL,
    senha_hash              VARCHAR(255) NOT NULL,
    ativo                   BOOLEAN      NOT NULL DEFAULT TRUE,
    tentativas_login_falhas INTEGER      NOT NULL DEFAULT 0,
    bloqueado_ate           TIMESTAMPTZ,
    -- Tokens de acesso emitidos antes desta data deixam de valer (troca/redefinição de senha).
    senha_alterada_em       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ultimo_login_em         TIMESTAMPTZ,
    criado_em               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email UNIQUE (email)
);

CREATE TABLE usuario_perfil (
    usuario_id BIGINT NOT NULL,
    perfil_id  BIGINT NOT NULL,
    CONSTRAINT pk_usuario_perfil PRIMARY KEY (usuario_id, perfil_id),
    CONSTRAINT fk_usuario_perfil_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_usuario_perfil_perfil FOREIGN KEY (perfil_id) REFERENCES perfil (id)
);

CREATE INDEX idx_usuario_perfil_perfil ON usuario_perfil (perfil_id);

-- Refresh tokens são opacos; guardamos apenas o hash SHA-256 para que um vazamento do banco
-- não permita reutilizar sessões.
CREATE TABLE refresh_token (
    id          BIGSERIAL    NOT NULL,
    usuario_id  BIGINT       NOT NULL,
    token_hash  VARCHAR(255) NOT NULL,
    expira_em   TIMESTAMPTZ  NOT NULL,
    revogado_em TIMESTAMPTZ,
    criado_em   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_refresh_token PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE INDEX idx_refresh_token_usuario ON refresh_token (usuario_id);
