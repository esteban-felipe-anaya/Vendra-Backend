-- ============================================================================
-- Vendra 0012 — Realtime publication.
-- Spring writes status/location/notification rows; Supabase Realtime broadcasts
-- the changes to subscribed, RLS-authorized clients. REPLICA IDENTITY FULL so
-- subscribers receive old+new column values for UPDATE events.
-- ============================================================================

alter table public.sub_orders        replica identity full;
alter table public.deliveries        replica identity full;
alter table public.courier_locations replica identity full;
alter table public.notifications     replica identity full;
alter table public.orders            replica identity full;

-- Supabase ships a `supabase_realtime` publication by default; add our tables to it.
do $$
begin
  if exists (select 1 from pg_publication where pubname = 'supabase_realtime') then
    alter publication supabase_realtime add table
      public.sub_orders, public.deliveries, public.courier_locations,
      public.notifications, public.orders;
  else
    create publication supabase_realtime for table
      public.sub_orders, public.deliveries, public.courier_locations,
      public.notifications, public.orders;
  end if;
exception when duplicate_object then
  null;  -- tables already in publication
end $$;
