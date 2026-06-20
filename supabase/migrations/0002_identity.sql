-- ============================================================================
-- Vendra 0002 — identity: profiles, shops, couriers, addresses
-- ============================================================================

-- profiles.id == auth.users.id (Supabase-owned UUID). Source of truth for role.
create table public.profiles (
  id            uuid primary key references auth.users(id) on delete cascade,
  role          text not null default 'customer'
                  check (role in ('customer','shop','courier','admin')),
  full_name     text,
  phone         text,
  avatar_url    text,
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now()
);
comment on table public.profiles is 'One row per auth user; role is the source of truth, mirrored into JWT app_metadata.role.';

create trigger trg_profiles_updated before update on public.profiles
  for each row execute function public.set_updated_at();

-- shops — a merchant tenant, owned by a profile.
create table public.shops (
  id                 text primary key default vendra_id('shop'),
  owner_id           uuid not null references public.profiles(id) on delete cascade,
  name               text not null,
  slug               text not null unique,
  description        text,
  logo_url           text,
  banner_url         text,
  status             text not null default 'pending'
                       check (status in ('pending','approved','suspended')),
  stripe_account_id  text,                          -- Stripe Connect (Express) account
  payouts_enabled    boolean not null default false,
  rating_avg         numeric(3,2) not null default 0,
  rating_count       integer not null default 0,
  created_at         timestamptz not null default now(),
  updated_at         timestamptz not null default now()
);
create index idx_shops_owner  on public.shops(owner_id);
create index idx_shops_status on public.shops(status);
create trigger trg_shops_updated before update on public.shops
  for each row execute function public.set_updated_at();

-- couriers — a delivery agent, owned by a profile.
create table public.couriers (
  id                 text primary key default vendra_id('cou'),
  profile_id         uuid not null unique references public.profiles(id) on delete cascade,
  vehicle_type       text not null default 'bike'
                       check (vehicle_type in ('bike','scooter','car','foot')),
  availability        text not null default 'offline'
                       check (availability in ('offline','available','busy')),
  current_lat        numeric(9,6),
  current_lng        numeric(9,6),
  stripe_account_id  text,
  payouts_enabled    boolean not null default false,
  rating_avg         numeric(3,2) not null default 0,
  rating_count       integer not null default 0,
  created_at         timestamptz not null default now(),
  updated_at         timestamptz not null default now()
);
create index idx_couriers_avail on public.couriers(availability);
create trigger trg_couriers_updated before update on public.couriers
  for each row execute function public.set_updated_at();

-- addresses — customer shipping addresses.
create table public.addresses (
  id           text primary key default vendra_id('adr'),
  user_id      uuid not null references public.profiles(id) on delete cascade,
  label        text,
  recipient    text not null,
  phone        text,
  line1        text not null,
  line2        text,
  city         text not null,
  region       text,
  postal_code  text,
  country      text not null default 'US',
  lat          numeric(9,6),
  lng          numeric(9,6),
  is_default   boolean not null default false,
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now()
);
create index idx_addresses_user on public.addresses(user_id);
create trigger trg_addresses_updated before update on public.addresses
  for each row execute function public.set_updated_at();
