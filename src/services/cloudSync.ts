import type { SupabaseClient } from '@supabase/supabase-js';
import { GameHistoryEntry, UserStats } from '../types/game';
import { storage } from './storage';

export interface LocalStatsPayload {
  p_wins: number;
  p_losses: number;
  p_high_score: number;
}

export interface CloudPlayerStats {
  user_id: string;
  elo: number;
  wins: number;
  losses: number;
  high_score: number;
  local_synced_at: string | null;
}

/**
 * Maps offline career stats onto the `player_stats` columns.
 * Local stats don't count draws, so draws found in the (50-entry) history are excluded from losses.
 */
export function buildLocalStatsPayload(stats: UserStats, history: GameHistoryEntry[]): LocalStatsPayload {
  const draws = history.filter(h => h.result === 'DRAW').length;
  const highScore = history.reduce((max, h) => Math.max(max, h.playerScore), 0);
  return {
    p_wins: stats.totalWins,
    p_losses: Math.max(0, stats.totalGames - stats.totalWins - draws),
    p_high_score: highScore
  };
}

/** Imports the offline career once per account (the server ignores repeat calls). */
export async function syncLocalStatsToCloud(client: SupabaseClient): Promise<CloudPlayerStats> {
  const payload = buildLocalStatsPayload(storage.getStats(), storage.getHistory());
  const { data, error } = await client.rpc('sync_local_stats', payload);
  if (error) throw new Error(error.message);
  return data as CloudPlayerStats;
}
