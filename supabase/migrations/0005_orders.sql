-- ============================================================================
-- Vendra 0005 — orders: parent order → one sub_order per shop → order_items.
-- All money computed server-side (Spring). Numerics in major units (e.g. 12.50).
-- ============================================================================

create table public.orders (
  id                 text primary key default vendra_id('ord'),
  customer_id        uuid not null references public.profiles(id) on delete restrict,
  address_id         text references public.addresses(id) on delete set null,
  -- snapshot of the ship-to address (addresses may change/delete later).
  ship_to            jsonb,
  currency           text not null default 'usd',
  items_subtotal     numeric(12,2) not null default 0,   -- sum of all shop subtotals
  delivery_fee       numeric(12,2) not null default 0,   -- single courier fee for the order
  tax                numeric(12,2) not null default 0,
  discount           numeric(12,2) not null default 0,
  total              numeric(12,2) not null default 0,    -- charged to the customer
  platform_commission numeric(12,2) not null default 0,   -- platform's cut across sub-orders
  promo_id           text,
  payment_status     text not null default 'requires_payment'
                       check (payment_status in
                         ('requires_payment','authorized','captured','refunded','failed')),
  stripe_payment_intent_id text,
  status             text not null default 'placed'
                       check (status in ('placed','processing','completed','cancelled')),
  placed_at          timestamptz not null default now(),
  created_at         timestamptz not null default now(),
  updated_at         timestamptz not null default now()
);
create index idx_orders_customer on public.orders(customer_id);
create index idx_orders_status   on public.orders(status);
create trigger trg_orders_updated before update on public.orders
  for each row execute function public.set_updated_at();

-- sub_order — the per-shop fulfillment unit; the state machine lives here.
create table public.sub_orders (
  id                 text primary key default vendra_id('sub'),
  order_id           text not null references public.orders(id) on delete cascade,
  shop_id            text not null references public.shops(id) on delete restrict,
  status             text not null default 'placed'
                       check (status in ('placed','accepted','preparing','ready',
                         'courier_assigned','picked_up','on_the_way','delivered',
                         'cancelled','rejected')),
  items_subtotal     numeric(12,2) not null default 0,
  commission_rate    numeric(5,4) not null default 0.1000,   -- snapshot at checkout
  commission_amount  numeric(12,2) not null default 0,
  shop_payout        numeric(12,2) not null default 0,        -- subtotal - commission
  payout_status      text not null default 'pending'
                       check (payout_status in ('pending','paid','failed')),
  stripe_transfer_id text,
  accepted_at        timestamptz,
  ready_at           timestamptz,
  delivered_at       timestamptz,
  created_at         timestamptz not null default now(),
  updated_at         timestamptz not null default now()
);
create index idx_sub_orders_order  on public.sub_orders(order_id);
create index idx_sub_orders_shop   on public.sub_orders(shop_id);
create index idx_sub_orders_status on public.sub_orders(status);
create trigger trg_sub_orders_updated before update on public.sub_orders
  for each row execute function public.set_updated_at();

-- order_items — line items, snapshotting name/price at purchase time.
create table public.order_items (
  id            text primary key default vendra_id('itm'),
  sub_order_id  text not null references public.sub_orders(id) on delete cascade,
  product_id    text references public.products(id) on delete set null,
  variant_id    text references public.product_variants(id) on delete set null,
  name_snapshot text not null,
  image_snapshot text,
  unit_price    numeric(12,2) not null,
  quantity      integer not null check (quantity > 0),
  line_total    numeric(12,2) not null,
  created_at    timestamptz not null default now()
);
create index idx_order_items_sub on public.order_items(sub_order_id);
