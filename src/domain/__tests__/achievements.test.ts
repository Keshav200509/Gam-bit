import { describe, expect, it } from 'vitest';
import { checkAchievements, configKey } from '../achievements';
import {
  applyGameToStats,
  applyRoundToProgress,
  CompletedRound,
  createAchievementProgress,
  evaluateIntelUse,
  isEffectiveScout
} from '../achievementTracking';
import { DEFAULT_PROFILE } from '../../services/storage';
import { IntelHint, UserStats } from '../../types/game';

const tok = (value: number) => ({ value, isLocked: false });
const stats = (patch: Partial<UserStats> = {}): UserStats => ({ ...DEFAULT_PROFILE.stats, ...patch });
const intel = (isTrue: boolean, region = 'northwest'): IntelHint => ({
  text: '',
  isTrue,
  reliabilityLabel: '',
  type: 'TOKEN_REGION',
  actualData: { region, tokenValue: 3 }
});
const round = (patch: Partial<CompletedRound> = {}): CompletedRound => ({
  config: { arena: 'CONFRONTATION', maxLocksPerRound: 2 },
  roundNumber: 1,
  playerPlacements: {},
  aiPlacements: {},
  playerLocks: [],
  results: {},
  intel: null,
  scoutedPosition: null,
  scoutResult: null,
  playerScore: 0,
  aiScore: 0,
  ...patch
});

describe('isEffectiveScout', () => {
  it('counts a CLEAR scout followed by an uncontested claim of that cell', () => {
    const r = round({
      scoutedPosition: { row: 1, col: 1 },
      scoutResult: 'CLEAR',
      playerPlacements: { '1,1': tok(2) },
      results: { '1,1': { type: 'PlayerUncontested' } }
    });
    expect(isEffectiveScout(r)).toBe(true);
  });

  it('ignores CONTESTED scouts and scouts the player did not follow up on', () => {
    expect(isEffectiveScout(round({ scoutedPosition: { row: 1, col: 1 }, scoutResult: 'CLEAR' }))).toBe(false);
    const contested = round({
      scoutedPosition: { row: 1, col: 1 },
      scoutResult: 'CONTESTED',
      playerPlacements: { '1,1': tok(2) },
      results: { '1,1': { type: 'PlayerUncontested' } }
    });
    expect(isEffectiveScout(contested)).toBe(false);
  });

  it('uses the scouted region in Ascendency', () => {
    const r = round({
      config: { arena: 'ASCENDENCY', maxLocksPerRound: 2 },
      scoutedPosition: { row: 0, col: 0 },
      scoutResult: 'CLEAR',
      playerPlacements: { '1,1': tok(2) },
      results: { '1,1': { type: 'PlayerUncontested' } }
    });
    expect(isEffectiveScout(r)).toBe(true);
  });
});

describe('evaluateIntelUse', () => {
  const contestedWinAt = (key: string) => ({
    playerPlacements: { [key]: tok(5) },
    aiPlacements: { [key]: tok(1) },
    results: { [key]: { type: 'PlayerWins' as const } }
  });

  it('TRUSTED when true intel is followed and a contested clash is won in that region', () => {
    expect(evaluateIntelUse(round({ intel: intel(true), ...contestedWinAt('0,0') }))).toBe('TRUSTED');
  });

  it('BLUFF_CALLED when false intel is avoided and a contested clash is won elsewhere', () => {
    expect(evaluateIntelUse(round({ intel: intel(false), ...contestedWinAt('4,4') }))).toBe('BLUFF_CALLED');
  });

  it('nothing when the player fell for a bluff, ignored true intel, or only won uncontested', () => {
    expect(evaluateIntelUse(round({ intel: intel(false), ...contestedWinAt('0,0') }))).toBeNull();
    expect(evaluateIntelUse(round({ intel: intel(true), ...contestedWinAt('4,4') }))).toBeNull();
    const uncontested = round({
      intel: intel(true),
      playerPlacements: { '0,0': tok(5) },
      results: { '0,0': { type: 'PlayerUncontested' } }
    });
    expect(evaluateIntelUse(uncontested)).toBeNull();
  });

  it('ignores intel that names no region', () => {
    const strategy: IntelHint = { ...intel(true), type: 'STRATEGY', actualData: { strategy: 'STEALING_CELLS' } };
    expect(evaluateIntelUse(round({ intel: strategy, ...contestedWinAt('0,0') }))).toBeNull();
  });
});

describe('applyRoundToProgress', () => {
  it('requires every available lock in every round', () => {
    const lock = { row: 0, col: 0 };
    let p = createAchievementProgress(0);
    p = applyRoundToProgress(p, round({ playerLocks: [lock, { row: 1, col: 1 }] }));
    expect(p.usedAllLocksEveryRound).toBe(true);
    p = applyRoundToProgress(p, round({ playerLocks: [lock] }));
    expect(p.usedAllLocksEveryRound).toBe(false);

    const oneLockLevel = round({ config: { arena: 'CONFRONTATION', maxLocksPerRound: 1 }, playerLocks: [lock] });
    expect(applyRoundToProgress(createAchievementProgress(0), oneLockLevel).usedAllLocksEveryRound).toBe(true);

    const noLockLevel = round({ config: { arena: 'OBLIVION', maxLocksPerRound: 0 } });
    expect(applyRoundToProgress(createAchievementProgress(0), noLockLevel).usedAllLocksEveryRound).toBe(false);
  });

  it('flags being 20+ behind at the end of round 8 only', () => {
    const behind = { playerScore: 10, aiScore: 30 };
    expect(applyRoundToProgress(createAchievementProgress(0), round({ roundNumber: 7, ...behind })).wasBehindAtRound8).toBe(false);
    expect(applyRoundToProgress(createAchievementProgress(0), round({ roundNumber: 8, ...behind })).wasBehindAtRound8).toBe(true);
  });
});

describe('applyGameToStats', () => {
  const game = {
    isWin: true,
    arena: 'OBLIVION' as const,
    level: 'LEVEL_3' as const,
    isDailyChallenge: false,
    clashesWon: 4,
    clashesLost: 2,
    progress: { ...createAchievementProgress(0), effectiveScouts: 2, intelTrusted: 1, bluffsCalled: 3 }
  };

  it('applies a win, including achievement counters, without mutating the input', () => {
    const before = stats();
    const after = applyGameToStats(before, game);
    expect(after).toMatchObject({
      totalGames: 1,
      totalWins: 1,
      clashesWon: 4,
      clashesLost: 2,
      scoutEffectiveCount: 2,
      intelAnalystCount: 1,
      bluffCallerCount: 3,
      bothLocksWinCount: 1,
      configurationsWon: ['OBLIVION:LEVEL_3']
    });
    expect(before.totalGames).toBe(0);
    expect(before.configurationsWon).toEqual([]);
  });

  it('does not record Daily Challenge wins as configurations', () => {
    expect(applyGameToStats(stats(), { ...game, isDailyChallenge: true }).configurationsWon).toEqual([]);
  });

  it('a loss only updates games, clashes, counters and rating', () => {
    const after = applyGameToStats(stats(), { ...game, isWin: false });
    expect(after.totalWins).toBe(0);
    expect(after.bothLocksWinCount).toBe(0);
    expect(after.pvpRating).toBe(985);
    expect(after.bluffCallerCount).toBe(3);
  });
});

describe('checkAchievements', () => {
  const win = {
    playerScore: 60,
    aiScore: 50,
    durationMs: 10 * 60 * 1000,
    wasBehindAtRound8: false,
    usedBothLocksEveryRound: false,
    arena: 'ASCENDENCY',
    level: 'LEVEL_1'
  };

  it('unlocks Strategist at 10 wins, not 9', () => {
    expect(checkAchievements(stats({ totalWins: 9 }), win)).not.toContain('strategist');
    expect(checkAchievements(stats({ totalWins: 10 }), win)).toContain('strategist');
  });

  it('unlocks Lock Master on the 5th qualifying win', () => {
    expect(checkAchievements(stats({ bothLocksWinCount: 4 }), win)).not.toContain('lock_master');
    expect(checkAchievements(stats({ bothLocksWinCount: 5 }), win)).toContain('lock_master');
  });

  it('unlocks Arena Master and Champion from won configurations', () => {
    const level3 = ['ASCENDENCY', 'CONFRONTATION', 'OBLIVION'].map(a => configKey(a, 'LEVEL_3'));
    const all = ['ASCENDENCY', 'CONFRONTATION', 'OBLIVION'].flatMap(a =>
      ['LEVEL_1', 'LEVEL_2', 'LEVEL_3'].map(l => configKey(a, l))
    );
    const partial = checkAchievements(stats({ configurationsWon: level3 }));
    expect(partial).toContain('arena_master');
    expect(partial).not.toContain('champion');
    expect(checkAchievements(stats({ configurationsWon: all }))).toContain('champion');
  });

  it('unlocks Intel Analyst and Bluff Caller from their counters', () => {
    const unlocked = checkAchievements(stats({ intelAnalystCount: 5, bluffCallerCount: 3 }));
    expect(unlocked).toEqual(expect.arrayContaining(['intel_analyst', 'bluff_caller']));
  });
});
