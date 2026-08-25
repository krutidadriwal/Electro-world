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

-- This table is only ever written to by the trusted server (via the Supabase
-- transaction pooler connection string), never directly by the Android app,
-- so Row Level Security is intentionally left off.
