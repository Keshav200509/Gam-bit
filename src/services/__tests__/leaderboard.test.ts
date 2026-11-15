import { describe, expect, it, vi } from 'vitest';
import type { SupabaseClient } from '@supabase/supabase-js';
import { fetchGlobalLeaderboard, tierForElo } from '../leaderboard';

/**
 * Builds a stub standing in for the PostgREST query builder. Each method returns
 * `this` so the chain in the service can be followed, and the final `limit` call
 * resolves with the supplied result.
 */
function mockClient(result: { data: unknown; error: unknown }) {
  const limit = vi.fn().mockResolvedValue(result);
  const order = vi.fn();
  const select = vi.fn();
  const from = vi.fn();

  const chain = { select, order, limit };
  select.mockReturnValue(chain);
  order.mockReturnValue(chain);
  from.mockReturnValue(chain);

  return { client: { from } as unknown as SupabaseClient, from, select, order, limit };
}

const row = (over: Record<string, unknown> = {}) => ({
  user_id: 'aaaaaaaa-1111',
  elo: 1300,
  wins: 10,
  losses: 4,
  high_score: 120,
  profiles: { username: 'Vortex_Prime' },
  ...over
});

describe('fetchGlobalLeaderboard', () => {
  it('maps rows, keeping the joined profile name', async () => {
    const { client } = mockClient({ data: [row()], error: null });

    await expect(fetchGlobalLeaderboard(client)).resolves.toEqual([
      { userId: 'aaaaaaaa-1111', name: 'Vortex_Prime', highScore: 120, elo: 1300, wins: 10, losses: 4 }
    ]);
  });

  it('queries player_stats joined to profiles and orders by score then elo', async () => {
    const { client, from, select, order, limit } = mockClient({ data: [], error: null });

    await fetchGlobalLeaderboard(client, 5);

    expect(from).toHaveBeenCalledWith('player_stats');
    expect(select).toHaveBeenCalledWith('user_id, elo, wins, losses, high_score, profiles(username)');
    expect(order).toHaveBeenNthCalledWith(1, 'high_score', { ascending: false });
    expect(order).toHaveBeenNthCalledWith(2, 'elo', { ascending: false });
    expect(limit).toHaveBeenCalledWith(5);
  });

  it('returns an empty list when the table has no rows', async () => {
    const { client } = mockClient({ data: [], error: null });
    await expect(fetchGlobalLeaderboard(client)).resolves.toEqual([]);
  });

  it('treats a null data payload as an empty list', async () => {
    const { client } = mockClient({ data: null, error: null });
    await expect(fetchGlobalLeaderboard(client)).resolves.toEqual([]);
  });

  it('throws the database error message', async () => {
    const { client } = mockClient({ data: null, error: { message: 'permission denied for table player_stats' } });
    await expect(fetchGlobalLeaderboard(client)).rejects.toThrow('permission denied for table player_stats');
  });

  it('substitutes a readable name when the profile or username is missing', async () => {
    const { client } = mockClient({
      data: [row({ profiles: null }), row({ user_id: 'bbbbbbbb-2222', profiles: { username: '   ' } })],
      error: null
    });

    const [noProfile, blankName] = await fetchGlobalLeaderboard(client);
    expect(noProfile.name).toBe('Commander AAAA');
    expect(blankName.name).toBe('Commander BBBB');
  });
});

describe('tierForElo', () => {
  it('maps ratings onto tiers, including the boundaries', () => {
    expect(tierForElo(1600)).toBe('Grandmaster');
    expect(tierForElo(1400)).toBe('Veteran');
    expect(tierForElo(1200)).toBe('Specialist');
    expect(tierForElo(1000)).toBe('Operator');
    expect(tierForElo(999)).toBe('Rookie');
  });
});
