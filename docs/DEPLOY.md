# Deploy gratuito

Opção recomendada, sem custo e sem cartão de crédito: **Render** (API em Docker) + **Supabase** (PostgreSQL e,
depois, arquivos) + **cron-job.org** (ping que impede a API de dormir).

| Peça | Serviço | Limite do plano gratuito que importa aqui |
|------|---------|-------------------------------------------|
| API | Render, web service free | Dorme após 15 min sem requisições; 750 h/mês **por conta** (dá para 1 serviço ligado 24 h) |
| Banco | Supabase, plano free | 500 MB de banco; sempre ligado, mas o projeto é **pausado após 7 dias sem uso** |
| Arquivos | Supabase Storage | 1 GB (imagens e documentos; o banco guarda só o caminho do arquivo) |
| Ping | cron-job.org | Chamada a cada 10 min: mantém a API acordada e o Supabase ativo |

> O Postgres gratuito do próprio Render é apagado 30 dias após a criação, por isso o banco fica no Supabase.
> Confira os limites atuais nos sites: eles mudam com frequência.

## 1. Banco no Supabase

1. Crie uma conta em https://supabase.com e um projeto na região **West US (Oregon)**, a mesma do Render.
   Guarde a senha do banco definida na criação.
2. Em **Connect**, escolha **Session pooler** e copie host, porta, banco e usuário. Monte a URL JDBC, sem
   usuário e senha dentro dela:
   `jdbc:postgresql://<host>.pooler.supabase.com:5432/postgres?sslmode=require`
   - Use o **Session pooler**: a conexão direta do Supabase é só IPv6, e o Render não sai por IPv6.
   - Não use o **Transaction pooler** (porta 6543): ele não aceita os prepared statements do Hibernate.
   - O usuário do pooler tem o formato `postgres.<id-do-projeto>`.

As migrations do Flyway criam as tabelas na primeira subida da API.

## 2. API no Render

1. Crie uma conta em https://render.com e conecte o GitHub.
2. **New → Blueprint** e escolha este repositório. O Render lê o `render.yaml`.
3. Preencha as variáveis pedidas:
   - `DB_URL`: a URL JDBC do passo anterior
   - `DB_USERNAME` / `DB_PASSWORD`: usuário do pooler e senha do banco no Supabase
   - `ADMIN_EMAIL` / `ADMIN_SENHA`: o primeiro Superadmin, com senha de 8 a 72 caracteres, letra e número
     (só é usado enquanto não houver usuário; mudar depois não altera nada)
   - `CORS_ALLOWED_ORIGINS`: a URL do front publicado (ex.: `https://meu-front.vercel.app`); várias, separadas por vírgula
   - `JWT_SECRET` é gerado pelo Render automaticamente
4. Aguarde o deploy e teste: `https://<seu-app>.onrender.com/actuator/health/liveness` deve responder `{"status":"UP"}`.

Cada push na `main` gera um deploy novo (`autoDeploy: true`).

## 3. Não deixar a API dormir nem o Supabase pausar

1. Crie uma conta em https://cron-job.org e um job:
   - URL: `GET https://<seu-app>.onrender.com/actuator/health/readiness`
   - Frequência: a cada **10 minutos**
   - Timeout (em *Advanced*): o máximo, 30 s. Se a API estiver dormindo, ela demora para acordar.
   - Notificação por e-mail quando falhar.
2. Depois de uns 20 minutos, confira no histórico do job se as respostas são `200`.

O ping usa o `readiness` de propósito: ele **consulta o banco** (`application.yml`, grupo `readiness`), então
cada chamada conta como uso do Supabase e o projeto não é pausado. Pelo `liveness` a API ficaria acordada, mas
o Supabase pausaria após 7 dias sem uso real. O health check do próprio Render continua no `liveness`, para que
uma instabilidade do banco não faça o Render reiniciar a API em loop.

## 4. Arquivos (imagens e documentos)

Quando o módulo de arquivos existir, eles vão para o **Supabase Storage**, nunca para dentro do banco:

- buckets privados para documentos (ex.: notas fiscais); o backend gera links temporários para download;
- imagens reduzidas no upload (largura máxima ~1200 px, WebP ou JPEG comprimido), porque 1 GB acaba rápido;
- a chave de acesso do Storage (S3) fica só nas variáveis do Render, nunca no front.

## 5. Front-end

Publique o front na **Vercel** (grátis, feita para Next.js), não no Render: as 750 h/mês do Render são por
conta, e a API sozinha já usa quase todas. Na Vercel, a única variável é `API_URL=https://<seu-app>.onrender.com`.
Depois, confira se `CORS_ALLOWED_ORIGINS` no Render tem a URL real do front.

## O que já está configurado para não cair

- **Memória**: a JVM usa no máximo 70% da RAM do container, com SerialGC e menos threads de compilação.
  Medido localmente: ~240 MB, abaixo dos 512 MB do Render free.
- **Pool de conexões** pequeno (5), compatível com o limite de conexões de bancos gratuitos.
- **Health checks**: `/actuator/health/liveness` (processo vivo, sem banco) e `/actuator/health/readiness`
  (pronto, inclui o banco). O Render reinicia a API se o liveness falhar; o ping externo usa o readiness.
- **Desligamento gracioso**: requisições em andamento terminam antes de um redeploy.
- **Swagger desligado em produção** (`SWAGGER_ENABLED=true` liga).

## Alternativa: VM própria (sempre ligada)

Para uma VM (ex.: Oracle Cloud Always Free, que exige cartão só para verificação), o `docker-compose.yml`
já sobe banco + API com reinício automático:

```bash
docker compose up -d --build
```

Cuidados no Oracle Always Free: instâncias consideradas ociosas por 7 dias (CPU, rede e memória abaixo de 20%)
podem ser recuperadas pela Oracle, e a capacidade ARM costuma faltar nas regiões mais procuradas. Para HTTPS,
coloque um proxy reverso (ex.: Caddy) na frente da API.

## Redis

Não é necessário por enquanto: o sistema não tem cache nem filas. Adicionar Redis só consumiria memória e mais
um serviço para manter. Quando surgir necessidade real (cache de permissões, sessões compartilhadas entre várias
instâncias, filas), a opção gratuita é o Upstash Redis.
