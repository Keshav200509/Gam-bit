import { AchievementItem, UserStats } from '../types/game';

export const ALL_ACHIEVEMENTS: Omit<AchievementItem, 'unlocked' | 'unlockedAt'>[] = [
  { id: 'first_win', title: 'First Win', description: 'Win your first game' },
  { id: 'strategist', title: 'Strategist', description: 'Win 10 games' },
  { id: 'perfectionist', title: 'Perfectionist', description: 'Win a game by 50+ points' },
  { id: 'comeback_kid', title: 'Comeback Kid', description: 'Win after being behind by 20+ points at round 8' },
  { id: 'scout_master', title: 'Scout Master', description: 'Use scout effectively 10 times (scouted clear -> claimed uncontested)' },
  { id: 'lock_master', title: 'Lock Master', description: 'Win 5 games using both locks every game' },
  { id: 'intel_analyst', title: 'Intel Analyst', description: 'Trust intel correctly 5 times (acted on accurate intel, won the clash)' },
  { id: 'bluff_caller', title: 'Bluff Caller', description: 'Correctly identify a bluff intel 3 times (acted against intel, won)' },
  { id: 'speed_demon', title: 'Speed Demon', description: 'Win a game in under 6 minutes' },
  { id: 'iron_player', title: 'Iron Player', description: 'Play 50 games' },
  { id: 'champion', title: 'Champion', description: 'Win all 9 configurations' },
  { id: 'arena_master', title: 'Arena Master', description: 'Win level 3 of all 3 arenas' }
];

export function checkAchievements(
  stats: UserStats,
  completedGame?: {
    playerScore: number;
    aiScore: number;
    durationMs: number;
    wasBehindAtRound8: boolean;
    usedBothLocksEveryRound: boolean;
    arena: string;
    level: string;
  }
): string[] {
  const newlyUnlocked: string[] = [];
  const currentUnlocked = new Set(stats.unlockedAchievementIds);

  const check = (id: string, condition: boolean) => {
    if (!currentUnlocked.has(id) && condition) {
      newlyUnlocked.push(id);
      currentUnlocked.add(id);
    }
  };

  if (completedGame) {
    const isWin = completedGame.playerScore > completedGame.aiScore;
    const margin = completedGame.playerScore - completedGame.aiScore;

    if (isWin) {
      check('first_win', true);
      check('strategist', stats.totalWins + 1 >= 10);
      check('perfectionist', margin >= 50);
      check('comeback_kid', completedGame.wasBehindAtRound8);
      check('speed_demon', completedGame.durationMs < 6 * 60 * 1000);
      check('lock_master', completedGame.usedBothLocksEveryRound && stats.bothLocksWinCount + 1 >= 5);
    }
  }

  check('iron_player', stats.totalGames >= 50);
  check('scout_master', stats.scoutEffectiveCount >= 10);
  check('intel_analyst', stats.intelAnalystCount >= 5);
  check('bluff_caller', stats.bluffCallerCount >= 3);

  return newlyUnlocked;
}
