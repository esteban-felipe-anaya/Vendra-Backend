# Vendra — Supabase (schema source of truth)

This directory is the **single source of truth** for the Vendra database. Spring Boot runs
`ddl-auto=validate` against it — it never alters the schema. **No Flyway.**

## Layout

```
supabase/
  config.toml                 CLI / project config (asymmetric JWT, storage buckets, realtime)
  migrations/                 ordered DDL — the schema
    0001_foundation.sql       extensions, vendra_id(), updated_at trigger
    0002_identity.sql         profiles, shops, couriers, addresses
    0003_catalog.sql          categories, products, variants, images, reviews
    0004_cart.sql             carts, cart_items, wishlist
    0005_orders.sql           orders, sub_orders, order_items
    0006_delivery.sql         deliveries, courier_locations
    0007_finance.sql          platform_settings, promos, payouts, ledger
    0008_notifications.sql    notifications
    0009_rls.sql              Row Level Security + helper functions
    0010_storage.sql          buckets (products/shops/avatars) + policies
    0011_auth.sql             signup → profile trigger + role → JWT mirror
    0012_realtime.sql         realtime publication
  seed.sql                    idempotent: shops, ~40 products, couriers, 1 in-flight order, logins
```

## Apply

```bash
supabase link --project-ref <your-ref>
supabase db push                       # runs migrations/*.sql in order
supabase db execute -f seed.sql        # idempotent seed

# local stack instead:
supabase start && supabase db reset     # reset replays migrations + seed.sql automatically
```

## Auth note

`0011_auth.sql` creates a `profiles` row on signup and mirrors `profiles.role` into
`auth.users.raw_app_meta_data.role`, so the JWT carries `app_metadata.role` for Spring to authorize
from. To create a `shop`/`courier`/`admin`, sign up with `app_metadata.role` set (admin API) or
update the `profiles.role` afterwards — the trigger re-stamps the token claim on next refresh.

## Test logins (from `seed.sql`)

| Role | Email | Password |
|------|-------|----------|
| admin | admin@vendra.app | `Admin123!` |
| customer | ava@vendra.app | `Customer123!` |
| customer | noah@vendra.app | `Customer123!` |
| shop | lumen@vendra.app / verde@vendra.app / atlas@vendra.app | `Shop123!` |
| courier | theo@vendra.app / mia@vendra.app | `Courier123!` |

`ava@vendra.app` owns the seeded in-flight order; courier `theo@vendra.app` is en route on it.
