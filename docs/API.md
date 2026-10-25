# Contrato da API — Gestão Supermercado

Referência para quem vai construir o front: como autenticar, as convenções da API e todos os endpoints, com
permissões, corpos de requisição, respostas e regras que afetam as telas.

- **Contrato exato (máquina):** [`openapi.json`](openapi.json) — OpenAPI 3, gerado pela própria aplicação. Use-o para
  gerar tipos e clientes (ex.: `npx openapi-typescript docs/openapi.json -o src/api/schema.d.ts`).
- **Explorar e testar:** com a API rodando localmente, `http://localhost:8080/swagger-ui.html` (botão *Authorize* aceita
  o `accessToken`). Em produção o Swagger fica desligado.
- **Prefixo de todas as rotas:** `/api/v1`. Os exemplos abaixo omitem o host.

## Sumário

1. [Autenticação e sessão](#1-autenticação-e-sessão)
2. [Convenções](#2-convenções)
3. [Perfis e permissões (o que mostrar para quem)](#3-perfis-e-permissões-o-que-mostrar-para-quem)
4. [Endpoints](#4-endpoints)
   - [Auth](#41-auth) · [Usuários](#42-usuários) · [Perfis e permissões](#43-perfis-e-permissões) ·
     [Produtos](#44-produtos) · [Estoque](#45-estoque) · [Compras](#46-compras) · [Vendas](#47-vendas)
5. [Enums](#5-enums)
6. [Pendências conhecidas](#6-pendências-conhecidas)
7. [Mantendo este documento atualizado](#7-mantendo-este-documento-atualizado)

---

## 1. Autenticação e sessão

A API é stateless: toda rota, exceto login, refresh e logout, exige o header

```
Authorization: Bearer <accessToken>
```

### Fluxo

1. **Login** — `POST /auth/login` com e-mail e senha. Retorna `accessToken` (JWT, válido por **15 minutos**,
   `expiresIn` em segundos) e `refreshToken` (opaco, válido por **7 dias**, **uso único**).
2. **Carregar o usuário** — `GET /core/usuarios/me`. Retorna nome, perfis e a lista de **permissões efetivas**. O front
   decide menus, telas e botões **pelas permissões**, nunca pelo nome do perfil (o Superadmin pode criar perfis novos).
3. **Renovar** — quando a API responder `401` (ou pouco antes de `expiresIn`), chame `POST /auth/refresh` com o
   `refreshToken`. A resposta traz um **novo par**: guarde os dois; o refresh antigo deixa de valer.
4. **Sair** — `POST /auth/logout` com o `refreshToken`.

### Regras que o front precisa respeitar

- **Refresh é de uso único e rotativo.** Reutilizar um refresh já usado é tratado como roubo de sessão: **todas** as
  sessões do usuário são encerradas e o refresh novo também deixa de valer. Se duas abas renovarem ao mesmo tempo, uma
  delas vai derrubar a outra; centralize a renovação (uma única promessa de refresh em andamento compartilhada pelas
  requisições).
- **Permissões são lidas a cada requisição.** Mudar o perfil de alguém, inativar o perfil ou o usuário vale na hora.
  Trate `403` em qualquer tela (a permissão pode ter sido retirada com a tela aberta) e recarregue `/me` quando receber
  um `403` inesperado.
- **Trocar ou redefinir a senha encerra todas as sessões** do usuário (inclusive a atual): depois de
  `PUT /core/usuarios/me/senha`, mande o usuário para o login.
- **Bloqueio por tentativas:** após **5** senhas erradas seguidas, o login fica bloqueado por **15 minutos**. A
  mensagem de erro é sempre a mesma (`E-mail ou senha inválidos.`) e não revela se o e-mail existe nem se está
  bloqueado.
- `401` com o corpo `{"mensagem": "Autenticação necessária ou sessão expirada."}` significa token ausente, inválido,
  expirado, de usuário inativo ou emitido antes de uma troca de senha → tente o refresh uma vez; se falhar, login.

---

## 2. Convenções

### Requisições

- Corpo sempre `Content-Type: application/json` em **UTF-8**.
- Texto é aparado (`strip`) pelo backend; strings opcionais em branco viram `null`.
- E-mails são guardados em minúsculas.

### Status HTTP

| Status | Quando |
|--------|--------|
| `200` | Consulta ou edição com corpo de resposta |
| `201` | Criação (corpo traz o recurso criado) |
| `204` | Operação sem corpo (troca de status, senha, logout) |
| `400` | JSON malformado ou valor de enum inexistente (`"Corpo da requisição inválido."`) |
| `401` | Não autenticado / sessão expirada / credenciais inválidas no login |
| `403` | Autenticado, mas sem a permissão exigida |
| `404` | Recurso (ou recurso referenciado no corpo, como `produtoId`) não existe |
| `409` | Conflito: valor único repetido (e-mail, código de barras, CNPJ, nome de perfil) ou registro alterado ao mesmo tempo por outra operação — **ofereça "tentar de novo"** |
| `422` | Validação de campos ou regra de negócio (estoque insuficiente, produto inativo...) |
| `500` | Erro inesperado; detalhes só no log do servidor |

### Formato de erro (todos os status de erro)

```json
{
  "status": 422,
  "mensagem": "Dados inválidos.",
  "campos": [
    { "campo": "motivo", "mensagem": "não deve estar em branco" }
  ],
  "timestamp": "2026-10-25T14:46:30.767-03:00"
}
```

- `mensagem` é sempre legível em português e pode ser mostrada ao usuário.
- `campos` só vem preenchido em erro de validação (`422` com `"Dados inválidos."`); o `campo` é o nome do campo do
  JSON (em listas: `itens[0].quantidade`). Use para marcar o campo no formulário. Nos demais erros vem `[]`.

### Paginação

Listagens paginadas aceitam os parâmetros de query:

| Parâmetro | Exemplo | Observação |
|-----------|---------|------------|
| `page` | `0` | Base 0 |
| `size` | `20` | Padrão 20 |
| `sort` | `nome,asc` | Pode repetir. Campos não permitidos são ignorados em silêncio (vale a ordenação padrão do endpoint) |

E respondem:

```json
{
  "conteudo": [ ... ],
  "pagina": 0,
  "tamanho": 20,
  "totalItens": 57,
  "totalPaginas": 3
}
```

Exceções sem paginação (devolvem lista simples): `GET /core/perfis` e `GET /core/permissoes`.

### Datas e horas

- Respostas: ISO-8601 **com fuso** (`2026-10-25T14:46:31.191-03:00` ou `2026-10-25T17:46:31.191Z`). O mesmo instante
  pode vir com fusos diferentes conforme o endpoint; sempre converta com uma biblioteca de datas antes de comparar ou
  exibir.
- Filtros de período (`dataInicio`, `dataFim`): ISO-8601 com fuso, **inclusivos**. Ex.: o dia 25/10 em Brasília é
  `dataInicio=2026-10-25T00:00:00-03:00&dataFim=2026-10-25T23:59:59.999-03:00`. Se usar fuso positivo (`+`), codifique
  como `%2B` na URL. `dataInicio` depois de `dataFim` → `422`.

### Números

- Todos os valores numéricos são **números JSON** (não strings). Use uma biblioteca decimal (ou trabalhe em centavos)
  para somar dinheiro no front; o backend já entrega os totais calculados.
- **Dinheiro:** 2 casas decimais (`precoVenda`, `valorTotal`, `troco`...).
- **Quantidade:** até 3 casas decimais (gramas/mililitros). Pode vir com zeros à direita (`24.000`, `2.550`) — formate
  para exibir. Produto vendido por unidade (`UN`) só aceita quantidade inteira.
- Arredondamento de subtotais: meio-par (*banker's rounding*) para 2 casas.

### Exclusão

Nada é excluído pela API. Usuários, perfis, produtos e fornecedores são **inativados**
(`PUT .../{id}/status` com `{"ativo": false}`); vendas são **canceladas**; compras e movimentações de estoque são
imutáveis.

---

## 3. Perfis e permissões (o que mostrar para quem)

O front deve habilitar cada área pela **permissão** presente em `GET /core/usuarios/me` → `permissoes`.

| Permissão | Libera | Caixa | Estoquista | Gerente | Superadmin |
|-----------|--------|:-----:|:----------:|:-------:|:----------:|
| `VENDA_LISTAR` | Consultar vendas | ✔ | | ✔ | ✔ |
| `VENDA_REGISTRAR` | Registrar venda (tela do caixa) | ✔ | | ✔ | ✔ |
| `VENDA_CANCELAR` | Cancelar / desfazer venda | ✔ | | ✔ | ✔ |
| `PRODUTO_LISTAR` | Consultar produtos, preço e estoque | ✔ | ✔ | ✔ | ✔ |
| `PRODUTO_GERENCIAR` | Cadastrar/editar/inativar produto; ver `precoCusto` | | ✔ | ✔ | ✔ |
| `ESTOQUE_LISTAR` | Histórico de estoque e alerta de reposição | | ✔ | ✔ | ✔ |
| `ESTOQUE_AJUSTAR` | Ajuste manual de estoque | | ✔ | ✔ | ✔ |
| `COMPRA_LISTAR` | Consultar compras e fornecedores | | ✔ | ✔ | ✔ |
| `COMPRA_REGISTRAR` | Lançar compra; cadastrar/editar fornecedor | | ✔ | ✔ | ✔ |
| `RELATORIO_VISUALIZAR` | Relatórios (módulo ainda não implementado) | | | ✔ | ✔ |
| `USUARIO_LISTAR` | Listar usuários | | | | ✔ |
| `USUARIO_CRIAR` | Cadastrar usuário | | | | ✔ |
| `USUARIO_EDITAR` | Editar usuário e seus perfis | | | | ✔ |
| `USUARIO_ALTERAR_STATUS` | Ativar/inativar usuário | | | | ✔ |
| `USUARIO_REDEFINIR_SENHA` | Redefinir senha de outro usuário | | | | ✔ |
| `PERFIL_LISTAR` | Listar perfis | | | | ✔ |
| `PERFIL_CRIAR` | Cadastrar perfil | | | | ✔ |
| `PERFIL_EDITAR` | Editar perfil e permissões | | | | ✔ |
| `PERFIL_ALTERAR_STATUS` | Ativar/inativar perfil | | | | ✔ |
| `PERMISSAO_LISTAR` | Catálogo de permissões (tela de perfis) | | | | ✔ |

A tabela mostra o padrão; o Superadmin pode mudar as permissões de Caixa, Estoquista e Gerente e criar perfis novos.

Sem permissão nenhuma: `GET /core/usuarios/me` e `PUT /core/usuarios/me/senha` (qualquer usuário autenticado).

---

## 4. Endpoints

Legenda: 🔓 público · 👤 qualquer usuário autenticado · demais exigem a permissão indicada.

### 4.1 Auth

| Método | Rota | Acesso | Corpo | Resposta |
|--------|------|--------|-------|----------|
| `POST` | `/auth/login` | 🔓 | `LoginRequest` | `200 TokenResponse` · `401` · `422` |
| `POST` | `/auth/refresh` | 🔓 | `RefreshTokenRequest` | `200 TokenResponse` · `401` |
| `POST` | `/auth/logout` | 🔓 | `RefreshTokenRequest` | `204` (idempotente) |

```jsonc
// LoginRequest
{ "email": "maria@mercado.com", "senha": "SenhaForte1" }

// RefreshTokenRequest
{ "refreshToken": "q3J...opaco" }

// TokenResponse
{ "accessToken": "eyJhbGciOiJIUzI1NiJ9...", "tokenType": "Bearer", "expiresIn": 900, "refreshToken": "q3J...opaco" }
```

### 4.2 Usuários

| Método | Rota | Permissão | Corpo / Query | Resposta |
|--------|------|-----------|---------------|----------|
| `GET` | `/core/usuarios/me` | 👤 | — | `200 UsuarioLogadoResponse` |
| `PUT` | `/core/usuarios/me/senha` | 👤 | `SenhaAlteracaoRequest` | `204` · `422` (senha atual incorreta / fora da política) |
| `GET` | `/core/usuarios` | `USUARIO_LISTAR` | query: `nome`, `email` (contém), `ativo`, `perfilId` + paginação (padrão `sort=nome,asc`; ordenáveis: `nome`, `email`, `ativo`, `criadoEm`, `ultimoLoginEm`) | `200 Pagina<UsuarioResponse>` |
| `GET` | `/core/usuarios/usuario/{id}` | `USUARIO_LISTAR` | — | `200 UsuarioResponse` · `404` |
| `POST` | `/core/usuarios/usuario` | `USUARIO_CRIAR` | `UsuarioCreateRequest` | `201 UsuarioResponse` · `404` perfil · `409` e-mail · `422` |
| `PUT` | `/core/usuarios/usuario/{id}` | `USUARIO_EDITAR` | `UsuarioUpdateRequest` | `200 UsuarioResponse` · `404` · `409` · `422` |
| `PUT` | `/core/usuarios/usuario/{id}/status` | `USUARIO_ALTERAR_STATUS` | `{"ativo": false}` | `204` · `404` · `422` |
| `PUT` | `/core/usuarios/usuario/{id}/senha` | `USUARIO_REDEFINIR_SENHA` | `{"novaSenha": "..."}` | `204` · `404` · `422` |

```jsonc
// UsuarioLogadoResponse
{
  "id": 7, "nome": "Maria Souza", "email": "maria@mercado.com",
  "perfis": [ { "id": 3, "nome": "Caixa", "ativo": true } ],
  "permissoes": [ "VENDA_LISTAR", "VENDA_REGISTRAR", "VENDA_CANCELAR", "PRODUTO_LISTAR" ]
}

// UsuarioResponse
{
  "id": 7, "nome": "Maria Souza", "email": "maria@mercado.com", "ativo": true,
  "ultimoLoginEm": "2026-10-25T14:00:00Z", "criadoEm": "2026-10-21T18:00:00Z",
  "perfis": [ { "id": 3, "nome": "Caixa", "ativo": true } ]
}

// UsuarioCreateRequest (UsuarioUpdateRequest é igual, sem "senha"; perfilIds substitui os atuais)
{ "nome": "Maria Souza", "email": "maria@mercado.com", "senha": "Troque123", "perfilIds": [3] }

// SenhaAlteracaoRequest
{ "senhaAtual": "SenhaAntiga1", "novaSenha": "NovaSenha1" }
```

Regras:

- **Política de senha:** 8 a 72 caracteres, com pelo menos uma letra e um número.
- `perfilIds` é obrigatório e não pode ser vazio; perfil inativo é recusado (`422`).
- Ninguém pode ativar/inativar o próprio usuário (`422`).
- O sistema recusa qualquer operação que deixe o sistema sem nenhum Superadmin ativo (`422`).
- Inativar usuário ou redefinir sua senha encerra as sessões dele.

### 4.3 Perfis e permissões

| Método | Rota | Permissão | Corpo / Query | Resposta |
|--------|------|-----------|---------------|----------|
| `GET` | `/core/perfis` | `PERFIL_LISTAR` | query opcional `ativo` | `200 PerfilResponse[]` (sem paginação) |
| `GET` | `/core/perfis/perfil/{id}` | `PERFIL_LISTAR` | — | `200 PerfilResponse` · `404` |
| `POST` | `/core/perfis/perfil` | `PERFIL_CRIAR` | `PerfilRequest` | `201 PerfilResponse` · `409` nome · `422` |
| `PUT` | `/core/perfis/perfil/{id}` | `PERFIL_EDITAR` | `PerfilRequest` | `200 PerfilResponse` · `404` · `409` · `422` |
| `PUT` | `/core/perfis/perfil/{id}/status` | `PERFIL_ALTERAR_STATUS` | `{"ativo": false}` | `204` · `404` · `422` |
| `GET` | `/core/permissoes` | `PERMISSAO_LISTAR` | — | `200 PermissaoResponse[]` (sem paginação) |

```jsonc
// PerfilRequest (permissoes substitui as atuais)
{ "nome": "Fiscal de Caixa", "descricao": "Supervisiona os caixas", "permissoes": ["VENDA_LISTAR", "VENDA_CANCELAR"] }

// PerfilResponse
{ "id": 3, "nome": "Caixa", "descricao": "Operador de caixa: registra vendas", "ativo": true, "sistema": false,
  "permissoes": ["VENDA_LISTAR", "VENDA_REGISTRAR", "VENDA_CANCELAR", "PRODUTO_LISTAR"] }

// PermissaoResponse — use "grupo" para agrupar os checkboxes na tela de perfil
{ "codigo": "VENDA_REGISTRAR", "descricao": "Registrar vendas no caixa", "grupo": "Vendas" }
```

Regras: perfil com `sistema: true` (Superadmin) não pode ser editado nem inativado (`422`) — desabilite os botões.
Perfil inativo não concede permissões a ninguém.

### 4.4 Produtos

| Método | Rota | Permissão | Corpo / Query | Resposta |
|--------|------|-----------|---------------|----------|
| `GET` | `/produto/produtos` | `PRODUTO_LISTAR` | query: `nome` (contém), `codigoBarras` (exato), `ativo`, `abaixoEstoqueMinimo` + paginação (padrão `sort=nome,asc`; ordenáveis: `nome`, `codigoBarras`, `precoVenda`, `quantidadeEstoque`, `ativo`, `criadoEm`) | `200 Pagina<ProdutoResponse>` |
| `GET` | `/produto/produtos/produto/{id}` | `PRODUTO_LISTAR` | — | `200 ProdutoResponse` · `404` |
| `POST` | `/produto/produtos/produto` | `PRODUTO_GERENCIAR` | `ProdutoRequest` | `201 ProdutoResponse` · `409` código de barras · `422` |
| `PUT` | `/produto/produtos/produto/{id}` | `PRODUTO_GERENCIAR` | `ProdutoRequest` | `200 ProdutoResponse` · `404` · `409` · `422` |
| `PUT` | `/produto/produtos/produto/{id}/status` | `PRODUTO_GERENCIAR` | `{"ativo": false}` | `204` · `404` |

```jsonc
// ProdutoRequest
{ "nome": "Arroz Tipo 1 5kg", "codigoBarras": "7891234567890", "unidadeMedida": "UN",
  "precoCusto": 18.50, "precoVenda": 24.90, "estoqueMinimo": 10 }

// ProdutoResponse
{ "id": 12, "nome": "Arroz Tipo 1 5kg", "codigoBarras": "7891234567890", "unidadeMedida": "UN",
  "precoCusto": 18.50,          // null para quem não tem PRODUTO_GERENCIAR (ex.: Caixa)
  "precoVenda": 24.90, "quantidadeEstoque": 42.000, "estoqueMinimo": 10.000,
  "abaixoEstoqueMinimo": false, "ativo": true, "criadoEm": "2026-10-21T22:00:00Z" }
```

Regras:

- **A quantidade em estoque não é editável aqui.** Produto nasce com estoque 0; o estoque muda por compra, venda ou
  ajuste (módulo Estoque).
- `codigoBarras` é opcional (itens a granel) e único. No caixa, busque o produto lido pelo leitor com
  `GET /produto/produtos?codigoBarras=...`.
- `precoCusto` é atualizado automaticamente a cada compra lançada (custo da última compra).
- Trocar a unidade para `UN` com estoque fracionado é recusado (`422`).
- Produto inativo não pode ser vendido, comprado nem ajustado.

### 4.5 Estoque

| Método | Rota | Permissão | Corpo / Query | Resposta |
|--------|------|-----------|---------------|----------|
| `POST` | `/estoque/movimentacoes/ajuste` | `ESTOQUE_AJUSTAR` | `AjusteEstoqueRequest` | `201 MovimentacaoEstoqueResponse` · `404` produto · `409` · `422` |
| `GET` | `/estoque/movimentacoes` | `ESTOQUE_LISTAR` | query: `produtoId`, `tipo`, `origem`, `dataInicio`, `dataFim` + paginação (padrão mais recente primeiro; ordenáveis: `criadoEm`, `quantidade`, `tipo`, `origem`, `produtoId`) | `200 Pagina<MovimentacaoEstoqueResponse>` |
| `GET` | `/estoque/movimentacoes/movimentacao/{id}` | `ESTOQUE_LISTAR` | — | `200 MovimentacaoEstoqueResponse` · `404` |
| `GET` | `/estoque/reposicao` | `ESTOQUE_LISTAR` | paginação (padrão `sort=nome,asc`) | `200 Pagina<ReposicaoResponse>` |

```jsonc
// AjusteEstoqueRequest
{ "produtoId": 12, "tipo": "SAIDA", "quantidade": 3, "motivo": "Embalagens avariadas no recebimento" }

// MovimentacaoEstoqueResponse
{ "id": 105, "produtoId": 12, "produtoNome": "Arroz Tipo 1 5kg", "tipo": "SAIDA", "origem": "AJUSTE",
  "quantidade": 3.000, "saldoApos": 39.000, "motivo": "Embalagens avariadas no recebimento",
  "referenciaId": null,   // id da compra (origem COMPRA) ou da venda (VENDA / ESTORNO_VENDA)
  "usuarioId": 3, "criadoEm": "2026-10-24T18:23:50.927Z" }

// ReposicaoResponse — produtos ativos abaixo do estoque mínimo
{ "produtoId": 12, "nome": "Arroz Tipo 1 5kg", "codigoBarras": "7891234567890", "unidadeMedida": "UN",
  "quantidadeEstoque": 7.000, "estoqueMinimo": 10.000, "quantidadeFaltante": 3.000 }
```

Regras: `motivo` obrigatório no ajuste. Saída maior que o estoque → `422 "Estoque insuficiente para a operação."`.
O histórico é imutável: para corrigir um ajuste errado, faça outro ajuste no sentido contrário. Use `referenciaId`
para linkar a movimentação à compra/venda de origem.

### 4.6 Compras

**Fornecedores**

| Método | Rota | Permissão | Corpo / Query | Resposta |
|--------|------|-----------|---------------|----------|
| `GET` | `/compra/fornecedores` | `COMPRA_LISTAR` | query: `nome` (contém), `cnpj` (exato, com ou sem pontuação), `ativo` + paginação (padrão `sort=nome,asc`) | `200 Pagina<FornecedorResponse>` |
| `GET` | `/compra/fornecedores/fornecedor/{id}` | `COMPRA_LISTAR` | — | `200 FornecedorResponse` · `404` |
| `POST` | `/compra/fornecedores/fornecedor` | `COMPRA_REGISTRAR` | `FornecedorRequest` | `201 FornecedorResponse` · `409` CNPJ · `422` |
| `PUT` | `/compra/fornecedores/fornecedor/{id}` | `COMPRA_REGISTRAR` | `FornecedorRequest` | `200 FornecedorResponse` · `404` · `409` · `422` |
| `PUT` | `/compra/fornecedores/fornecedor/{id}/status` | `COMPRA_REGISTRAR` | `{"ativo": false}` | `204` · `404` |

```jsonc
// FornecedorRequest — cnpj, telefone e email são opcionais
{ "nome": "Atacadão Irecê Ltda", "cnpj": "12.345.678/0001-90", "telefone": "(74) 99999-0000", "email": "vendas@atacadao.com" }

// FornecedorResponse — cnpj volta só com dígitos (formate para exibir)
{ "id": 4, "nome": "Atacadão Irecê Ltda", "cnpj": "12345678000190", "telefone": "(74) 99999-0000",
  "email": "vendas@atacadao.com", "ativo": true, "criadoEm": "2026-10-25T14:46:30.667-03:00" }
```

**Compras**

| Método | Rota | Permissão | Corpo / Query | Resposta |
|--------|------|-----------|---------------|----------|
| `POST` | `/compra/compras/compra` | `COMPRA_REGISTRAR` | `CompraRequest` | `201 CompraResponse` · `404` fornecedor/produto · `409` · `422` |
| `GET` | `/compra/compras` | `COMPRA_LISTAR` | query: `fornecedorId`, `numeroNotaFiscal` (exato), `dataInicio`, `dataFim` + paginação (padrão mais recente primeiro; ordenáveis: `criadoEm`, `valorTotal`, `numeroNotaFiscal`) | `200 Pagina<CompraResumoResponse>` |
| `GET` | `/compra/compras/compra/{id}` | `COMPRA_LISTAR` | — | `200 CompraResponse` · `404` |

```jsonc
// CompraRequest
{ "fornecedorId": 4, "numeroNotaFiscal": "000123456", "observacao": "Entrega parcial",
  "itens": [
    { "produtoId": 12, "quantidade": 24, "precoCustoUnitario": 19.90 },
    { "produtoId": 15, "quantidade": 2.5, "precoCustoUnitario": 32.00 }
  ] }

// CompraResponse
{ "id": 48, "fornecedorId": 4, "fornecedorNome": "Atacadão Irecê Ltda", "numeroNotaFiscal": "000123456",
  "valorTotal": 557.60, "observacao": "Entrega parcial", "usuarioId": 3, "criadoEm": "2026-10-25T14:46:31.191-03:00",
  "itens": [
    { "produtoId": 12, "produtoNome": "Arroz Tipo 1 5kg", "quantidade": 24, "precoCustoUnitario": 19.90, "subtotal": 477.60 },
    { "produtoId": 15, "produtoNome": "Queijo Coalho", "quantidade": 2.5, "precoCustoUnitario": 32.00, "subtotal": 80.00 }
  ] }

// CompraResumoResponse (listagem, sem itens)
{ "id": 48, "fornecedorId": 4, "fornecedorNome": "Atacadão Irecê Ltda", "numeroNotaFiscal": "000123456",
  "valorTotal": 557.60, "usuarioId": 3, "criadoEm": "2026-10-25T17:46:31.191Z" }
```

Regras:

- Lançar a compra dá **entrada no estoque** de cada item e atualiza o `precoCusto` do produto para o custo pago.
- **Tudo ou nada:** se qualquer item falhar (produto inexistente → `404`; inativo, quantidade fracionada em `UN` ou
  produto repetido → `422`), nada é gravado. Some quantidades do mesmo produto em um único item.
- Fornecedor inativo não recebe compras (`422`).
- Compra não é editada nem cancelada; erro de lançamento se corrige com ajuste de estoque.
- Máximo de 200 itens por compra.

### 4.7 Vendas

| Método | Rota | Permissão | Corpo / Query | Resposta |
|--------|------|-----------|---------------|----------|
| `POST` | `/venda/vendas/venda` | `VENDA_REGISTRAR` | `VendaRequest` | `201 VendaResponse` · `404` produto · `409` · `422` |
| `GET` | `/venda/vendas` | `VENDA_LISTAR` | query: `status`, `formaPagamento`, `usuarioId`, `dataInicio`, `dataFim` + paginação (padrão mais recente primeiro; ordenáveis: `criadoEm`, `valorTotal`, `status`, `formaPagamento`) | `200 Pagina<VendaResumoResponse>` |
| `GET` | `/venda/vendas/venda/{id}` | `VENDA_LISTAR` | — | `200 VendaResponse` · `404` |
| `PUT` | `/venda/vendas/venda/{id}/cancelamento` | `VENDA_CANCELAR` | `CancelamentoVendaRequest` | `200 VendaResponse` · `404` · `409` · `422` já cancelada |

```jsonc
// VendaRequest — o front NÃO envia preço: ele vem do cadastro do produto
{ "formaPagamento": "DINHEIRO",
  "valorRecebido": 100.00,          // opcional, só considerado em DINHEIRO
  "itens": [
    { "produtoId": 12, "quantidade": 3 },
    { "produtoId": 15, "quantidade": 0.450 }
  ] }

// VendaResponse
{ "id": 305, "status": "CONCLUIDA", "formaPagamento": "DINHEIRO",
  "valorTotal": 97.95, "valorRecebido": 100.00, "troco": 2.05,   // valorRecebido/troco: null fora do dinheiro
  "usuarioId": 3, "criadoEm": "2026-10-25T14:57:35.596-03:00",
  "canceladoEm": null, "canceladoPorUsuarioId": null, "motivoCancelamento": null,
  "itens": [
    { "produtoId": 12, "produtoNome": "Arroz 5kg", "quantidade": 3, "precoUnitario": 25.90, "subtotal": 77.70 },
    { "produtoId": 15, "produtoNome": "Queijo Coalho", "quantidade": 0.450, "precoUnitario": 45.00, "subtotal": 20.25 }
  ] }

// VendaResumoResponse (listagem, sem itens)
{ "id": 305, "status": "CONCLUIDA", "formaPagamento": "DINHEIRO", "valorTotal": 97.95, "usuarioId": 3,
  "criadoEm": "2026-10-25T17:57:35.596Z" }

// CancelamentoVendaRequest — motivo opcional
{ "motivo": "Cliente desistiu da compra" }
```

Regras:

- **Fluxo do caixa sugerido:** monte o carrinho no front (buscando produtos por código de barras ou nome em
  `/produto/produtos`, que já trazem `precoVenda` e `quantidadeEstoque`), mostre o total calculado localmente como
  prévia e envie a venda inteira em um único `POST` ao confirmar. O valor oficial é o da resposta.
- Registrar dá **baixa no estoque** de cada item. **Tudo ou nada:** se faltar estoque de qualquer item → `422
  "Estoque insuficiente para a operação."` e nada é gravado. Produto inexistente → `404`; inativo, repetido ou
  quantidade fracionada em `UN` → `422`.
- Dinheiro com `valorRecebido` menor que o total → `422 "O valor recebido é menor que o total da venda."`.
- **Desfazer / cancelar:** o mesmo endpoint de cancelamento serve para o botão "Desfazer" logo após confirmar e para
  cancelar uma venda antiga. Os itens voltam ao estoque. Uma venda já cancelada → `422`.
- O custo dos itens nunca é devolvido nas respostas de venda.
- Máximo de 200 itens por venda.

---

## 5. Enums

| Enum | Valores |
|------|---------|
| `Permissao` | ver [seção 3](#3-perfis-e-permissões-o-que-mostrar-para-quem) |
| `UnidadeMedida` | `UN` (unidade, quantidade inteira) · `KG` · `L` |
| `TipoMovimentacao` | `ENTRADA` · `SAIDA` |
| `OrigemMovimentacao` | `AJUSTE` · `COMPRA` · `VENDA` · `ESTORNO_VENDA` |
| `FormaPagamento` | `DINHEIRO` · `CARTAO_DEBITO` · `CARTAO_CREDITO` · `PIX` |
| `StatusVenda` | `CONCLUIDA` · `CANCELADA` |

Valor fora da lista no corpo → `400 "Corpo da requisição inválido."`.

---

## 6. Pendências conhecidas

- **CORS não está configurado.** Um front servido em outra origem (ex.: `http://localhost:5173`) terá as chamadas
  bloqueadas pelo navegador. Até ser configurado no backend, use o proxy do servidor de desenvolvimento do front (ex.:
  `server.proxy` no Vite apontando `/api` para `http://localhost:8080`).
- **Relatórios (`RELATORIO_VISUALIZAR`)** ainda não têm endpoints: faturamento por período, margem e PDF.
- Respostas trazem `usuarioId` (quem vendeu, comprou, ajustou), mas não o nome do usuário. Para exibir nomes, quem tem
  `USUARIO_LISTAR` pode consultar `/core/usuarios`; para os demais perfis ainda não há endpoint.

---

## 7. Mantendo este documento atualizado

Ao criar ou alterar um endpoint, atualize a seção correspondente aqui e regenere o `openapi.json`:

1. Suba a API localmente (`./mvnw spring-boot:run`).
2. `curl -s http://localhost:8080/v3/api-docs -o docs/openapi.json`
3. Remova o bloco `servers` (ele aponta para a máquina de quem gerou) e formate o JSON antes de commitar.
