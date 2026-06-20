-- ============================================================================
-- Vendra 0008 — notifications: persisted + pushed via Realtime to the right actor.
-- ============================================================================

create table public.notifications (
  id          text primary key default vendra_id('ntf'),
  user_id     uuid not null references public.profiles(id) on delete cascade,
  type        text not null
                check (type in ('order_placed','order_update','new_order','dispatch_offer',
                  'delivery_update','payout','review','system')),
  title       text not null,
  body        text,
  -- loose link to the subject row (order/sub_order/delivery id) for deep-linking.
  ref_type    text,
  ref_id      text,
  is_read     boolean not null default false,
  created_at  timestamptz not null default now()
);
create index idx_notifications_user on public.notifications(user_id, created_at desc);
create index idx_notifications_unread on public.notifications(user_id) where is_read = false;
