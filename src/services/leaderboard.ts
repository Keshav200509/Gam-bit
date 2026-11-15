import type { SupabaseClient } from '@supabase/supabase-js';

/** Rows requested from the global leaderboard when no limit is given. */
export const GLOBAL_LEADERBOARD_LIMIT = 20;

/** A ranked commander as returned by `fetchGlobalLeaderboard`. */
export interface GlobalLeaderboardEntry {
  userId: string;
  name: string;
  highScore: number;
  elo: number;
  wins: number;
  losses: number;
}

/** Shape of the embedded PostgREST row (`player_stats` with its `profiles` row). */
interface PlayerStatsRow {
  user_id: string;
  elo: number;
  wins: number;
  losses: number;
  high_score: number;
  profiles: { username: string | null } | null;
}

/**
 * Read-only standings from `player_stats`, joined to `profiles` for the display name.
 * Ordered by best score, then rating, so the two metrics agree on a single ranking.
 */
export async function fetchGlobalLeaderboard(
  client: SupabaseClient,
  limit: number = GLOBAL_LEADERBOARD_LIMIT
): Promise<GlobalLeaderboardEntry[]> {
  const { data, error } = await client
    .from('player_stats')
    .select('user_id, elo, wins, losses, high_score, profiles(username)')
    .order('high_score', { ascending: false })
    .order('elo', { ascending: false })
    .limit(limit);

  if (error) throw new Error(error.message);
  return ((data ?? []) as unknown as PlayerStatsRow[]).map(toEntry);
}

function toEntry(row: PlayerStatsRow): GlobalLeaderboardEntry {
  return {
    userId: row.user_id,
    name: displayName(row),
    highScore: row.high_score,
    elo: row.elo,
    wins: row.wins,
    losses: row.losses
  };
}

/** Falls back to a short id when the account has no username, so no row renders blank. */
function displayName(row: PlayerStatsRow): string {
  const username = row.profiles?.username?.trim();
  return username || `Commander ${row.user_id.slice(0, 4).toUpperCase()}`;
}

/** Flavour label shown under a commander's name, derived from their rating. */
export function tierForElo(elo: number): string {
  if (elo >= 1600) return 'Grandmaster';
  if (elo >= 1400) return 'Veteran';
  if (elo >= 1200) return 'Specialist';
  if (elo >= 1000) return 'Operator';
  return 'Rookie';
}
