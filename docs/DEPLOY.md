# Deploy gratuito

Opção recomendada, sem custo e sem cartão de crédito: **Render** (API em Docker) + **Neon** (PostgreSQL) +
**cron-job.org** (ping que impede a API de dormir).

| Peça | Serviço | Limite do plano gratuito que importa aqui |
|------|---------|-------------------------------------------|
| API | Render, web service free | Dorme após 15 min sem requisições; 750 h/mês (dá para 1 serviço ligado 24 h) |
| Banco | Neon, plano free | 0,5 GB por projeto; 100 CU-horas/mês; hiberna quando ocioso e acorda sozinho |
| Ping | cron-job.org | Chamada a cada 10 min para a API não dormir |

> O Postgres gratuito do próprio Render é apagado 30 dias após a criação, por isso o banco fica no Neon.

## 1. Banco no Neon

1. Crie uma conta em https://neon.com e um projeto (região mais próxima do Render, ex.: AWS US West/Oregon).
2. Em **Connect**, copie host, banco, usuário e senha. Monte a URL JDBC:
   `jdbc:postgresql://<host>/<banco>?sslmode=require`

As migrations do Flyway criam as tabelas na primeira subida da API.

## 2. API no Render

1. Crie uma conta em https://render.com e conecte o GitHub.
2. **New → Blueprint** e escolha este repositório. O Render lê o `render.yaml`.
3. Preencha as variáveis pedidas:
   - `DB_URL`: a URL JDBC do passo anterior
   - `DB_USERNAME` / `DB_PASSWORD`: do Neon
   - `ADMIN_EMAIL` / `ADMIN_SENHA`: o primeiro Superadmin (só é usado enquanto não houver usuário)
   - `CORS_ALLOWED_ORIGINS`: a URL do front publicado (ex.: `https://meu-front.vercel.app`); várias, separadas por vírgula
   - `JWT_SECRET` é gerado pelo Render automaticamente
4. Aguarde o deploy e teste: `https://<seu-app>.onrender.com/actuator/health/liveness` deve responder `{"status":"UP"}`.

Cada push na `main` gera um deploy novo (`autoDeploy: true`).

## 3. Não deixar a API dormir

1. Crie uma conta em https://cron-job.org.
2. Crie um job `GET https://<seu-app>.onrender.com/actuator/health/liveness` a cada **10 minutos**.

O ping usa o `liveness` de propósito: ele **não consulta o banco**, então o Neon continua hibernando quando
ninguém usa o sistema e a cota de 100 CU-horas não é consumida à toa. Se o ping consultasse o banco, o Neon
ficaria ligado 24 h e estouraria a cota no meio do mês.

Mesmo assim, a primeira requisição depois de um tempo sem uso pode levar alguns segundos a mais enquanto o
Neon acorda (o pool de conexões já espera até 20 s).

## O que já está configurado para não cair

- **Memória**: a JVM usa no máximo 70% da RAM do container, com SerialGC e menos threads de compilação.
  Medido localmente: ~240 MB, abaixo dos 512 MB do Render free.
- **Pool de conexões** pequeno (5), compatível com o limite de conexões de bancos gratuitos.
- **Health checks**: `/actuator/health/liveness` (processo vivo) e `/actuator/health/readiness` (pronto,
  inclui o banco). O Render reinicia a API se o liveness falhar.
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
