# Vendra — Backend

Spring Boot API (`server/`) + Supabase database (`supabase/`, the schema source of truth).

Clients sign in **through this API** (`POST /api/v1/auth/login` · `/signup` · `/refresh` · `/logout`),
which proxies Supabase Auth server-side — clients never call Supabase Auth directly. They then call
the API with `Authorization: Bearer <jwt>`; Spring verifies the JWT via Supabase JWKS, authorizes by
role, and talks to Postgres over JDBC with the service key. Schema lives only in
`supabase/migrations` — Hibernate runs `ddl-auto=validate` (no Flyway).

```
backend/
  server/     Spring Boot (Java 21, Maven)
  supabase/   migrations + seed.sql
  .env        your secrets (gitignored) — copy from .env.example
```

## Prerequisites

- Java 21, Maven (or the bundled `./mvnw`)
- [Supabase CLI](https://supabase.com/docs/guides/cli) — `npm i -g supabase`
- A Supabase project (free tier) · a Stripe **test** account (optional, needed for checkout)

## 1. Database

Create a Supabase project (save the DB password and Project Ref), then:

```bash
cd backend/supabase
supabase login
supabase link --project-ref <your-ref>
supabase db push                 # applies migrations → all tables, RLS, storage, auth trigger
```

**Seed** (demo data + test logins, idempotent): open the project's **SQL Editor**, paste all of
`seed.sql`, and Run. _(CLI alternative: `supabase db query --linked < seed.sql`.)_

Verify: `select count(*) from products;` → **40**.

## 2. Configure `backend/.env`

```bash
cd backend && cp .env.example .env
```

It's auto-loaded by the app. Fill it from the Supabase dashboard:

| Variable | Where to find it |
|----------|------------------|
| `SUPABASE_URL`, `SUPABASE_REF` | Settings → API (Project URL) |
| `SUPABASE_ANON_KEY` | Settings → API → anon/public key |
| `SUPABASE_SERVICE_ROLE_KEY` | Settings → API → service_role key (secret) |
| `SUPABASE_JWKS_URI` | `https://<ref>.supabase.co/auth/v1/.well-known/jwks.json` |
| `SUPABASE_JWT_ISSUER` | `https://<ref>.supabase.co/auth/v1` |
| `SUPABASE_JWT_AUDIENCE` | `authenticated` |
| `SPRING_DATASOURCE_URL` | Settings → Database → **Connection pooling** (Transaction, port 6543): `jdbc:postgresql://aws-0-<region>.pooler.supabase.com:6543/postgres?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | `postgres.<ref>` |
| `SPRING_DATASOURCE_PASSWORD` | your DB password |
| `SUPABASE_STORAGE_URL` | `https://<ref>.supabase.co/storage/v1` |
| `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET` | Stripe (test) — optional, see below |

> Use the **pooler** host + username `postgres.<ref>` (not `postgres`). New Supabase projects use
> asymmetric ES256 JWTs by default — exactly what the JWKS config expects.

## 3. Run

```bash
cd backend/server
./mvnw spring-boot:run            # Windows: mvnw.cmd spring-boot:run
```

- Swagger UI: <http://localhost:8080/docs>
- Public check: `curl http://localhost:8080/api/v1/products` → seeded products

Build/test: `./mvnw -DskipTests package` · `./mvnw test`. Regenerate the OpenAPI contract:
`./mvnw test -Dtest=OpenApiExportTest` (writes `../../docs/openapi.json`).

## 4. Stripe (optional, for checkout/payouts)

Set `STRIPE_SECRET_KEY` (`sk_test_…`), enable **Connect**, and forward webhooks:

```bash
stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe   # prints whsec_… → STRIPE_WEBHOOK_SECRET
```

Without Stripe, `POST /api/v1/checkout` returns 503; everything else works. Test card `4242 4242 4242 4242`.

## Test logins (from `seed.sql`)

| Role | Email | Password |
|------|-------|----------|
| admin | admin@vendra.app | `Admin123!` |
| customer | ava@vendra.app | `Customer123!` (has the in-flight demo order) |
| shop | lumen@vendra.app / verde@vendra.app / atlas@vendra.app | `Shop123!` |
| courier | theo@vendra.app / mia@vendra.app | `Courier123!` |

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Startup `Schema-validation: missing table` | Run `supabase db push` before starting the API. |
| Seed `function gen_salt does not exist` | pgcrypto lives in the `extensions` schema; the current `seed.sql` already handles it (`search_path = auth, public, extensions`). |
| `supabase db query` → `127.0.0.1:54322 refused` | `db query` defaults to local; use the SQL Editor or `--linked` / `--db-url`. |
| `db push` → `storage.buckets ... unconvertible type 'string'` | Pull the latest `config.toml` (buckets come from the migration, not config). |
| 401 on authorized calls | Check `SUPABASE_JWKS_URI`; token must be a current Supabase access token. |
| 403 with valid token | Role mismatch — role comes from `app_metadata.role`; change `profiles.role` and re-login. |
| DB connect timeout | Use the pooler host (not `db.<ref>.supabase.co`); keep `?sslmode=require`. |
| Startup `FATAL: (ENOTFOUND) tenant/user postgres.<ref> not found` | Wrong pooler host. Use the **exact** host from dashboard → **Connect**, or from `supabase/.temp/pooler-url` after `supabase link`. Note the instance prefix is your project's real one (e.g. `aws-1-...`), not necessarily `aws-0`. Username stays `postgres.<ref>`. |
| `password authentication failed` | Wrong `SPRING_DATASOURCE_PASSWORD` (the database password, not the service-role key) — reset it under Settings → Database if unsure. |

More detail: [`server/README.md`](server/README.md) (API/package map) · [`supabase/README.md`](supabase/README.md) (schema).
