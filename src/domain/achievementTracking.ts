import {
  ArenaConfiguration,
  ArenaId,
  ClashResult,
  GameAchievementProgress,
  GameLevelId,
  IntelHint,
  Position,
  ScoutResult,
  Token,
  UserStats
} from '../types/game';
import { configKey } from './achievements';
import { getRegionName, parsePosKey, posKey } from './positionUtils';

export function createAchievementProgress(now: number = Date.now()): GameAchievementProgress {
  return {
    startedAt: now,
    wasBehindAtRound8: false,
    usedAllLocksEveryRound: true,
    effectiveScouts: 0,
    intelTrusted: 0,
    bluffsCalled: 0
  };
}

export interface CompletedRound {
  config: Pick<ArenaConfiguration, 'arena' | 'maxLocksPerRound'>;
  roundNumber: number;
  playerPlacements: Record<string, Token>;
  aiPlacements: Record<string, Token>;
  playerLocks: Position[];
  results: Record<string, ClashResult>;
  intel: IntelHint | null;
  scoutedPosition: Position | null;
  scoutResult: ScoutResult | null;
  playerScore: number;
  aiScore: number;
}

const isPlayerContestedWin = (round: CompletedRound, key: string): boolean => {
  const type = round.results[key]?.type;
  return (
    !!round.playerPlacements[key] &&
    !!round.aiPlacements[key] &&
    (type === 'PlayerWins' || type === 'PlayerDefends')
  );
};

/** Scout said CLEAR and the player then claimed the scouted cell (Ascendency: region) uncontested. */
export function isEffectiveScout(round: CompletedRound): boolean {
  const { scoutedPosition, scoutResult } = round;
  if (!scoutedPosition || scoutResult !== 'CLEAR') return false;

  const targetKeys =
    round.config.arena === 'ASCENDENCY'
      ? Object.keys(round.playerPlacements).filter(k => {
          const p = parsePosKey(k);
          return getRegionName(p.row, p.col) === getRegionName(scoutedPosition.row, scoutedPosition.col);
        })
      : [posKey(scoutedPosition)].filter(k => round.playerPlacements[k]);

  return targetKeys.some(k => round.results[k]?.type === 'PlayerUncontested');
}

/**
 * Only intel that names a region can be acted on.
 * - TRUSTED: intel was true, the player placed in the named region and won a contested clash there.
 * - BLUFF_CALLED: intel was false, the player stayed out of the named region and won a contested clash elsewhere.
 */
export function evaluateIntelUse(round: CompletedRound): 'TRUSTED' | 'BLUFF_CALLED' | null {
  const region = round.intel?.actualData.region;
  if (!round.intel || !region) return null;

  const keysInRegion = Object.keys(round.playerPlacements).filter(k => {
    const p = parsePosKey(k);
    return getRegionName(p.row, p.col) === region;
  });

  if (round.intel.isTrue) {
    return keysInRegion.some(k => isPlayerContestedWin(round, k)) ? 'TRUSTED' : null;
  }
  if (keysInRegion.length > 0) return null;
  return Object.keys(round.playerPlacements).some(k => isPlayerContestedWin(round, k)) ? 'BLUFF_CALLED' : null;
}

export function applyRoundToProgress(
  progress: GameAchievementProgress,
  round: CompletedRound
): GameAchievementProgress {
  const maxLocks = round.config.maxLocksPerRound;
  const intelUse = evaluateIntelUse(round);
  return {
    ...progress,
    wasBehindAtRound8:
      progress.wasBehindAtRound8 || (round.roundNumber === 8 && round.aiScore - round.playerScore >= 20),
    usedAllLocksEveryRound:
      progress.usedAllLocksEveryRound && maxLocks > 0 && round.playerLocks.length >= maxLocks,
    effectiveScouts: progress.effectiveScouts + (isEffectiveScout(round) ? 1 : 0),
    intelTrusted: progress.intelTrusted + (intelUse === 'TRUSTED' ? 1 : 0),
    bluffsCalled: progress.bluffsCalled + (intelUse === 'BLUFF_CALLED' ? 1 : 0)
  };
}

export interface CompletedGame {
  isWin: boolean;
  arena: ArenaId;
  level: GameLevelId;
  isDailyChallenge: boolean;
  clashesWon: number;
  clashesLost: number;
  progress: GameAchievementProgress;
}

/** Applies a finished game to the profile stats. Call before `checkAchievements`. */
export function applyGameToStats(stats: UserStats, game: CompletedGame): UserStats {
  const next: UserStats = {
    ...stats,
    totalGames: stats.totalGames + 1,
    clashesWon: stats.clashesWon + game.clashesWon,
    clashesLost: stats.clashesLost + game.clashesLost,
    scoutEffectiveCount: stats.scoutEffectiveCount + game.progress.effectiveScouts,
    intelAnalystCount: stats.intelAnalystCount + game.progress.intelTrusted,
    bluffCallerCount: stats.bluffCallerCount + game.progress.bluffsCalled
  };

  if (!game.isWin) {
    next.pvpRating = Math.max(800, stats.pvpRating - 15);
    return next;
  }

  next.totalWins = stats.totalWins + 1;
  next.pvpRating = stats.pvpRating + 25;
  if (game.progress.usedAllLocksEveryRound) next.bothLocksWinCount = stats.bothLocksWinCount + 1;
  if (game.progress.wasBehindAtRound8) next.comebackKidCount = stats.comebackKidCount + 1;
  if (!game.isDailyChallenge) {
    next.configurationsWon = [...new Set([...(stats.configurationsWon ?? []), configKey(game.arena, game.level)])];
  }
  return next;
}
