# Gestão Supermercado

Backend da gestão de supermercado. Spring Boot 4, Java 21, PostgreSQL, Flyway e JWT.

## Módulos

O código é organizado por módulo de negócio em `src/main/java/br/com/gestaosupermercado/modules/<modulo>/<subdominio>/`,
cada subdomínio com `controller`, `service` (interface + `Impl`), `repository`, `model`, `dto/request`, `dto/response`
e, quando preciso, `enums` e `listener`. O que é transversal fica em `shared/`.

### Core (`modules/core`)

Identidade e controle de acesso:

| Subdomínio  | Responsabilidade |
|-------------|------------------|
| `auth`      | Login, renovação (refresh token rotativo) e logout |
| `usuario`   | Cadastro de usuários, troca/redefinição de senha, `/me` |
| `perfil`    | Perfis de acesso (cargos: Gerente, Caixa, Estoquista...) e suas permissões |
| `permissao` | Catálogo de permissões |

**Modelo de acesso (RBAC dinâmico):** usuário → N perfis → N permissões. As permissões **não** vão dentro do JWT; são
lidas do banco a cada requisição. Por isso mudar o perfil de alguém, inativar um perfil ou inativar o usuário vale na
hora, sem esperar o token expirar. Trocar a senha invalida todos os tokens emitidos antes.

Regras de segurança já implementadas:

- Senha com BCrypt; política única (`@SenhaForte`): 8 a 72 caracteres, com letra e número.
- Bloqueio temporário após tentativas falhas (`app.security.login`).
- Mensagem de erro de login genérica (não revela se o e-mail existe).
- Access token curto (15 min) + refresh token opaco de uso único; reutilizar um refresh já usado revoga todas as
  sessões do usuário. No banco fica só o hash SHA-256 do refresh token.
- O perfil **Superadmin** é do sistema: não pode ser editado nem inativado, e o sistema recusa qualquer operação
  que o deixe sem nenhum Superadmin ativo.

### Produto (`modules/produto`)

Cadastro de produtos (`/api/v1/produto/produtos`), base de Estoque, Vendas, Compras e Relatórios. Consulta exige
`PRODUTO_LISTAR`; cadastro, edição e inativação exigem `PRODUTO_GERENCIAR`. Produto não é excluído, só inativado,
para não quebrar vendas e relatórios antigos.

Para os outros módulos:

- Obtenha o produto com `ProdutoService.findProdutoAtivoForMovimentacao(id)` dentro da sua transação.
- Mova o estoque só por `Produto.addEstoque` / `Produto.removeEstoque`: recusam quantidade zero/negativa, quantidade
  fracionada em produto vendido por unidade (`UN`) e saída maior que o disponível.
- Movimentações concorrentes do mesmo produto falham com 409 (lock otimista) em vez de se sobrescreverem.
- Relatórios de margem usam `precoCusto` e `precoVenda`. Na API de produtos, `precoCusto` só aparece para quem tem
  `PRODUTO_GERENCIAR` (o Caixa recebe `null`).

### Estoque (`modules/estoque`)

| Subdomínio     | Responsabilidade |
|----------------|------------------|
| `movimentacao` | Ajuste manual (`POST /api/v1/estoque/movimentacoes/ajuste`) e histórico de entradas e saídas (`GET /api/v1/estoque/movimentacoes`) |
| `reposicao`    | Alerta de reposição: produtos ativos abaixo do estoque mínimo e quanto falta (`GET /api/v1/estoque/reposicao`) |

Consulta exige `ESTOQUE_LISTAR`; ajuste exige `ESTOQUE_AJUSTAR` e um motivo (perda, avaria, inventário...).
Cada movimentação guarda tipo (`ENTRADA`/`SAIDA`), origem (`AJUSTE`, `COMPRA`, `VENDA`, `ESTORNO_VENDA`), o saldo do
produto logo depois dela e o usuário que a fez. O histórico é imutável: um erro se corrige com outro ajuste.

Para Compras e Vendas, a regra acima fica assim: **não** chame `Produto.addEstoque`/`removeEstoque` direto; use
`MovimentacaoEstoqueService.saveMovimentacao(produtoId, tipo, quantidade, origem, referenciaId, usuarioId)` dentro da
transação da compra/venda, passando o id dela em `referenciaId`. Assim toda mudança de estoque fica no histórico.

### Perfis

| Perfil | Pode fazer |
|--------|------------|
| **Caixa** | Somente vendas: registrar, consultar e cancelar vendas (consulta produtos para vender, sem alterá-los) |
| **Estoquista** | Somente estoque: produtos, estoque e compras de fornecedores |
| **Gerente** | Tudo do Caixa e do Estoquista, mais relatórios (faturamento, margem, PDF). Não gere usuários nem perfis |
| **Superadmin** | Tudo do sistema, inclusive usuários e perfis. Recebe toda permissão nova automaticamente, pelo código |

Os motivos dessa divisão estão em [ADR-001](docs/adr/ADR-001-perfis-e-permissoes.md).

## Como adicionar uma permissão nova

1. Adicione a constante no enum `Permissao`.
2. Crie uma migration que insere a permissão no catálogo (`ON CONFLICT DO NOTHING`) e a concede aos perfis que
   devem tê-la (Caixa, Estoquista e/ou Gerente). O Superadmin já recebe todas pelo código.
3. Use no endpoint com `@PreAuthorize("hasAuthority('NOVA_PERMISSAO')")`.

A aplicação não sobe se o enum e a tabela `permissao` estiverem diferentes (`PermissaoCatalogoValidator`).

## Rodando localmente

Pré-requisitos: JDK 21+ e Docker.

1. Copie `.env.example` para `.env` e preencha `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL` e `ADMIN_SENHA`.
2. Suba o banco: `docker compose up -d db`
3. Rode a API: `./mvnw spring-boot:run`

Na primeira subida, com o banco vazio, o Superadmin é criado com `ADMIN_EMAIL`/`ADMIN_SENHA`.
Documentação da API: http://localhost:8080/swagger-ui.html (ou a porta definida em `SERVER_PORT`)

Para subir banco e API juntos em containers (como em um servidor): `docker compose up -d --build`.

## Deploy

Passo a passo para publicar de graça (Render + Neon) em [docs/DEPLOY.md](docs/DEPLOY.md).

## Testes

```bash
./mvnw verify
```

Roda os testes unitários e os de integração (`*IT`), que sobem um PostgreSQL via Testcontainers (requer Docker).
