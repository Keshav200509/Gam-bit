-- Gambit database schema for Supabase (Postgres).
-- Apply in the Supabase SQL editor or with `supabase db push`.
-- Everything is readable by anyone (public leaderboards); writes are limited to the row owner.

-- ---------------------------------------------------------------------------
-- Tables
-- ---------------------------------------------------------------------------

create table if not exists public.profiles (
  id          uuid primary key references auth.users (id) on delete cascade,
  username    text,
  avatar_url  text,
  updated_at  timestamptz not null default now()
);

create table if not exists public.player_stats (
  user_id     uuid primary key references public.profiles (id) on delete cascade,
  elo         int not null default 1200,
  wins        int not null default 0,
  losses      int not null default 0,
  high_score  int not null default 0
);

create table if not exists public.game_results (
  id          bigserial primary key,
  user_id     uuid not null references public.profiles (id) on delete cascade,
  score       int,
  turns       int,
  mode        text,
  created_at  timestamptz not null default now()
);

create index if not exists player_stats_elo_idx on public.player_stats (elo desc);
create index if not exists player_stats_high_score_idx on public.player_stats (high_score desc);
create index if not exists game_results_user_created_idx on public.game_results (user_id, created_at desc);

-- ---------------------------------------------------------------------------
-- Row Level Security
-- ---------------------------------------------------------------------------

alter table public.profiles     enable row level security;
alter table public.player_stats enable row level security;
alter table public.game_results enable row level security;

-- profiles: public read, owner insert/update
drop policy if exists "Profiles are viewable by everyone" on public.profiles;
create policy "Profiles are viewable by everyone"
  on public.profiles for select
  to anon, authenticated
  using (true);

drop policy if exists "Users can insert their own profile" on public.profiles;
create policy "Users can insert their own profile"
  on public.profiles for insert
  to authenticated
  with check ((select auth.uid()) = id);

drop policy if exists "Users can update their own profile" on public.profiles;
create policy "Users can update their own profile"
  on public.profiles for update
  to authenticated
  using ((select auth.uid()) = id)
  with check ((select auth.uid()) = id);

-- player_stats: public read (leaderboard), owner insert/update
drop policy if exists "Player stats are viewable by everyone" on public.player_stats;
create policy "Player stats are viewable by everyone"
  on public.player_stats for select
  to anon, authenticated
  using (true);

drop policy if exists "Users can insert their own stats" on public.player_stats;
create policy "Users can insert their own stats"
  on public.player_stats for insert
  to authenticated
  with check ((select auth.uid()) = user_id);

drop policy if exists "Users can update their own stats" on public.player_stats;
create policy "Users can update their own stats"
  on public.player_stats for update
  to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

-- game_results: public read, owner insert (results are append-only)
drop policy if exists "Game results are viewable by everyone" on public.game_results;
create policy "Game results are viewable by everyone"
  on public.game_results for select
  to anon, authenticated
  using (true);

drop policy if exists "Users can insert their own game results" on public.game_results;
create policy "Users can insert their own game results"
  on public.game_results for insert
  to authenticated
  with check ((select auth.uid()) = user_id);

-- ---------------------------------------------------------------------------
-- New users get a profile and a stats row automatically
-- ---------------------------------------------------------------------------

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.profiles (id, username, avatar_url)
  values (
    new.id,
    new.raw_user_meta_data ->> 'username',
    new.raw_user_meta_data ->> 'avatar_url'
  );
  insert into public.player_stats (user_id) values (new.id);
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();
