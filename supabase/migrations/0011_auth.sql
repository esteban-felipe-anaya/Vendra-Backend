-- ============================================================================
-- Vendra 0011 — Auth bridge.
-- On signup: create a profiles row. The role is taken from the signup metadata
-- (app_metadata.role or user_metadata.role), defaulting to 'customer'.
-- profiles.role is the SOURCE OF TRUTH and is mirrored into auth.users
-- raw_app_meta_data.role so it lands in the JWT (app_metadata.role) that Spring
-- authorizes from. Changing a profile's role re-stamps the token claim.
-- ============================================================================

-- 1) create profile on new auth user
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public, auth as $$
declare
  v_role text;
  v_name text;
begin
  v_role := coalesce(
    nullif(new.raw_app_meta_data->>'role',''),
    nullif(new.raw_user_meta_data->>'role',''),
    'customer'
  );
  if v_role not in ('customer','shop','courier','admin') then
    v_role := 'customer';
  end if;
  v_name := coalesce(new.raw_user_meta_data->>'full_name', new.raw_user_meta_data->>'name');

  insert into public.profiles (id, role, full_name)
  values (new.id, v_role, v_name)
  on conflict (id) do nothing;

  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- 2) mirror profiles.role → auth.users.raw_app_meta_data.role (so JWT carries it)
create or replace function public.sync_role_to_jwt()
returns trigger language plpgsql security definer set search_path = public, auth as $$
begin
  update auth.users
     set raw_app_meta_data =
           coalesce(raw_app_meta_data, '{}'::jsonb) || jsonb_build_object('role', new.role)
   where id = new.id;
  return new;
end;
$$;

drop trigger if exists on_profile_role_sync on public.profiles;
create trigger on_profile_role_sync
  after insert or update of role on public.profiles
  for each row execute function public.sync_role_to_jwt();
