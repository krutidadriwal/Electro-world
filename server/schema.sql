-- Run once in the Supabase SQL Editor (Project -> SQL Editor -> New query).

create table if not exists public.users (
  phone text primary key,          -- E.164 format, e.g. +919876543210
  name text not null,
  created_at timestamptz not null default now(),
  last_login_at timestamptz not null default now()
);

-- This table is only ever written to by the trusted server (via the Supabase
-- transaction pooler connection string), never directly by the Android app,
-- so Row Level Security is intentionally left off.
