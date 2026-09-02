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

create index if not exists installations_phone_idx on public.installations (phone, created_at desc);
