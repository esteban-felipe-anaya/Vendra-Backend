-- ============================================================================
-- Vendra 0010 — Storage buckets + policies.
-- Buckets are public-read (catalog imagery); writes are owner/role scoped.
-- Spring also issues signed upload URLs server-side; these policies cover the
-- client-direct upload path used by web/admin/mobile.
-- ============================================================================

insert into storage.buckets (id, name, public)
values ('products','products', true),
       ('shops','shops', true),
       ('avatars','avatars', true)
on conflict (id) do nothing;

-- public read for all three buckets
create policy "vendra public read products" on storage.objects for select
  using (bucket_id = 'products');
create policy "vendra public read shops" on storage.objects for select
  using (bucket_id = 'shops');
create policy "vendra public read avatars" on storage.objects for select
  using (bucket_id = 'avatars');

-- authenticated users may upload to products/shops (Spring also validates ownership);
-- objects are conventionally keyed <shop_id>/<file> so admin/owner tooling can scope.
create policy "vendra authed write products" on storage.objects for insert to authenticated
  with check (bucket_id = 'products');
create policy "vendra authed write shops" on storage.objects for insert to authenticated
  with check (bucket_id = 'shops');

-- avatars: a user manages files under a folder named by their uid.
create policy "vendra avatar write own" on storage.objects for insert to authenticated
  with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);
create policy "vendra avatar update own" on storage.objects for update to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);
create policy "vendra avatar delete own" on storage.objects for delete to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);
