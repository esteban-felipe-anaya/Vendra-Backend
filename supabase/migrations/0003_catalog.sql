-- ============================================================================
-- Vendra 0003 — catalog: categories, products, product_variants, product_images, reviews
-- ============================================================================

create table public.categories (
  id          text primary key default vendra_id('cat'),
  name        text not null,
  slug        text not null unique,
  icon        text,                       -- lucide icon name or emoji
  image_url   text,
  sort_order  integer not null default 0,
  is_active   boolean not null default true,
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);
create trigger trg_categories_updated before update on public.categories
  for each row execute function public.set_updated_at();

create table public.products (
  id            text primary key default vendra_id('prd'),
  shop_id       text not null references public.shops(id) on delete cascade,
  category_id   text references public.categories(id) on delete set null,
  name          text not null,
  slug          text not null,
  description   text,
  -- base price in major units; per-variant price overrides this when set.
  price         numeric(12,2) not null check (price >= 0),
  currency      text not null default 'usd',
  -- denormalized stock = sum of variant stock when variants exist, else own stock.
  stock         integer not null default 0 check (stock >= 0),
  image_url     text,                       -- primary image
  is_active     boolean not null default true,
  rating_avg    numeric(3,2) not null default 0,
  rating_count  integer not null default 0,
  sold_count    integer not null default 0,
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now(),
  unique (shop_id, slug)
);
create index idx_products_shop     on public.products(shop_id);
create index idx_products_category on public.products(category_id);
create index idx_products_active   on public.products(is_active);
create trigger trg_products_updated before update on public.products
  for each row execute function public.set_updated_at();

-- additional gallery images for a product (primary lives on products.image_url).
create table public.product_images (
  id          text primary key default vendra_id('img'),
  product_id  text not null references public.products(id) on delete cascade,
  url         text not null,
  sort_order  integer not null default 0,
  created_at  timestamptz not null default now()
);
create index idx_product_images_product on public.product_images(product_id);

-- variants — e.g. size/color. price NULL → inherit product.price.
create table public.product_variants (
  id          text primary key default vendra_id('var'),
  product_id  text not null references public.products(id) on delete cascade,
  name        text not null,              -- e.g. "Large / Indigo"
  sku         text,
  price       numeric(12,2) check (price >= 0),
  stock       integer not null default 0 check (stock >= 0),
  is_active   boolean not null default true,
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);
create index idx_variants_product on public.product_variants(product_id);
create trigger trg_variants_updated before update on public.product_variants
  for each row execute function public.set_updated_at();

create table public.reviews (
  id          text primary key default vendra_id('rev'),
  product_id  text not null references public.products(id) on delete cascade,
  user_id     uuid not null references public.profiles(id) on delete cascade,
  -- links the review to a delivered purchase (verified buyer).
  order_id    text,
  rating      integer not null check (rating between 1 and 5),
  title       text,
  body        text,
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now(),
  unique (product_id, user_id)
);
create index idx_reviews_product on public.reviews(product_id);
create index idx_reviews_user    on public.reviews(user_id);
create trigger trg_reviews_updated before update on public.reviews
  for each row execute function public.set_updated_at();
