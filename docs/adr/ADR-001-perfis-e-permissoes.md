# ADR-001: Perfis de acesso e divisão das permissões

- **Status:** Aceita
- **Data:** 2026-10-21

## Contexto

O sistema atende pequenos mercados de bairro, com o dono e poucos funcionários (ver Entrega 05, Visão da
Experiência do Usuário). O protótipo organiza o sistema em cinco áreas: Vendas, Produtos, Estoque, Compras e
Relatórios. A gestão de acesso (usuários e perfis) é uma sexta área, que não aparece no menu do dia a dia.

O controle de acesso é feito por RBAC dinâmico: o usuário tem um ou mais perfis, cada perfil tem permissões, e
as permissões são lidas do banco a cada requisição.

## Decisão

O sistema terá três perfis do mercado e um perfil do sistema:

| Perfil | Áreas | Permissões |
|--------|-------|------------|
| **Caixa** | Vendas | `VENDA_LISTAR`, `VENDA_REGISTRAR`, `VENDA_CANCELAR`, `PRODUTO_LISTAR` |
| **Estoquista** | Produtos, Estoque, Compras | `PRODUTO_LISTAR`, `PRODUTO_GERENCIAR`, `ESTOQUE_LISTAR`, `ESTOQUE_AJUSTAR`, `COMPRA_LISTAR`, `COMPRA_REGISTRAR` |
| **Gerente** | Tudo do Caixa e do Estoquista, mais Relatórios | As 9 acima e `RELATORIO_VISUALIZAR` |
| **Superadmin** | Tudo, inclusive usuários e perfis | Todas, concedidas pelo código |

### 1. O Caixa consulta produtos, mas não os altera

Para vender, o Caixa precisa buscar o produto pelo nome ou pelo código de barras e ver preço e estoque
disponível (requisitos RU01 e RU06). Por isso recebe `PRODUTO_LISTAR`. Cadastrar, editar ou inativar produtos
(`PRODUTO_GERENCIAR`) fica fora do perfil, porque mexer em preço e cadastro não faz parte da venda.

### 2. Compras ficam com o Estoquista

No sistema, lançar uma compra de fornecedor é dar entrada de mercadoria no estoque. Separar compras em outro
perfil dividiria uma mesma tarefa entre duas pessoas. Por isso `COMPRA_LISTAR` e `COMPRA_REGISTRAR` pertencem ao
Estoquista (e, por consequência, ao Gerente).

### 3. O Caixa pode cancelar e desfazer vendas

O protótipo prevê "Cancelar venda" em todas as etapas e "Desfazer" por alguns segundos após a confirmação
(requisito RU04, recuperação de erros). Quem registra a venda precisa poder corrigi-la sem chamar outra pessoa,
então `VENDA_CANCELAR` faz parte do Caixa.

### 4. Gerente não gere acesso

O Gerente vê tudo da operação e os relatórios, mas não lista nem altera usuários e perfis. Gestão de acesso é
exclusiva do Superadmin.

### 5. Superadmin recebe todas as permissões pelo código

O Superadmin é o perfil do sistema (`perfil.sistema = true`): não pode ser editado nem inativado, e o sistema
não aceita ficar sem nenhum Superadmin ativo. Suas permissões são `Permissao.values()` no código, e não a
tabela `perfil_permissao`. Assim, uma permissão nova nunca fica de fora dele por esquecimento na migration.

## Consequências

- Cada módulo novo (vendas, produtos, estoque, compras, relatórios) protege seus endpoints com as permissões
  acima; não é preciso criar permissões novas para o básico dessas áreas.
- Uma permissão nova entra no enum `Permissao` e numa migration que a concede aos perfis do mercado que devem
  tê-la. O Superadmin não precisa de concessão.
- O Superadmin pode ajustar Caixa, Estoquista e Gerente pela API, ou criar outros perfis, sem deploy.

## Alternativas consideradas

- **Cancelar venda confirmada só pelo Gerente.** Daria mais controle contra fraude no caixa, mas obrigaria o
  Caixa a chamar o Gerente para corrigir qualquer erro, o que contraria o RU04. Se for necessário no futuro,
  basta criar uma permissão separada (ex.: `VENDA_ESTORNAR`) para cancelar vendas já confirmadas, concedida só
  ao Gerente, mantendo `VENDA_CANCELAR` para a venda em andamento e o "Desfazer" imediato.
- **Perfil próprio para Compras.** Descartado pelo motivo do item 2.
- **Gerente com gestão de usuários.** Descartado para manter a gestão de acesso concentrada no Superadmin.
