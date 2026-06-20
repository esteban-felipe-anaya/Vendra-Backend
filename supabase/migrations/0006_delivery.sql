-- ============================================================================
-- Vendra 0006 — delivery: deliveries (dispatch + active job) and courier_locations.
-- One delivery per sub_order. First available courier to accept wins (atomic claim).
-- ============================================================================

create table public.deliveries (
  id            text primary key default vendra_id('del'),
  sub_order_id  text not null unique references public.sub_orders(id) on delete cascade,
  order_id      text not null references public.orders(id) on delete cascade,
  courier_id    text references public.couriers(id) on delete set null,
  status        text not null default 'offered'
                  check (status in ('offered','assigned','picked_up','on_the_way',
                    'delivered','cancelled')),
  -- pickup = shop location snapshot; dropoff = customer address snapshot.
  pickup_lat    numeric(9,6),
  pickup_lng    numeric(9,6),
  pickup_label  text,
  dropoff_lat   numeric(9,6),
  dropoff_lng   numeric(9,6),
  dropoff_label text,
  courier_fee   numeric(12,2) not null default 0,
  payout_status text not null default 'pending'
                  check (payout_status in ('pending','paid','failed')),
  stripe_transfer_id text,
  offered_at    timestamptz not null default now(),
  assigned_at   timestamptz,
  picked_up_at  timestamptz,
  delivered_at  timestamptz,
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now()
);
create index idx_deliveries_courier on public.deliveries(courier_id);
create index idx_deliveries_status  on public.deliveries(status);
create trigger trg_deliveries_updated before update on public.deliveries
  for each row execute function public.set_updated_at();

-- courier_locations — periodic pings during an active delivery; powers the live map.
create table public.courier_locations (
  id          text primary key default vendra_id('loc'),
  delivery_id text not null references public.deliveries(id) on delete cascade,
  courier_id  text not null references public.couriers(id) on delete cascade,
  lat         numeric(9,6) not null,
  lng         numeric(9,6) not null,
  heading     numeric(5,2),
  recorded_at timestamptz not null default now()
);
create index idx_courier_locations_delivery on public.courier_locations(delivery_id, recorded_at desc);
