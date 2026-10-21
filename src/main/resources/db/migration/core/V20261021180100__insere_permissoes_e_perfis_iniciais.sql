-- Catálogo inicial de permissões do Core e perfis padrão do supermercado.
-- Toda permissão nova em migrations futuras deve: (1) entrar no catálogo com ON CONFLICT DO NOTHING,
-- (2) existir no enum Java `Permissao` e (3) ser concedida ao perfil Administrador.

INSERT INTO permissao (codigo, descricao, grupo) VALUES
    ('USUARIO_LISTAR',           'Listar e consultar usuários',          'Usuários'),
    ('USUARIO_CRIAR',            'Cadastrar usuários',                   'Usuários'),
    ('USUARIO_EDITAR',           'Editar dados e perfis de usuários',    'Usuários'),
    ('USUARIO_ALTERAR_STATUS',   'Ativar e inativar usuários',           'Usuários'),
    ('USUARIO_REDEFINIR_SENHA',  'Redefinir a senha de outros usuários', 'Usuários'),
    ('PERFIL_LISTAR',            'Listar e consultar perfis',            'Perfis'),
    ('PERFIL_CRIAR',             'Cadastrar perfis',                     'Perfis'),
    ('PERFIL_EDITAR',            'Editar perfis e suas permissões',      'Perfis'),
    ('PERFIL_ALTERAR_STATUS',    'Ativar e inativar perfis',             'Perfis'),
    ('PERMISSAO_LISTAR',         'Listar o catálogo de permissões',      'Permissões')
ON CONFLICT DO NOTHING;

INSERT INTO perfil (nome, descricao, sistema) VALUES
    ('Administrador', 'Acesso total à gestão de usuários, perfis e permissões', TRUE),
    ('Gerente',       'Gerência da loja',                                       FALSE),
    ('Caixa',         'Operador de caixa',                                      FALSE),
    ('Estoquista',    'Recebimento e controle de estoque',                      FALSE);

INSERT INTO perfil_permissao (perfil_id, permissao_codigo)
SELECT p.id, pm.codigo
FROM perfil p
CROSS JOIN permissao pm
WHERE p.nome = 'Administrador';

INSERT INTO perfil_permissao (perfil_id, permissao_codigo)
SELECT p.id, pm.codigo
FROM perfil p
JOIN permissao pm ON pm.codigo IN ('USUARIO_LISTAR', 'PERFIL_LISTAR', 'PERMISSAO_LISTAR')
WHERE p.nome = 'Gerente';
