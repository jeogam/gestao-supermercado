# ADR-002: Cadastro público cria conta sem perfil

- **Status:** Aceita
- **Data:** 2026-10-25

## Contexto

O front-end tem uma tela "Criar conta". Até aqui, só o Superadmin criava usuários
(`POST /api/v1/core/usuarios/usuario`), escolhendo os perfis de cada um (ver ADR-001).

Uma rota pública de cadastro pode ser chamada por qualquer pessoa na internet. Se a conta nova já recebesse um
perfil do mercado, quem se cadastrasse teria acesso a vendas, estoque ou relatórios sem o dono saber.

## Decisão

`POST /api/v1/auth/cadastro` é público e cria a conta **ativa e sem nenhum perfil**:

- a pessoa consegue entrar, mas não tem nenhuma permissão (o `/me` devolve `perfis` e `permissoes` vazios);
- o Superadmin libera o acesso vinculando um perfil (Caixa, Estoquista ou Gerente) pela gestão de usuários;
- as regras de nome, e-mail e senha são as mesmas do cadastro feito pelo Superadmin (`@SenhaForte`, e-mail
  normalizado e único);
- a resposta é `201` sem corpo; e-mail repetido responde `409`.

## Consequências

- O front mostra "aguardando liberação" quando o usuário entra sem nenhuma permissão.
- Nenhum perfil novo nem migration foi necessário.
- O `409` revela que um e-mail já tem conta. O login evita isso de propósito, mas aqui foi aceito: sem envio de
  e-mail no sistema, não há outro jeito de avisar a pessoa que ela já tem cadastro.
- Não há limite de cadastros por IP. Se a API for exposta publicamente, isso deve ser feito na borda (proxy ou
  hospedagem) ou com um rate limit na aplicação.

## Alternativas consideradas

- **Conta nova como Caixa.** Daria acesso a vendas a qualquer pessoa que se cadastrasse. Descartado.
- **Conta nova inativa até aprovação.** Seguro, mas o login responde a mesma mensagem genérica de "e-mail ou
  senha inválidos" para usuário inativo (para não revelar contas), e a pessoa acharia que errou a senha.
  Descartado em favor da conta ativa sem permissões, que tem o mesmo efeito de segurança e uma mensagem clara.
- **Primeiro cadastro vira Superadmin.** O Superadmin inicial já é criado pelo `SuperadminInicialLoader`;
  repetir a regra aqui abriria uma corrida pela conta de administrador num banco novo.
