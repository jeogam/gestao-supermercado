-- Define os perfis do mercado e o que cada um pode fazer:
--   Caixa      -> somente vendas (consulta produtos para vender, sem alterá-los)
--   Estoquista -> somente estoque: produtos, estoque e compras (compra é entrada de estoque)
--   Gerente    -> tudo do Caixa e do Estoquista, mais relatórios; não gere usuários nem perfis
--   Superadmin -> tudo do sistema (antigo "Administrador", perfil do sistema)

INSERT INTO permissao (codigo, descricao, grupo) VALUES
    ('VENDA_LISTAR',         'Consultar vendas realizadas',                       'Vendas'),
    ('VENDA_REGISTRAR',      'Registrar vendas no caixa',                         'Vendas'),
    ('VENDA_CANCELAR',       'Cancelar ou desfazer vendas',                       'Vendas'),
    ('PRODUTO_LISTAR',       'Consultar produtos, preços e estoque disponível',   'Produtos'),
    ('PRODUTO_GERENCIAR',    'Cadastrar, editar e inativar produtos',             'Produtos'),
    ('ESTOQUE_LISTAR',       'Consultar estoque e alertas de reposição',          'Estoque'),
    ('ESTOQUE_AJUSTAR',      'Ajustar quantidades em estoque',                    'Estoque'),
    ('COMPRA_LISTAR',        'Consultar compras lançadas',                        'Compras'),
    ('COMPRA_REGISTRAR',     'Lançar compras de fornecedores',                    'Compras'),
    ('RELATORIO_VISUALIZAR', 'Ver faturamento, margem e gerar relatório em PDF',  'Relatórios')
ON CONFLICT DO NOTHING;

UPDATE perfil
SET nome = 'Superadmin',
    descricao = 'Acesso total ao sistema, inclusive usuários e perfis',
    atualizado_em = now()
WHERE sistema = TRUE;

UPDATE perfil SET descricao = 'Operador de caixa: registra vendas',                      atualizado_em = now() WHERE nome = 'Caixa';
UPDATE perfil SET descricao = 'Cuida de produtos, estoque e compras',                    atualizado_em = now() WHERE nome = 'Estoquista';
UPDATE perfil SET descricao = 'Vendas, estoque e relatórios da loja',                    atualizado_em = now() WHERE nome = 'Gerente';

-- Recompõe as permissões dos perfis do mercado do zero, para o resultado não depender do estado anterior.
DELETE FROM perfil_permissao
WHERE perfil_id IN (SELECT id FROM perfil WHERE nome IN ('Caixa', 'Estoquista', 'Gerente'));

INSERT INTO perfil_permissao (perfil_id, permissao_codigo)
SELECT p.id, pm.codigo
FROM perfil p
JOIN permissao pm ON pm.codigo IN ('VENDA_LISTAR', 'VENDA_REGISTRAR', 'VENDA_CANCELAR', 'PRODUTO_LISTAR')
WHERE p.nome = 'Caixa';

INSERT INTO perfil_permissao (perfil_id, permissao_codigo)
SELECT p.id, pm.codigo
FROM perfil p
JOIN permissao pm ON pm.codigo IN ('PRODUTO_LISTAR', 'PRODUTO_GERENCIAR', 'ESTOQUE_LISTAR', 'ESTOQUE_AJUSTAR',
                                   'COMPRA_LISTAR', 'COMPRA_REGISTRAR')
WHERE p.nome = 'Estoquista';

INSERT INTO perfil_permissao (perfil_id, permissao_codigo)
SELECT p.id, pm.codigo
FROM perfil p
JOIN permissao pm ON pm.codigo IN ('VENDA_LISTAR', 'VENDA_REGISTRAR', 'VENDA_CANCELAR', 'PRODUTO_LISTAR',
                                   'PRODUTO_GERENCIAR', 'ESTOQUE_LISTAR', 'ESTOQUE_AJUSTAR', 'COMPRA_LISTAR',
                                   'COMPRA_REGISTRAR', 'RELATORIO_VISUALIZAR')
WHERE p.nome = 'Gerente';

-- Superadmin recebe todo o catálogo (inclusive as permissões acima).
INSERT INTO perfil_permissao (perfil_id, permissao_codigo)
SELECT p.id, pm.codigo
FROM perfil p
CROSS JOIN permissao pm
WHERE p.sistema = TRUE
ON CONFLICT DO NOTHING;
