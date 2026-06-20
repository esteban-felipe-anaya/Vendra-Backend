-- ============================================================================
-- Vendra 0004 — cart: one open cart per customer; items may span many shops.
-- ============================================================================

create table public.carts (
  id          text primary key default vendra_id('cart'),
  user_id     uuid not null unique references public.profiles(id) on delete cascade,
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);
create trigger trg_carts_updated before update on public.carts
  for each row execute function public.set_updated_at();

create table public.cart_items (
  id          text primary key default vendra_id('citm'),
  cart_id     text not null references public.carts(id) on delete cascade,
  product_id  text not null references public.products(id) on delete cascade,
  variant_id  text references public.product_variants(id) on delete set null,
  -- denormalized for fast cart render; authoritative price re-checked at checkout.
  shop_id     text not null references public.shops(id) on delete cascade,
  quantity    integer not null check (quantity > 0),
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now(),
  unique (cart_id, product_id, variant_id)
);
create index idx_cart_items_cart on public.cart_items(cart_id);
create trigger trg_cart_items_updated before update on public.cart_items
  for each row execute function public.set_updated_at();

-- wishlist — saved products.
create table public.wishlist_items (
  id          text primary key default vendra_id('wish'),
  user_id     uuid not null references public.profiles(id) on delete cascade,
  product_id  text not null references public.products(id) on delete cascade,
  created_at  timestamptz not null default now(),
  unique (user_id, product_id)
);
create index idx_wishlist_user on public.wishlist_items(user_id);
