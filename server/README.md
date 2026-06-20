# Vendra — Backend (Spring Boot API)

The REST API every Vendra client calls. Verifies Supabase JWTs (JWKS), owns all business logic and
money math, and talks to Supabase Postgres over JDBC with the service key (bypassing RLS).

- **Stack:** Spring Boot 3.3 · Java 21 · Spring Web · Spring Data JPA (`ddl-auto=validate`) ·
  Spring Security OAuth2 Resource Server · PostgreSQL (Supabase) · springdoc-openapi · Stripe Java.
- **Schema source of truth:** `../supabase/migrations` (NOT Flyway). Hibernate only validates.
- **Contract:** OpenAPI at `/docs` (Swagger UI) and `/v3/api-docs`; exported to `../../docs/openapi.json`.

## Run

```bash
cp ../.env.example ../.env     # fill Supabase + Stripe values, then export them
./mvnw spring-boot:run          # http://localhost:8080  ·  Swagger: /docs
```

The app reads config from environment variables (see `../.env.example`). It will not start without a
reachable Supabase Postgres (because `ddl-auto=validate` checks the schema against the entities).

## Architecture map (packages)

| Package | Responsibility |
|---------|----------------|
| `security` | JWKS resource-server config, JWT→role converter, `AuthUser` principal |
| `account` | resolve principal → profile/shop/courier (row-scoping); `/me` |
| `catalog` | public browse (categories, shops, products, reviews) |
| `cart` | multi-shop customer cart |
| `checkout` | split cart → per-shop sub-orders, server-side money, atomic stock, Stripe PaymentIntent |
| `order` | state machine, customer + shop order views & transitions, `OrderMapper`, `PricingService` |
| `dispatch` | courier job pool, first-accept-wins claim, delivery flow, location pings |
| `payment` | Stripe Connect (Express) onboarding/charge/transfer; payouts + ledger on delivered |
| `shop` | merchant product CRUD, dashboard, Stripe onboarding |
| `review` | verified-buyer reviews, rating recompute |
| `storage` | Supabase Storage signed upload URLs |
| `admin` | platform operator API (approve/suspend, moderation, settings, oversight, dashboard) |
| `webhook` | Stripe webhook (capture → notify shops; failures) |
| `notify` | persisted notifications (the row insert IS the Realtime push) |

## Regenerate the OpenAPI contract

```bash
mvn test -Dtest=OpenApiExportTest      # boots against in-memory H2, writes ../../docs/openapi.json
```

## Tests

```bash
mvn test                                # JUnit 5 (state machine, pricing) + MockMvc
```

## Conventions

- String PKs with readable prefixes (`prd_`, `ord_`, `sub_`, `cou_`, `del_` …) via `IdGenerator`;
  `profiles.id` aligns to the Supabase `auth.users` UUID.
- Money is `BigDecimal` (JSON numbers); image/URL fields are plain `String`; list endpoints return
  plain arrays. **All** money math is server-side; stock decrements atomically at placement.
- Authorization is role-based from the JWT (`@PreAuthorize`) and row-scoped in the service layer.
