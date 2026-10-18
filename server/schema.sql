-- Run once in the Supabase SQL Editor (Project -> SQL Editor -> New query).

create table if not exists public.users (
  phone text primary key,          -- E.164 format, e.g. +919876543210
  name text not null,
  how_heard_about_us text,
  created_at timestamptz not null default now(),
  last_login_at timestamptz not null default now()
);

alter table public.users add column if not exists how_heard_about_us text;

-- Google Drive folder ID (under DRIVE_ROOT_FOLDER_ID) holding this user's invoice
-- PDFs. Resolved and cached by the server the first time /api/invoices is called
-- for this user; matched by a subfolder named after their 10-digit phone number.
alter table public.users add column if not exists drive_folder_id text;

-- Login PIN, set once the phone number has been OTP-verified (see
-- /api/auth/verify-otp and /api/auth/set-pin). Null until signup completes.
alter table public.users add column if not exists pin_hash text;
alter table public.users add column if not exists pin_salt text;

-- One pending OTP per phone, sent via TextBee (see server/lib/textbee.js). A
-- new send-otp call overwrites any existing row for that phone. Verified (or
-- expired/exhausted) codes are deleted rather than kept around.
create table if not exists public.otp_codes (
  phone text primary key,
  code_hash text not null,
  expires_at timestamptz not null,
  attempts integer not null default 0,
  created_at timestamptz not null default now()
);

-- Caps OTP sends per calendar day per phone (the cap itself is
-- MAX_OTP_SENDS_PER_DAY in POST /api/auth/send-otp), independent of otp_codes -- that row gets deleted on
-- successful verification or overwritten on resend, so it can't be used to
-- remember "already sent today" across a verify. Day boundary is IST
-- (Asia/Kolkata), matching the app's user base.
create table if not exists public.otp_daily_limits (
  phone text primary key,
  send_date date not null,
  send_count integer not null default 0
);

-- These tables are only ever written to by the trusted server (via the
-- Supabase transaction pooler connection string), never directly by the
-- Android app, so Row Level Security is intentionally left off.

do $$ begin
  create type public.complaint_status as enum ('open', 'in_progress', 'resolved', 'closed');
exception
  when duplicate_object then null;
end $$;

-- Shared two-level category taxonomy: used both to categorize a complaint's
-- product and as the Wishlist's category -> subcategory browse structure.
-- Configurable here in Supabase rather than hardcoded in the app, so the
-- list can be edited without a release.
--
-- icon_key is the category's primary key (not a surrogate uuid): a short
-- admin-assigned code (e.g. 'AC', 'REF', 'TV') that's also what the app uses
-- to pick a client-side icon (see iconForKey() in WishlistScreen.kt).
create table if not exists public.categories (
  icon_key text primary key,
  name text not null unique,
  sort_order integer not null default 0
);

-- Whether this category can be requested for an in-home demo and/or a paid
-- installation via the Installation Request form. A category with neither
-- flag set is not shown on that form at all.
alter table public.categories add column if not exists can_install boolean not null default false;
alter table public.categories add column if not exists can_demo boolean not null default false;

-- One-time seed of the current install/demo capability per category. Safe to
-- re-run: existing rows are matched by icon_key and only these columns are
-- refreshed, so any other admin edits made directly in Supabase since (e.g.
-- renames, new categories) are left alone.
insert into public.categories (icon_key, name, sort_order, can_install, can_demo) values
  ('AC', 'Air Conditioners', 1, true, true),
  ('AFK', 'Air Fryer', 20, false, false),
  ('CLH', 'Cooler', 25, false, false),
  ('DFV', 'Deep Freezer/Visicooler', 25, false, false),
  ('IAB', 'Inverter & Battery', 30, true, false),
  ('KHK', 'Kitchen Hood (Chimney)', 21, true, false),
  ('LCK', 'LPG Cooktop (Stove)', 21, false, false),
  ('MWO', 'Microwave', 0, false, true),
  ('OHA', 'Other Home Appliances', 100, false, false),
  ('OKA', 'Other Kitchen Appliances', 100, false, false),
  ('REF', 'Refrigerator', 2, false, true),
  ('ROUV', 'Water Purifiers', 0, true, false),
  ('SSH', 'Sound System', 20, false, false),
  ('TV', 'Televisions', 10, true, true),
  ('VCH', 'Vacuum Cleaner', 23, false, false),
  ('WCD', 'Water Cooler/ Dispenser', 25, false, false),
  ('WHH', 'Water Heater', 24, true, false),
  ('WMC', 'Washing Machine', 0, true, true)
on conflict (icon_key) do update set
  sort_order = excluded.sort_order,
  can_install = excluded.can_install,
  can_demo = excluded.can_demo;

create table if not exists public.subcategories (
  id uuid primary key default gen_random_uuid(),
  category_icon_key text not null references public.categories(icon_key) on delete cascade,
  name text not null,
  sort_order integer not null default 0,
  unique (category_icon_key, name)
);

create index if not exists subcategories_category_idx on public.subcategories (category_icon_key, sort_order);

-- One-time migration for databases that already ran an earlier version of
-- this schema, where categories had a surrogate uuid `id` column. No-ops on
-- a fresh install, where the table above was already created in its final
-- (icon_key-keyed) shape.
do $$ begin
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'categories' and column_name = 'id') then
    update public.categories set icon_key = 'OTHER' where icon_key is null;
    alter table public.categories alter column icon_key set not null;
    alter table public.categories drop constraint if exists categories_icon_key_key;
    alter table public.categories add constraint categories_icon_key_key unique (icon_key);

    if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'complaints' and column_name = 'category_id') then
      alter table public.complaints add column if not exists category_icon_key text;
      update public.complaints comp
        set category_icon_key = c.icon_key
        from public.categories c
        where comp.category_id = c.id and comp.category_icon_key is null;
      alter table public.complaints alter column category_icon_key set not null;
      alter table public.complaints drop constraint if exists complaints_category_id_fkey;
      alter table public.complaints add constraint complaints_category_icon_key_fkey
        foreign key (category_icon_key) references public.categories(icon_key);
      alter table public.complaints drop column if exists category_id;
    end if;

    -- All FKs into categories(id) are gone now, so id can be dropped and
    -- icon_key promoted to the real primary key, matching the create table
    -- definition above.
    alter table public.categories drop constraint if exists categories_pkey;
    alter table public.categories add primary key (icon_key);
    alter table public.categories drop column if exists id;
    alter table public.categories drop column if exists is_active;
    alter table public.categories drop column if exists created_at;
  end if;
end $$;

create table if not exists public.complaints (
  id uuid primary key default gen_random_uuid(),
  phone text not null references public.users(phone),
  invoice_file_id text,          -- Drive file id of the invoice the complaint is about, if any
  invoice_file_name text,        -- denormalized so it still displays if the Drive file is renamed/removed
  category_icon_key text not null references public.categories(icon_key),
  category_name text not null,      -- denormalized so history still reads correctly if the category is later renamed
  subcategory_id uuid references public.subcategories(id),
  subcategory_name text,
  description text not null,
  address text not null,
  contact_phone text,
  status public.complaint_status not null default 'open',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  resolved_at timestamptz
);

alter table public.complaints add column if not exists subcategory_id uuid references public.subcategories(id);
alter table public.complaints add column if not exists subcategory_name text;

-- Issue type was dropped from the complaint form; address (where the
-- complaint/service is needed) was added. address has no default, so it's
-- added nullable for databases with existing rows -- the API still requires
-- it on every new complaint.
alter table public.complaints drop column if exists issue_type;
alter table public.complaints add column if not exists address text;

-- Staff-assigned reference number (e.g. a service ticket/job number from
-- their own tracking system), set from the staff app when they update a
-- complaint's status. Null until assigned -- the customer app only shows
-- this section once a number exists. Shown in both apps as "Complaint
-- request ID".
--
-- Originally complaint_number; renamed in place (data kept) for databases
-- that still have the old name. The API still sends it as
-- "complaint_number", so app builds from before the rename keep working.
do $$ begin
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'complaints' and column_name = 'complaint_number')
     and not exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'complaints' and column_name = 'complaint_request_id') then
    alter table public.complaints rename column complaint_number to complaint_request_id;
  end if;
end $$;
alter table public.complaints add column if not exists complaint_request_id text;

-- Extends the original 4-value status set with a staff-driven workflow:
-- each of these requires a specific field to be filled on the staff app
-- before the status change is allowed (see complaintsStatus in
-- api/staff/[...path].js). in_progress/resolved are left in place
-- (Postgres can't drop an enum value) but are no longer offered as a
-- target status -- any existing rows still carrying them just display
-- their raw value, same precedent as wishlist_response_status below.
alter type public.complaint_status add value if not exists 'assigned_to_service_center';
alter type public.complaint_status add value if not exists 'assigned_to_technician';
alter type public.complaint_status add value if not exists 'blocked';
alter type public.complaint_status add value if not exists 'cancelled_by_staff';
alter type public.complaint_status add value if not exists 'cancelled_by_customer';

-- One column per status-specific field above. Set once when its matching
-- status is chosen and never cleared by a later status change -- kept as a
-- historical record, same denormalize-for-history convention used
-- throughout this schema (e.g. category_name on complaints itself).
alter table public.complaints add column if not exists technician_name text;
alter table public.complaints add column if not exists technician_mobile text;
alter table public.complaints add column if not exists blocked_reason text;
alter table public.complaints add column if not exists cancellation_reason text;
alter table public.complaints add column if not exists closed_date date;

create index if not exists complaints_phone_idx on public.complaints (phone, created_at desc);

-- A wishlisted entry is either a whole category (subcategory_id null) or a
-- specific subcategory within a category -- never both for the same
-- category at once (the app enforces that: starring any subcategory drops
-- the bare-category entry). The two partial unique indexes below make each
-- shape unique per user without a sentinel value standing in for null.
create table if not exists public.wishlist_items (
  id uuid primary key default gen_random_uuid(),
  phone text not null references public.users(phone),
  category_icon_key text not null references public.categories(icon_key),
  category_name text not null,      -- denormalized so history still reads correctly if the category is later renamed
  subcategory_id uuid references public.subcategories(id),
  subcategory_name text,
  created_at timestamptz not null default now()
);

create unique index if not exists wishlist_items_category_only_key
  on public.wishlist_items (phone, category_icon_key) where subcategory_id is null;
create unique index if not exists wishlist_items_category_subcategory_key
  on public.wishlist_items (phone, category_icon_key, subcategory_id) where subcategory_id is not null;
create index if not exists wishlist_items_phone_idx on public.wishlist_items (phone, created_at desc);

do $$ begin
  create type public.installation_status as enum ('open', 'in_progress', 'resolved', 'closed');
exception
  when duplicate_object then null;
end $$;

-- A request for a product demo and/or paid installation. At least one of
-- wants_demo / wants_installation is always true (enforced below and by the
-- API); which options are even offered for a given category comes from
-- categories.can_demo / can_install.
create table if not exists public.installations (
  id uuid primary key default gen_random_uuid(),
  phone text not null references public.users(phone),
  invoice_file_id text,          -- Drive file id of the invoice the item was bought with, if any
  invoice_file_name text,        -- denormalized so it still displays if the Drive file is renamed/removed
  category_icon_key text not null references public.categories(icon_key),
  category_name text not null,      -- denormalized so history still reads correctly if the category is later renamed
  item_name text not null,
  wants_demo boolean not null default false,
  wants_installation boolean not null default false,
  address text not null,
  contact_phone text,
  status public.installation_status not null default 'open',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  resolved_at timestamptz,
  constraint installations_wants_something check (wants_demo or wants_installation)
);

-- Staff-assigned reference number, set from the staff app when they update
-- an installation request's status. Null until assigned -- the customer app
-- only shows this section once a number exists. Shown in both apps as
-- "Installation request ID".
--
-- Originally installation_number; renamed in place (data kept), same as
-- complaints.complaint_request_id above. The API still sends it as
-- "installation_number".
do $$ begin
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'installations' and column_name = 'installation_number')
     and not exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'installations' and column_name = 'installation_request_id') then
    alter table public.installations rename column installation_number to installation_request_id;
  end if;
end $$;
alter table public.installations add column if not exists installation_request_id text;

-- Same staff-driven status workflow as complaints (see the comment above
-- complaint_status's add value statements).
alter type public.installation_status add value if not exists 'assigned_to_service_center';
alter type public.installation_status add value if not exists 'assigned_to_technician';
alter type public.installation_status add value if not exists 'blocked';
alter type public.installation_status add value if not exists 'cancelled_by_staff';
alter type public.installation_status add value if not exists 'cancelled_by_customer';

alter table public.installations add column if not exists technician_name text;
alter table public.installations add column if not exists technician_mobile text;
alter table public.installations add column if not exists blocked_reason text;
alter table public.installations add column if not exists cancellation_reason text;
alter table public.installations add column if not exists closed_date date;

create index if not exists installations_phone_idx on public.installations (phone, created_at desc);

-- Delete Account (see PATCH/DELETE /api/user) removes the users row outright;
-- these were created without an explicit ON DELETE behavior (so the default
-- RESTRICT would block that delete), promoted here to CASCADE so a deleted
-- user's complaints/wishlist/installations go with them. Default constraint
-- names (Postgres auto-generates "<table>_<column>_fkey" when none is given).
alter table public.complaints drop constraint if exists complaints_phone_fkey;
alter table public.complaints add constraint complaints_phone_fkey
  foreign key (phone) references public.users(phone) on delete cascade;

alter table public.wishlist_items drop constraint if exists wishlist_items_phone_fkey;
alter table public.wishlist_items add constraint wishlist_items_phone_fkey
  foreign key (phone) references public.users(phone) on delete cascade;

alter table public.installations drop constraint if exists installations_phone_fkey;
alter table public.installations add constraint installations_phone_fkey
  foreign key (phone) references public.users(phone) on delete cascade;

do $$ begin
  create type public.invoice_request_status as enum ('open', 'in_progress', 'resolved', 'closed');
exception
  when duplicate_object then null;
end $$;

-- A user asking the store to issue/re-send an invoice they can't find in
-- their own Drive folder (e.g. an older purchase never scanned in, or a
-- lost copy). Free-text description only -- there's no structured item/
-- category to pick from since the whole point is the user doesn't have a
-- record of it themselves.
create table if not exists public.invoice_requests (
  id uuid primary key default gen_random_uuid(),
  phone text not null references public.users(phone) on delete cascade,
  description text not null,
  status public.invoice_request_status not null default 'open',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  resolved_at timestamptz
);

create index if not exists invoice_requests_phone_idx on public.invoice_requests (phone, created_at desc);

do $$ begin
  create type public.staff_role as enum ('admin', 'employee');
exception
  when duplicate_object then null;
end $$;

-- Staff (admin/employee) profiles, keyed by the Supabase Auth user id. Only
-- an admin can create these rows (via POST /api/staff/create) or change an
-- existing row's role (via PATCH /api/staff/role).
create table if not exists public.staff_profiles (
  id uuid primary key,                 -- == auth.users.id
  email text not null,
  role public.staff_role not null,
  invited_by uuid references public.staff_profiles(id),
  created_at timestamptz not null default now()
);

-- One-time migration for databases where staff_profiles.role was created as
-- plain text (check-constrained) before staff_role existed. No-op on a
-- fresh install, where the column above is already the enum type.
do $$ begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'staff_profiles' and column_name = 'role' and data_type = 'text'
  ) then
    alter table public.staff_profiles drop constraint if exists staff_profiles_role_check;
    alter table public.staff_profiles alter column role type public.staff_role using role::public.staff_role;
  end if;
end $$;

-- One row per push notification an admin sends. Rows are never deleted --
-- duration_minutes only controls how long it's shown as "active" in the
-- customer app's notification tab (created_at + duration_minutes), not how
-- long the row is kept, so history always remains queryable.
create table if not exists public.notifications (
  id uuid primary key default gen_random_uuid(),
  message text not null,
  image_drive_file_id text,
  image_url text,
  duration_minutes integer not null,
  created_by uuid not null references public.staff_profiles(id),
  created_at timestamptz not null default now()
);

create index if not exists notifications_created_at_idx on public.notifications (created_at desc);

-- FCM device tokens for customers, so a notification can be pushed to every
-- registered device. A phone may have more than one token (multiple
-- installs/devices); fcm_token is the unique key since a token belongs to
-- exactly one app install.
create table if not exists public.device_tokens (
  id uuid primary key default gen_random_uuid(),
  phone text not null references public.users(phone) on delete cascade,
  fcm_token text not null unique,
  updated_at timestamptz not null default now()
);

create index if not exists device_tokens_phone_idx on public.device_tokens (phone);

-- Snapshot of the Tally-exported retail price list (see server/lib/priceList.js),
-- fully replaced on every admin-triggered sync (POST /api/staff/price-list/sync)
-- rather than upserted -- there's no stable external id to match rows against
-- across syncs, and nothing else references this table by row id.
create table if not exists public.price_list_items (
  id uuid primary key default gen_random_uuid(),
  category text not null,
  group_name text not null,
  item_name text not null,
  final_price numeric not null,
  mrp numeric not null default 0,
  stock_label text not null default '',
  synced_at timestamptz not null default now()
);

alter table public.price_list_items add column if not exists mrp numeric not null default 0;
alter table public.price_list_items add column if not exists cost_price numeric not null default 0;

create index if not exists price_list_items_category_idx on public.price_list_items (category);


create table if not exists public.price_list_categories (
  category text primary key,
  image_path text not null,
  image_url text not null,
  updated_at timestamptz not null default now()
);

-- Tracks the sales team's outreach on a customer's wishlisted item -- calling
-- to quote a rate and gauge interest. Kept on the wishlist_items row itself
-- (not a separate table) since the response belongs to that one wishlisted
-- item, not the customer as a whole (a customer can have several wishlist
-- entries in different states).
do $$ begin
  create type public.wishlist_response_status as enum (
    'not_contacted',
    'interested',
    'not_interested',
    'call_back_later',
    'purchased',
    'unreachable'
  );
exception
  when duplicate_object then null;
end $$;

-- Expanded from the original 6-value set to match the sales team's actual
-- call-outcome taxonomy. 'interested' is left in place (Postgres can't drop
-- an enum value) even though it's no longer offered in the UI -- any
-- historical rows still carrying it just display their raw value.
alter type public.wishlist_response_status add value if not exists 'invalid_number';
alter type public.wishlist_response_status add value if not exists 'sent_details_whatsapp';
alter type public.wishlist_response_status add value if not exists 'store_visit_scheduled';
alter type public.wishlist_response_status add value if not exists 'interested_comparing_prices';
alter type public.wishlist_response_status add value if not exists 'interested_waiting_emi';
alter type public.wishlist_response_status add value if not exists 'interested_waiting_stock';
alter type public.wishlist_response_status add value if not exists 'interested_waiting_offer';
alter type public.wishlist_response_status add value if not exists 'pending_delivery';
alter type public.wishlist_response_status add value if not exists 'bought_elsewhere';
alter type public.wishlist_response_status add value if not exists 'dropped_plan';

alter table public.wishlist_items add column if not exists status public.wishlist_response_status not null default 'not_contacted';
alter table public.wishlist_items add column if not exists status_updated_at timestamptz;
alter table public.wishlist_items add column if not exists status_updated_by uuid references public.staff_profiles(id);

-- One row per tap of the Call button on a wishlist entry, for an audit trail
-- of outreach. customer_phone/customer_name/staff_email are captured at call
-- time (not just referenced by id) so the log stays readable even if the
-- wishlist entry, user, or staff member is later removed -- set null rather
-- than cascade-deleting the log itself.
create table if not exists public.wishlist_call_logs (
  id uuid primary key default gen_random_uuid(),
  wishlist_item_id uuid references public.wishlist_items(id) on delete set null,
  staff_id uuid references public.staff_profiles(id) on delete set null,
  staff_email text not null,
  customer_phone text not null,
  customer_name text not null,
  called_at timestamptz not null default now()
);

-- FCM tokens for the staff app's own push channel (new wishlist/complaint/
-- installation/invoice-request alerts) -- separate from public.device_tokens,
-- which is the customer app's (phone-keyed, not staff-keyed). Registered
-- under the staff app's own Firebase project ("electro-world-staff", distinct
-- from the customer app's "ew-app-gondia"), so sending to these tokens uses
-- its own service account -- see server/lib/staffFcm.js and the
-- STAFF_FIREBASE_SERVICE_ACCOUNT_JSON env var, not lib/fcm.js's.
create table if not exists public.staff_device_tokens (
  id uuid primary key default gen_random_uuid(),
  staff_id uuid not null references public.staff_profiles(id) on delete cascade,
  fcm_token text not null unique,
  updated_at timestamptz not null default now()
);

create index if not exists staff_device_tokens_staff_idx on public.staff_device_tokens (staff_id);

-- Pre-launch landing page sign-ups (server/public/index.html). Inserted
-- directly from the browser via the Supabase REST API with the anon key --
-- RLS below allows anon insert only, no select/update/delete, so the page
-- can take sign-ups without exposing the list or needing a new API function
-- (see the Vercel Hobby 12-function cap note in lib/db.js's callers).
create table if not exists public.waitlist (
  phone text primary key,          -- E.164 format, e.g. +919876543210
  name text,                       -- optional
  created_at timestamptz not null default now()
);

alter table public.waitlist enable row level security;

drop policy if exists "waitlist anon insert" on public.waitlist;
create policy "waitlist anon insert" on public.waitlist
  for insert
  to anon
  with check (true);

create index if not exists wishlist_call_logs_wishlist_item_idx on public.wishlist_call_logs (wishlist_item_id, called_at desc);

-- Points/rewards ledger
create table if not exists public.points_ledger (
  id uuid primary key default gen_random_uuid(),
  phone text not null references public.users(phone) on delete cascade,
  points integer not null,
  reason text not null,
  created_at timestamptz not null default now()
);

create index if not exists points_ledger_phone_idx on public.points_ledger (phone, created_at desc);

-- One row per invoice PDF already considered for purchase points (see
-- server/lib/invoicePoints.js), keyed by its Drive file id so an invoice is
-- only ever credited once. amount is the invoice total read off the PDF --
-- null if it couldn't be read, in which case points is 0 and nothing was
-- credited; delete such a row to have that invoice retried.
create table if not exists public.invoice_points (
  file_id text primary key,
  phone text not null references public.users(phone) on delete cascade,
  invoice_name text not null,
  amount numeric,
  points integer not null default 0,
  processed_at timestamptz not null default now()
);

alter table public.invoice_points enable row level security;

-- The invoice date printed on the PDF (null if it couldn't be read) --
-- decides whether the invoice earns points at all, and is what the app's
-- invoice list shows instead of the Drive upload date. Rows recorded before
-- this column existed are filled in by scripts/backfill-invoice-points.js.
alter table public.invoice_points add column if not exists invoice_date date;

-- Set when an admin needs to look at the invoice: its date couldn't be read,
-- or it's recent enough to earn points but its total couldn't be (see
-- needsReview in lib/invoicePoints.js). These are what the staff app lists
-- under "Invoices without points".
alter table public.invoice_points add column if not exists unreadable boolean not null default false;

create index if not exists invoice_points_phone_idx on public.invoice_points (phone);

-- Spin to Win (see server/lib/spin.js). Three tables, ported from the old
-- standalone spinner's products / bought_products / valid_code + leads.
--
-- Unlike the tables above, RLS is switched ON for these (with no policies,
-- so anon/authenticated get nothing): the old spinner let the browser read
-- and write prize stock directly with the anon key, which is exactly what
-- this replaces. The server's own connection owns the tables and so isn't
-- subject to RLS.

-- Prize stock. A prize is only ever on the wheel / winnable while quantity > 0;
-- each win decrements it.
create table if not exists public.spin_prizes (
  id uuid primary key default gen_random_uuid(),
  name text not null unique,
  quantity integer not null default 0 check (quantity >= 0),
  created_at timestamptz not null default now()
);

alter table public.spin_prizes enable row level security;

-- What staff pick from when unlocking a spin ("what did they buy?"), each
-- optionally mapped to the specific prizes that purchase can win. An empty
-- mapping means any in-stock prize.
create table if not exists public.spin_purchase_items (
  id uuid primary key default gen_random_uuid(),
  name text not null unique,
  mapped_prize_ids uuid[] not null default '{}',
  created_at timestamptz not null default now()
);

alter table public.spin_purchase_items enable row level security;

-- One row per spin unlocked for a customer, and -- once used_at is set --
-- the record of what it won. phone deliberately has no FK to users: staff
-- unlock a spin at the counter, possibly before the customer has installed
-- the app, and it's waiting for them when they sign up with that number.
-- bought_item / mapped_prize_ids / prize_name are copied at grant/win time
-- (not just referenced), so later edits to the purchase item or prize don't
-- rewrite what a customer was promised or won.
create table if not exists public.spin_grants (
  id uuid primary key default gen_random_uuid(),
  phone text not null,             -- E.164 format, e.g. +919876543210
  bought_item text not null,
  mapped_prize_ids uuid[] not null default '{}',
  granted_by uuid references public.staff_profiles(id) on delete set null,
  created_at timestamptz not null default now(),
  used_at timestamptz,
  prize_id uuid references public.spin_prizes(id) on delete set null,
  prize_name text
);

alter table public.spin_grants enable row level security;

create index if not exists spin_grants_phone_idx on public.spin_grants (phone, created_at);

-- When staff marked the won prize as handed over to the customer (null
-- until then). Set once from the staff app's Spins tab; a delivered win
-- can no longer be undone (see spinGrantDeliver / spinGrantUndo in
-- lib/staffSpin.js).
alter table public.spin_grants add column if not exists delivered_at timestamptz;

-- ===== TASK TRACKER (experimental) — start =====
-- Backs api/tasks/[...path].js, only reachable when the server's
-- TASK_TRACKER_MODE env var is "true". Admin-only for now (owner <->
-- developer work board); assigned_to is there for the later "all
-- employees" version. status/priority are text + check constraints rather
-- than enums so values can be added or dropped without enum migrations.
--
-- To remove the feature: delete this whole block and run
--   drop table if exists public.tracker_tasks;
create table if not exists public.tracker_tasks (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  description text not null default '',
  status text not null default 'todo'
    check (status in ('todo', 'in_progress', 'testing', 'done', 'wont_do')),
  priority text not null default 'normal'
    check (priority in ('low', 'normal', 'high', 'urgent')),
  assigned_to uuid references public.staff_profiles(id) on delete set null,
  created_by uuid references public.staff_profiles(id) on delete set null,
  updated_by uuid references public.staff_profiles(id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  completed_at timestamptz
);

alter table public.tracker_tasks enable row level security;
-- ===== TASK TRACKER (experimental) — end =====
