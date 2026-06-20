-- ============================================================================
-- Vendra 0007 — finance: platform settings, promos, payouts, ledger.
-- ============================================================================

-- single-row platform configuration (commission rate, base courier fee, tax).
create table public.platform_settings (
  id                   text primary key default 'platform',
  commission_rate      numeric(5,4) not null default 0.1000,   -- 10%
  base_delivery_fee    numeric(12,2) not null default 4.99,
  courier_fee_share    numeric(5,4) not null default 0.8000,   -- courier keeps 80% of delivery fee
  tax_rate             numeric(5,4) not null default 0.0000,
  currency             text not null default 'usd',
  updated_at           timestamptz not null default now(),
  constraint platform_settings_singleton check (id = 'platform')
);
insert into public.platform_settings (id) values ('platform') on conflict do nothing;
create trigger trg_platform_settings_updated before update on public.platform_settings
  for each row execute function public.set_updated_at();

create table public.promos (
  id            text primary key default vendra_id('promo'),
  code          text not null unique,
  description   text,
  discount_type text not null check (discount_type in ('percent','fixed')),
  amount        numeric(12,2) not null check (amount >= 0),
  min_subtotal  numeric(12,2) not null default 0,
  max_redemptions integer,
  redeemed_count  integer not null default 0,
  starts_at     timestamptz,
  ends_at       timestamptz,
  is_active     boolean not null default true,
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now()
);
create trigger trg_promos_updated before update on public.promos
  for each row execute function public.set_updated_at();

-- payouts — a settled transfer to a shop or courier connected account.
create table public.payouts (
  id            text primary key default vendra_id('pay'),
  payee_type    text not null check (payee_type in ('shop','courier')),
  shop_id       text references public.shops(id) on delete set null,
  courier_id    text references public.couriers(id) on delete set null,
  sub_order_id  text references public.sub_orders(id) on delete set null,
  delivery_id   text references public.deliveries(id) on delete set null,
  amount        numeric(12,2) not null,
  currency      text not null default 'usd',
  status        text not null default 'pending'
                  check (status in ('pending','paid','failed')),
  stripe_transfer_id text,
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now()
);
create index idx_payouts_shop    on public.payouts(shop_id);
create index idx_payouts_courier on public.payouts(courier_id);
create trigger trg_payouts_updated before update on public.payouts
  for each row execute function public.set_updated_at();

-- ledger — immutable double-entry-ish record of every money movement.
create table public.ledger (
  id            text primary key default vendra_id('led'),
  order_id      text references public.orders(id) on delete set null,
  sub_order_id  text references public.sub_orders(id) on delete set null,
  entry_type    text not null check (entry_type in
                  ('charge','commission','shop_payout','courier_payout','refund')),
  amount        numeric(12,2) not null,        -- signed: + into platform, - out
  currency      text not null default 'usd',
  memo          text,
  created_at    timestamptz not null default now()
);
create index idx_ledger_order on public.ledger(order_id);
