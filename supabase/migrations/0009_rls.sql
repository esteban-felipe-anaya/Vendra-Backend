-- ============================================================================
-- Vendra 0009 — Row Level Security.
-- Spring Boot uses the SERVICE ROLE and bypasses RLS for all writes/business logic.
-- These policies constrain the CLIENT-DIRECT surfaces (Realtime subscriptions and
-- any direct PostgREST reads) so a user only ever sees their own data, while public
-- catalog stays browsable.
-- ============================================================================

-- ---- helper functions (SECURITY DEFINER → read profiles without recursing RLS) ----
create or replace function public.jwt_role()
returns text language sql stable security definer set search_path = public as $$
  select role from public.profiles where id = auth.uid();
$$;

create or replace function public.is_admin()
returns boolean language sql stable security definer set search_path = public as $$
  select coalesce((select role = 'admin' from public.profiles where id = auth.uid()), false);
$$;

create or replace function public.owns_shop(p_shop_id text)
returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.shops s where s.id = p_shop_id and s.owner_id = auth.uid());
$$;

create or replace function public.my_courier_id()
returns text language sql stable security definer set search_path = public as $$
  select id from public.couriers where profile_id = auth.uid();
$$;

-- ---- enable RLS everywhere ----
alter table public.profiles          enable row level security;
alter table public.shops             enable row level security;
alter table public.couriers          enable row level security;
alter table public.addresses         enable row level security;
alter table public.categories        enable row level security;
alter table public.products          enable row level security;
alter table public.product_images    enable row level security;
alter table public.product_variants  enable row level security;
alter table public.reviews           enable row level security;
alter table public.carts             enable row level security;
alter table public.cart_items        enable row level security;
alter table public.wishlist_items    enable row level security;
alter table public.orders            enable row level security;
alter table public.sub_orders        enable row level security;
alter table public.order_items       enable row level security;
alter table public.deliveries        enable row level security;
alter table public.courier_locations enable row level security;
alter table public.platform_settings enable row level security;
alter table public.promos            enable row level security;
alter table public.payouts           enable row level security;
alter table public.ledger            enable row level security;
alter table public.notifications     enable row level security;

-- ---- profiles ----
create policy profiles_self_read   on public.profiles for select using (id = auth.uid() or is_admin());
create policy profiles_self_update on public.profiles for update using (id = auth.uid());

-- ---- shops: public sees approved; owner sees own; admin sees all ----
create policy shops_public_read on public.shops for select
  using (status = 'approved' or owner_id = auth.uid() or is_admin());

-- ---- couriers: self + admin ----
create policy couriers_self_read on public.couriers for select
  using (profile_id = auth.uid() or is_admin());

-- ---- catalog: public read ----
create policy categories_public_read on public.categories for select using (is_active or is_admin());
create policy products_public_read on public.products for select
  using (is_active or owns_shop(shop_id) or is_admin());
create policy product_images_public_read on public.product_images for select using (true);
create policy product_variants_public_read on public.product_variants for select using (true);
create policy reviews_public_read on public.reviews for select using (true);
create policy promos_public_read on public.promos for select using (is_active or is_admin());
create policy platform_settings_read on public.platform_settings for select using (true);

-- ---- addresses / cart / wishlist: strictly the owner ----
create policy addresses_owner on public.addresses for select using (user_id = auth.uid());
create policy carts_owner on public.carts for select using (user_id = auth.uid());
create policy cart_items_owner on public.cart_items for select
  using (exists (select 1 from public.carts c where c.id = cart_id and c.user_id = auth.uid()));
create policy wishlist_owner on public.wishlist_items for select using (user_id = auth.uid());

-- ---- orders: customer owns; admin all ----
create policy orders_read on public.orders for select
  using (customer_id = auth.uid() or is_admin());

-- ---- sub_orders: customer (via parent), shop owner, admin ----
create policy sub_orders_read on public.sub_orders for select using (
  is_admin()
  or owns_shop(shop_id)
  or exists (select 1 from public.orders o where o.id = order_id and o.customer_id = auth.uid())
);

-- ---- order_items: follows sub_order visibility ----
create policy order_items_read on public.order_items for select using (
  is_admin()
  or exists (
    select 1 from public.sub_orders so
    join public.orders o on o.id = so.order_id
    where so.id = sub_order_id
      and (owns_shop(so.shop_id) or o.customer_id = auth.uid())
  )
);

-- ---- deliveries: customer (via order), assigned courier, AVAILABLE couriers see offers, admin ----
create policy deliveries_read on public.deliveries for select using (
  is_admin()
  or courier_id = my_courier_id()
  or (status = 'offered' and my_courier_id() is not null
        and exists (select 1 from public.couriers c
                    where c.id = my_courier_id() and c.availability = 'available'))
  or exists (select 1 from public.orders o where o.id = order_id and o.customer_id = auth.uid())
);

-- ---- courier_locations: the order's customer, the courier, admin (powers live map) ----
create policy courier_locations_read on public.courier_locations for select using (
  is_admin()
  or courier_id = my_courier_id()
  or exists (
    select 1 from public.deliveries d
    join public.orders o on o.id = d.order_id
    where d.id = delivery_id and o.customer_id = auth.uid()
  )
);

-- ---- payouts: shop owner / courier owner / admin ----
create policy payouts_read on public.payouts for select using (
  is_admin() or owns_shop(shop_id) or courier_id = my_courier_id()
);

-- ---- ledger: admin only ----
create policy ledger_admin_read on public.ledger for select using (is_admin());

-- ---- notifications: the recipient ----
create policy notifications_owner_read on public.notifications for select using (user_id = auth.uid());
-- allow a client to mark its own notifications read directly (the one client write we permit).
create policy notifications_owner_update on public.notifications for update
  using (user_id = auth.uid()) with check (user_id = auth.uid());
