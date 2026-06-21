-- ============================================================================
-- Vendra — seed (idempotent). Real data + real image URLs (Unsplash CDN).
-- Includes a known admin + one test login per role, and ONE in-flight order so
-- the live courier map demonstrates immediately.
--
-- Apply:  supabase db execute -f seed.sql   (or psql -f seed.sql)
-- Safe to run repeatedly (ON CONFLICT guards).
--
-- TEST LOGINS (documented):
--   admin     admin@vendra.app       Admin123!
--   customer  ava@vendra.app         Customer123!     (has the in-flight order)
--   customer  noah@vendra.app        Customer123!
--   shop      lumen@vendra.app       Shop123!         (Lumen Goods)
--   shop      verde@vendra.app       Shop123!         (Verde Market)
--   shop      atlas@vendra.app       Shop123!         (Atlas Outfitters)
--   shop      brewhouse@vendra.app   Shop123!         (Brewhouse)
--   courier   theo@vendra.app        Courier123!      (assigned to the in-flight order)
--   courier   mia@vendra.app         Courier123!
-- ============================================================================

-- ---------------------------------------------------------------------------
-- helper: create an auth user + identity with a bcrypt password (idempotent).
-- ---------------------------------------------------------------------------
create or replace function public.seed_user(
  p_id uuid, p_email text, p_password text, p_role text, p_name text
) returns uuid language plpgsql security definer
  set search_path = auth, public, extensions as $$  -- extensions: pgcrypto (crypt/gen_salt)
begin
  insert into auth.users (
    instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
    raw_app_meta_data, raw_user_meta_data, created_at, updated_at,
    confirmation_token, recovery_token, email_change_token_new, email_change
  ) values (
    '00000000-0000-0000-0000-000000000000', p_id, 'authenticated', 'authenticated',
    p_email, crypt(p_password, gen_salt('bf')), now(),
    jsonb_build_object('provider','email','providers',array['email'],'role',p_role),
    jsonb_build_object('full_name',p_name,'role',p_role), now(), now(), '', '', '', ''
  ) on conflict (id) do nothing;

  insert into auth.identities (
    id, user_id, identity_data, provider, provider_id, last_sign_in_at, created_at, updated_at
  ) values (
    gen_random_uuid(), p_id,
    jsonb_build_object('sub', p_id::text, 'email', p_email),
    'email', p_id::text, now(), now(), now()
  ) on conflict (provider, provider_id) do nothing;

  return p_id;
end;
$$;

-- ---------------------------------------------------------------------------
-- users (the on_auth_user_created trigger auto-creates matching profiles rows)
-- ---------------------------------------------------------------------------
select public.seed_user('a0000000-0000-0000-0000-000000000001','admin@vendra.app','Admin123!','admin','Avery Admin');
select public.seed_user('c0000000-0000-0000-0000-000000000001','ava@vendra.app','Customer123!','customer','Ava Nguyen');
select public.seed_user('c0000000-0000-0000-0000-000000000002','noah@vendra.app','Customer123!','customer','Noah Patel');
select public.seed_user('50000000-0000-0000-0000-000000000001','lumen@vendra.app','Shop123!','shop','Lena Cruz');
select public.seed_user('50000000-0000-0000-0000-000000000002','verde@vendra.app','Shop123!','shop','Marco Vidal');
select public.seed_user('50000000-0000-0000-0000-000000000003','atlas@vendra.app','Shop123!','shop','Ines Roy');
select public.seed_user('50000000-0000-0000-0000-000000000004','brewhouse@vendra.app','Shop123!','shop','Sam Park');
select public.seed_user('c0000000-0000-0000-0000-0000000000a1','theo@vendra.app','Courier123!','courier','Theo Bauer');
select public.seed_user('c0000000-0000-0000-0000-0000000000a2','mia@vendra.app','Courier123!','courier','Mia Sato');

-- enrich profiles (avatars from a real source)
update public.profiles p set
  avatar_url = v.avatar, full_name = coalesce(p.full_name, v.name), phone = v.phone
from (values
  ('a0000000-0000-0000-0000-000000000001','Avery Admin','+1-202-555-0100','https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80'),
  ('c0000000-0000-0000-0000-000000000001','Ava Nguyen','+1-202-555-0111','https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80'),
  ('c0000000-0000-0000-0000-000000000002','Noah Patel','+1-202-555-0112','https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80'),
  ('c0000000-0000-0000-0000-0000000000a1','Theo Bauer','+1-202-555-0150','https://images.unsplash.com/photo-1633332755192-727a05c4013d?auto=format&fit=crop&w=200&q=80'),
  ('c0000000-0000-0000-0000-0000000000a2','Mia Sato','+1-202-555-0151','https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=200&q=80')
) as v(id,name,phone,avatar)
where p.id = v.id::uuid;

-- ---------------------------------------------------------------------------
-- shops (3 approved + 1 pending for admin moderation demo)
-- ---------------------------------------------------------------------------
insert into public.shops (id, owner_id, name, slug, description, logo_url, banner_url, status, stripe_account_id, payouts_enabled, rating_avg, rating_count) values
 ('shop_lumen','50000000-0000-0000-0000-000000000001','Lumen Goods','lumen-goods','Modern home & lighting, thoughtfully made.','https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=200&q=80','https://images.unsplash.com/photo-1556228453-efd6c1ff04f6?auto=format&fit=crop&w=1200&q=80','approved','acct_test_lumen', true, 4.7, 128),
 ('shop_verde','50000000-0000-0000-0000-000000000002','Verde Market','verde-market','Fresh groceries & pantry staples, locally sourced.','https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=200&q=80','https://images.unsplash.com/photo-1488459716781-31db52582fe9?auto=format&fit=crop&w=1200&q=80','approved','acct_test_verde', true, 4.5, 86),
 ('shop_atlas','50000000-0000-0000-0000-000000000003','Atlas Outfitters','atlas-outfitters','Apparel & gear for everyday adventures.','https://images.unsplash.com/photo-1441986300917-64674bd600d8?auto=format&fit=crop&w=200&q=80','https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=1200&q=80','approved','acct_test_atlas', true, 4.6, 64),
 ('shop_brew','50000000-0000-0000-0000-000000000004','Brewhouse','brewhouse','Specialty coffee & brewing equipment.','https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=200&q=80','https://images.unsplash.com/photo-1442512595331-e89e73853f31?auto=format&fit=crop&w=1200&q=80','pending', null, false, 0, 0)
on conflict (id) do nothing;

-- ---------------------------------------------------------------------------
-- couriers
-- ---------------------------------------------------------------------------
insert into public.couriers (id, profile_id, vehicle_type, availability, current_lat, current_lng, stripe_account_id, payouts_enabled, rating_avg, rating_count) values
 ('cou_theo','c0000000-0000-0000-0000-0000000000a1','scooter','busy', 40.7282, -73.9942,'acct_test_theo', true, 4.8, 210),
 ('cou_mia','c0000000-0000-0000-0000-0000000000a2','bike','available', 40.7411, -74.0018,'acct_test_mia', true, 4.9, 154)
on conflict (id) do nothing;

-- ---------------------------------------------------------------------------
-- categories
-- ---------------------------------------------------------------------------
insert into public.categories (id, name, slug, icon, image_url, sort_order) values
 ('cat_home','Home & Living','home-living','sofa','https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=600&q=80',1),
 ('cat_grocery','Grocery','grocery','shopping-basket','https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=600&q=80',2),
 ('cat_apparel','Apparel','apparel','shirt','https://images.unsplash.com/photo-1489987707025-afc232f7ea0f?auto=format&fit=crop&w=600&q=80',3),
 ('cat_coffee','Coffee & Tea','coffee-tea','coffee','https://images.unsplash.com/photo-1447933601403-0c6688de566e?auto=format&fit=crop&w=600&q=80',4),
 ('cat_tech','Tech & Audio','tech-audio','headphones','https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80',5),
 ('cat_beauty','Beauty','beauty','sparkles','https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?auto=format&fit=crop&w=600&q=80',6)
on conflict (id) do nothing;

-- ---------------------------------------------------------------------------
-- products (~40) — real Unsplash imagery
-- ---------------------------------------------------------------------------
insert into public.products (id, shop_id, category_id, name, slug, description, price, stock, image_url, rating_avg, rating_count, sold_count) values
 ('prd_l01','shop_lumen','cat_home','Arc Floor Lamp','arc-floor-lamp','Brushed-brass arc lamp with a linen shade.',189.00,24,'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=800&q=80',4.7,42,120),
 ('prd_l02','shop_lumen','cat_home','Ceramic Table Lamp','ceramic-table-lamp','Hand-glazed ceramic base, warm dimmable bulb.',96.00,40,'https://images.unsplash.com/photo-1543198126-a8ad8e47fb22?auto=format&fit=crop&w=800&q=80',4.6,30,88),
 ('prd_l03','shop_lumen','cat_home','Linen Throw Pillow','linen-throw-pillow','Stonewashed linen cover with feather insert.',38.00,120,'https://images.unsplash.com/photo-1584100936595-c0654b55a2e6?auto=format&fit=crop&w=800&q=80',4.5,51,260),
 ('prd_l04','shop_lumen','cat_home','Oak Side Table','oak-side-table','Solid white-oak side table, tapered legs.',149.00,18,'https://images.unsplash.com/photo-1532372320572-cda25653a26d?auto=format&fit=crop&w=800&q=80',4.8,22,40),
 ('prd_l05','shop_lumen','cat_home','Wool Area Rug','wool-area-rug','Hand-tufted wool rug, 5x7, muted tones.',299.00,12,'https://images.unsplash.com/photo-1600166898405-da9535204843?auto=format&fit=crop&w=800&q=80',4.7,19,30),
 ('prd_l06','shop_lumen','cat_home','Glass Vase Set','glass-vase-set','Set of three recycled-glass bud vases.',45.00,60,'https://images.unsplash.com/photo-1578500494198-246f612d3b3d?auto=format&fit=crop&w=800&q=80',4.4,27,150),
 ('prd_l07','shop_lumen','cat_home','Scented Soy Candle','scented-soy-candle','Cedar & amber, 60-hour burn.',28.00,200,'https://images.unsplash.com/photo-1602874801006-94d44a3ddbf1?auto=format&fit=crop&w=800&q=80',4.6,80,420),
 ('prd_l08','shop_lumen','cat_home','Woven Storage Basket','woven-storage-basket','Seagrass basket with handles.',54.00,45,'https://images.unsplash.com/photo-1595428774223-ef52624120d2?auto=format&fit=crop&w=800&q=80',4.5,18,72),
 ('prd_l09','shop_lumen','cat_tech','Walnut Desk Clock','walnut-desk-clock','Minimal walnut clock, silent sweep.',42.00,33,'https://images.unsplash.com/photo-1495364141860-b0d03eccd065?auto=format&fit=crop&w=800&q=80',4.3,12,55),
 ('prd_l10','shop_lumen','cat_home','Ribbed Mug Set','ribbed-mug-set','Set of four stoneware mugs.',36.00,90,'https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?auto=format&fit=crop&w=800&q=80',4.7,40,180),

 ('prd_v01','shop_verde','cat_grocery','Organic Avocados (4)','organic-avocados','Ripe Hass avocados, pack of four.',6.50,300,'https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?auto=format&fit=crop&w=800&q=80',4.5,60,900),
 ('prd_v02','shop_verde','cat_grocery','Sourdough Loaf','sourdough-loaf','Naturally leavened, baked daily.',7.00,80,'https://images.unsplash.com/photo-1585478259715-876acc5be8eb?auto=format&fit=crop&w=800&q=80',4.8,75,640),
 ('prd_v03','shop_verde','cat_grocery','Cherry Tomatoes','cherry-tomatoes','Vine-ripened, 1 pint.',4.25,150,'https://images.unsplash.com/photo-1592924357228-91a4daadcfea?auto=format&fit=crop&w=800&q=80',4.4,33,410),
 ('prd_v04','shop_verde','cat_grocery','Extra-Virgin Olive Oil','olive-oil','Cold-pressed, 500ml.',16.00,120,'https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?auto=format&fit=crop&w=800&q=80',4.7,48,300),
 ('prd_v05','shop_verde','cat_grocery','Farm Eggs (dozen)','farm-eggs','Pasture-raised, dozen.',5.50,200,'https://images.unsplash.com/photo-1518569656558-1f25e69d93d7?auto=format&fit=crop&w=800&q=80',4.6,90,720),
 ('prd_v06','shop_verde','cat_grocery','Wildflower Honey','wildflower-honey','Raw, unfiltered, 12oz.',11.00,140,'https://images.unsplash.com/photo-1587049352846-4a222e784d38?auto=format&fit=crop&w=800&q=80',4.8,52,260),
 ('prd_v07','shop_verde','cat_grocery','Baby Spinach','baby-spinach','Triple-washed, 5oz.',3.75,180,'https://images.unsplash.com/photo-1576045057995-568f588f82fb?auto=format&fit=crop&w=800&q=80',4.3,21,330),
 ('prd_v08','shop_verde','cat_grocery','Greek Yogurt','greek-yogurt','Plain, whole-milk, 32oz.',6.00,160,'https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=800&q=80',4.5,38,290),
 ('prd_v09','shop_verde','cat_grocery','Dark Chocolate Bar','dark-chocolate-bar','72% single-origin, 100g.',4.50,220,'https://images.unsplash.com/photo-1511381939415-e44015466834?auto=format&fit=crop&w=800&q=80',4.7,64,500),
 ('prd_v10','shop_verde','cat_grocery','Cold Brew Carton','cold-brew-carton','Smooth cold brew, 1L.',8.00,110,'https://images.unsplash.com/photo-1517701550927-30cf4ba1dba5?auto=format&fit=crop&w=800&q=80',4.6,29,210),

 ('prd_a01','shop_atlas','cat_apparel','Merino Crew Sweater','merino-crew-sweater','Midweight merino, everyday warmth.',98.00,60,'https://images.unsplash.com/photo-1576566588028-4147f3842f27?auto=format&fit=crop&w=800&q=80',4.6,44,180),
 ('prd_a02','shop_atlas','cat_apparel','Selvedge Denim Jacket','denim-jacket','14oz selvedge denim, classic fit.',148.00,35,'https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&w=800&q=80',4.7,31,90),
 ('prd_a03','shop_atlas','cat_apparel','Canvas Sneakers','canvas-sneakers','Vulcanized sole, organic cotton upper.',72.00,80,'https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?auto=format&fit=crop&w=800&q=80',4.4,58,300),
 ('prd_a04','shop_atlas','cat_apparel','Wool Beanie','wool-beanie','Ribbed lambswool, one size.',32.00,140,'https://images.unsplash.com/photo-1576871337622-98d48d1cf531?auto=format&fit=crop&w=800&q=80',4.5,40,260),
 ('prd_a05','shop_atlas','cat_apparel','Trail Backpack 22L','trail-backpack','Water-resistant daypack, laptop sleeve.',119.00,42,'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=800&q=80',4.8,36,120),
 ('prd_a06','shop_atlas','cat_apparel','Performance Tee','performance-tee','Breathable, quick-dry crew.',34.00,200,'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=800&q=80',4.3,72,540),
 ('prd_a07','shop_atlas','cat_apparel','Chino Shorts','chino-shorts','Stretch cotton, 7-inch inseam.',46.00,90,'https://images.unsplash.com/photo-1591195853828-11db59a44f6b?auto=format&fit=crop&w=800&q=80',4.4,25,170),
 ('prd_a08','shop_atlas','cat_apparel','Rain Shell','rain-shell','Packable 2.5-layer waterproof jacket.',159.00,28,'https://images.unsplash.com/photo-1545594861-3bf48e7fbc1b?auto=format&fit=crop&w=800&q=80',4.7,18,60),
 ('prd_a09','shop_atlas','cat_tech','Aluminum Sunglasses','aluminum-sunglasses','Polarized, lightweight aluminum frame.',88.00,75,'https://images.unsplash.com/photo-1511499767150-a48a237f0083?auto=format&fit=crop&w=800&q=80',4.5,33,150),
 ('prd_a10','shop_atlas','cat_apparel','Leather Belt','leather-belt','Full-grain leather, brass buckle.',58.00,110,'https://images.unsplash.com/photo-1624222247344-550fb60583dc?auto=format&fit=crop&w=800&q=80',4.6,27,140),

 ('prd_b01','shop_brew','cat_coffee','Single-Origin Beans 1kg','single-origin-beans','Ethiopia Yirgacheffe, whole bean.',24.00,150,'https://images.unsplash.com/photo-1559056199-641a0ac8b55e?auto=format&fit=crop&w=800&q=80',4.8,96,520),
 ('prd_b02','shop_brew','cat_coffee','Pour-Over Dripper','pour-over-dripper','Ceramic V60-style dripper.',28.00,90,'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=800&q=80',4.7,40,210),
 ('prd_b03','shop_brew','cat_coffee','Gooseneck Kettle','gooseneck-kettle','Variable-temp electric kettle, 0.9L.',79.00,50,'https://images.unsplash.com/photo-1570968915860-54d5c301fa9f?auto=format&fit=crop&w=800&q=80',4.6,28,95),
 ('prd_b04','shop_brew','cat_coffee','Burr Grinder','burr-grinder','40mm conical burr, 30 settings.',129.00,38,'https://images.unsplash.com/photo-1610889556528-9a770e32642f?auto=format&fit=crop&w=800&q=80',4.7,33,80),
 ('prd_b05','shop_brew','cat_coffee','Double-Wall Glasses (2)','double-wall-glasses','Insulated borosilicate, 250ml pair.',22.00,120,'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?auto=format&fit=crop&w=800&q=80',4.5,44,260),
 ('prd_b06','shop_brew','cat_coffee','Cold Brew Maker','cold-brew-maker','1L mason-style cold brew system.',34.00,70,'https://images.unsplash.com/photo-1461023058943-07fcbe16d735?auto=format&fit=crop&w=800&q=80',4.4,19,110),
 ('prd_b07','shop_brew','cat_coffee','Espresso Tamper','espresso-tamper','58mm stainless tamper, walnut handle.',39.00,85,'https://images.unsplash.com/photo-1610632380989-680fe40816c6?auto=format&fit=crop&w=800&q=80',4.6,22,90),
 ('prd_b08','shop_brew','cat_coffee','Reusable Travel Cup','travel-cup','Leakproof 12oz cup, bamboo lid.',26.00,160,'https://images.unsplash.com/photo-1577937927133-66ef06acdf18?auto=format&fit=crop&w=800&q=80',4.5,51,300),
 ('prd_b09','shop_brew','cat_coffee','Matcha Whisk Set','matcha-whisk-set','Bamboo chasen + ceramic bowl.',32.00,60,'https://images.unsplash.com/photo-1536013455962-7e1b6e6d6f6a?auto=format&fit=crop&w=800&q=80',4.7,15,48),
 ('prd_b10','shop_brew','cat_coffee','Decaf Sampler','decaf-sampler','Three 100g single-origin decafs.',21.00,100,'https://images.unsplash.com/photo-1442550528053-c431ecb55509?auto=format&fit=crop&w=800&q=80',4.4,12,70)
on conflict (id) do nothing;

-- a few variants (apparel sizes / coffee grind)
insert into public.product_variants (id, product_id, name, sku, price, stock) values
 ('var_a01s','prd_a01','Small','MER-S', null, 18),
 ('var_a01m','prd_a01','Medium','MER-M', null, 24),
 ('var_a01l','prd_a01','Large','MER-L', null, 18),
 ('var_b01w','prd_b01','Whole Bean','YRG-WB', null, 90),
 ('var_b01g','prd_b01','Ground','YRG-GR', 25.00, 60)
on conflict (id) do nothing;

-- ---------------------------------------------------------------------------
-- addresses
-- ---------------------------------------------------------------------------
insert into public.addresses (id, user_id, label, recipient, phone, line1, city, region, postal_code, country, lat, lng, is_default) values
 ('adr_ava','c0000000-0000-0000-0000-000000000001','Home','Ava Nguyen','+1-202-555-0111','55 Mercer St','New York','NY','10013','US',40.7233,-74.0030, true),
 ('adr_noah','c0000000-0000-0000-0000-000000000002','Home','Noah Patel','+1-202-555-0112','210 W 18th St','New York','NY','10011','US',40.7411,-74.0010, true)
on conflict (id) do nothing;

-- ---------------------------------------------------------------------------
-- promos
-- ---------------------------------------------------------------------------
insert into public.promos (id, code, description, discount_type, amount, min_subtotal, is_active) values
 ('promo_welcome','WELCOME10','10% off your first order','percent',10.00,25.00,true),
 ('promo_ship','FREESHIP','Free delivery over $40','fixed',4.99,40.00,true)
on conflict (id) do nothing;

-- ===========================================================================
-- ONE IN-FLIGHT ORDER  (Ava buys from Lumen + Verde; courier Theo en route)
--   parent ord_demo  → sub_demo_lumen (delivered? no) and sub_demo_verde
--   delivery del_demo: status on_the_way, courier cou_theo, with location pings
-- ===========================================================================
insert into public.orders (id, customer_id, address_id, ship_to, items_subtotal, delivery_fee, tax, discount, total, platform_commission, payment_status, stripe_payment_intent_id, status) values
 ('ord_demo','c0000000-0000-0000-0000-000000000001','adr_ava',
   jsonb_build_object('recipient','Ava Nguyen','line1','55 Mercer St','city','New York','region','NY','postal_code','10013','lat',40.7233,'lng',-74.0030),
   234.00, 4.99, 0.00, 0.00, 238.99, 23.40, 'captured','pi_test_demo','processing')
on conflict (id) do nothing;

insert into public.sub_orders (id, order_id, shop_id, status, items_subtotal, commission_rate, commission_amount, shop_payout, payout_status, accepted_at, ready_at) values
 ('sub_demo_lumen','ord_demo','shop_lumen','ready', 189.00, 0.1000, 18.90, 170.10,'pending', now() - interval '24 min', now() - interval '8 min'),
 ('sub_demo_verde','ord_demo','shop_verde','courier_assigned', 45.00, 0.1000, 4.50, 40.50,'pending', now() - interval '22 min', now() - interval '6 min')
on conflict (id) do nothing;

insert into public.order_items (id, sub_order_id, product_id, name_snapshot, image_snapshot, unit_price, quantity, line_total) values
 ('itm_demo1','sub_demo_lumen','prd_l01','Arc Floor Lamp','https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=800&q=80',189.00,1,189.00),
 ('itm_demo2','sub_demo_verde','prd_l06','Glass Vase Set','https://images.unsplash.com/photo-1578500494198-246f612d3b3d?auto=format&fit=crop&w=800&q=80',45.00,1,45.00)
on conflict (id) do nothing;

-- delivery for the Verde sub-order, courier Theo en route
insert into public.deliveries (id, sub_order_id, order_id, courier_id, status, pickup_lat, pickup_lng, pickup_label, dropoff_lat, dropoff_lng, dropoff_label, courier_fee, assigned_at, picked_up_at) values
 ('del_demo','sub_demo_verde','ord_demo','cou_theo','on_the_way', 40.7250,-73.9980,'Verde Market', 40.7233,-74.0030,'55 Mercer St', 3.99, now() - interval '6 min', now() - interval '4 min')
on conflict (id) do nothing;

-- a trail of courier location pings (powers the live map immediately)
insert into public.courier_locations (id, delivery_id, courier_id, lat, lng, heading, recorded_at) values
 ('loc_d1','del_demo','cou_theo',40.7250,-73.9980,210, now() - interval '4 min'),
 ('loc_d2','del_demo','cou_theo',40.7246,-73.9990,215, now() - interval '3 min'),
 ('loc_d3','del_demo','cou_theo',40.7241,-74.0001,220, now() - interval '2 min'),
 ('loc_d4','del_demo','cou_theo',40.7237,-74.0014,225, now() - interval '1 min'),
 ('loc_d5','del_demo','cou_theo',40.7235,-74.0023,230, now())
on conflict (id) do nothing;

insert into public.notifications (id, user_id, type, title, body, ref_type, ref_id) values
 ('ntf_demo1','c0000000-0000-0000-0000-000000000001','delivery_update','Your courier is on the way','Theo is 2 minutes away with your Verde Market order.','order','ord_demo'),
 ('ntf_demo2','50000000-0000-0000-0000-000000000001','new_order','New order received','Order ord_demo — Arc Floor Lamp. Mark it ready when packed.','sub_order','sub_demo_lumen')
on conflict (id) do nothing;

-- ledger: the captured charge for the demo order
insert into public.ledger (id, order_id, entry_type, amount, memo) values
 ('led_demo_charge','ord_demo','charge', 238.99, 'PaymentIntent pi_test_demo captured')
on conflict (id) do nothing;
