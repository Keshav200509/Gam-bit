import { describe, expect, it, vi } from 'vitest';
import type { SupabaseClient } from '@supabase/supabase-js';
import { buildLocalStatsPayload, syncLocalStatsToCloud } from '../cloudSync';
import { DEFAULT_PROFILE } from '../storage';
import { GameHistoryEntry, UserStats } from '../../types/game';

const stats = (over: Partial<UserStats>): UserStats => ({ ...DEFAULT_PROFILE.stats, ...over });

const game = (result: GameHistoryEntry['result'], playerScore: number): GameHistoryEntry => ({
  id: `${result}-${playerScore}`,
  date: '1/1/2026',
  arena: 'ASCENDENCY',
  level: 'LEVEL_1',
  playerScore,
  aiScore: 0,
  result,
  roundsPlayed: 12,
  clashesWon: 0,
  clashesLost: 0,
  intelAccuracy: 0,
  scoutUses: 0,
  lockUses: 0
});

describe('buildLocalStatsPayload', () => {
  it('maps wins, non-draw losses and the best history score', () => {
    const payload = buildLocalStatsPayload(stats({ totalGames: 10, totalWins: 6 }), [
      game('WIN', 31),
      game('DRAW', 20),
      game('LOSS', 44)
    ]);
    expect(payload).toEqual({ p_wins: 6, p_losses: 3, p_high_score: 44 });
  });

  it('is all zeros for a fresh profile', () => {
    expect(buildLocalStatsPayload(stats({}), [])).toEqual({ p_wins: 0, p_losses: 0, p_high_score: 0 });
  });

  it('never reports negative losses', () => {
    const payload = buildLocalStatsPayload(stats({ totalGames: 1, totalWins: 1 }), [game('DRAW', 5)]);
    expect(payload.p_losses).toBe(0);
  });
});

describe('syncLocalStatsToCloud', () => {
  it('calls the sync_local_stats RPC and returns the row', async () => {
    const row = { user_id: 'u', elo: 1200, wins: 0, losses: 0, high_score: 0, local_synced_at: 'now' };
    const rpc = vi.fn().mockResolvedValue({ data: row, error: null });
    const result = await syncLocalStatsToCloud({ rpc } as unknown as SupabaseClient);
    expect(rpc).toHaveBeenCalledWith('sync_local_stats', expect.objectContaining({ p_wins: expect.any(Number) }));
    expect(result).toBe(row);
  });

  it('throws the RPC error message', async () => {
    const rpc = vi.fn().mockResolvedValue({ data: null, error: { message: 'invalid offline stats' } });
    await expect(syncLocalStatsToCloud({ rpc } as unknown as SupabaseClient)).rejects.toThrow('invalid offline stats');
  });
});
