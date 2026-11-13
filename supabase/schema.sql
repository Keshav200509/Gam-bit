-- Gambit database schema for Supabase (Postgres).
-- Apply in the Supabase SQL editor or with `supabase db push`.
-- Everything is readable by anyone (public leaderboards). Players may edit only their own profile;
-- stats and game results are written only by public.record_game_result().

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

-- player_stats and game_results: public read; no client writes.
-- All writes go through public.record_game_result() below, so players cannot set their own elo.
drop policy if exists "Player stats are viewable by everyone" on public.player_stats;
create policy "Player stats are viewable by everyone"
  on public.player_stats for select
  to anon, authenticated
  using (true);

drop policy if exists "Users can insert their own stats" on public.player_stats;
drop policy if exists "Users can update their own stats" on public.player_stats;

drop policy if exists "Game results are viewable by everyone" on public.game_results;
create policy "Game results are viewable by everyone"
  on public.game_results for select
  to anon, authenticated
  using (true);

drop policy if exists "Users can insert their own game results" on public.game_results;

-- ---------------------------------------------------------------------------
-- Keep profiles.updated_at current
-- ---------------------------------------------------------------------------

create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at := now();
  return new;
end;
$$;

drop trigger if exists profiles_set_updated_at on public.profiles;
create trigger profiles_set_updated_at
  before update on public.profiles
  for each row execute function public.set_updated_at();

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

-- Backfill users who signed up before this schema was installed.
insert into public.profiles (id, username, avatar_url)
select u.id, u.raw_user_meta_data ->> 'username', u.raw_user_meta_data ->> 'avatar_url'
from auth.users u
on conflict (id) do nothing;

insert into public.player_stats (user_id)
select p.id from public.profiles p
on conflict (user_id) do nothing;

-- ---------------------------------------------------------------------------
-- Server-side game recording (the only write path for stats and results)
-- ---------------------------------------------------------------------------

-- Records a finished game for the signed-in player and updates their stats.
-- Elo is computed here against a fixed 1200-rated AI opponent (K = 32), so a client
-- can move its rating by at most 32 points per recorded game.
-- Call from the client: supabase.rpc('record_game_result', { p_score, p_turns, p_mode, p_result })
create or replace function public.record_game_result(
  p_score  int,
  p_turns  int,
  p_mode   text,
  p_result text
)
returns public.player_stats
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid      uuid := auth.uid();
  v_stats    public.player_stats;
  v_actual   numeric;
  v_expected numeric;
begin
  if v_uid is null then
    raise exception 'not authenticated' using errcode = '42501';
  end if;
  if p_result is null or p_result not in ('WIN', 'LOSS', 'DRAW') then
    raise exception 'p_result must be WIN, LOSS or DRAW' using errcode = '22023';
  end if;
  if p_score is null or p_score < 0 or p_turns is null or p_turns < 1 then
    raise exception 'p_score must be >= 0 and p_turns >= 1' using errcode = '22023';
  end if;

  insert into public.player_stats (user_id) values (v_uid)
  on conflict (user_id) do nothing;

  select * into v_stats from public.player_stats where user_id = v_uid for update;

  v_actual   := case p_result when 'WIN' then 1 when 'DRAW' then 0.5 else 0 end;
  v_expected := 1 / (1 + power(10, (1200 - v_stats.elo) / 400.0));

  insert into public.game_results (user_id, score, turns, mode)
  values (v_uid, p_score, p_turns, p_mode);

  update public.player_stats
  set elo        = elo + round(32 * (v_actual - v_expected))::int,
      wins       = wins + (p_result = 'WIN')::int,
      losses     = losses + (p_result = 'LOSS')::int,
      high_score = greatest(high_score, p_score)
  where user_id = v_uid
  returning * into v_stats;

  return v_stats;
end;
$$;

revoke execute on function public.record_game_result(int, int, text, text) from public, anon;
grant execute on function public.record_game_result(int, int, text, text) to authenticated;
