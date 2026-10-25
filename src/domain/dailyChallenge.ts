import { ArenaId, GameLevelId } from '../types/game';

function stringHash(str: string): number {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i);
    hash = (hash << 5) - hash + char;
    hash |= 0;
  }
  return hash;
}

export function getTodayDateString(): string {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export interface DailyChallengeConfig {
  date: string;
  arena: ArenaId;
  level: GameLevelId;
  boardSeed: number;
  aiSeed: number;
}

export function generateDailyChallenge(dateStr: string = getTodayDateString()): DailyChallengeConfig {
  const hash = stringHash(dateStr);
  const arenas: ArenaId[] = ['ASCENDENCY', 'CONFRONTATION', 'OBLIVION'];
  const index = Math.abs(hash) % arenas.length;

  return {
    date: dateStr,
    arena: arenas[index],
    level: 'LEVEL_2',
    boardSeed: hash,
    aiSeed: stringHash(dateStr + 'ai')
  };
}

export function createSeededRandom(seed: number): () => number {
  let s = Math.abs(seed) || 123456789;
  return function () {
    s = (s * 16807) % 2147483647;
    return (s - 1) / 2147483646;
  };
}
