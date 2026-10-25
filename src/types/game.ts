export type CellOwner = 'NONE' | 'PLAYER' | 'AI';

export type CellModifier = 'None' | 'GoldenCell' | 'Volatile' | 'Battleground';

export interface Position {
  row: number;
  col: number;
}

export interface Cell {
  value: number;
  owner: CellOwner;
  fortify: number;
  modifier: CellModifier;
}

export interface Board {
  cells: Cell[][];
}

export interface Token {
  value: number;
  isLocked: boolean;
}

export type ArenaId = 'ASCENDENCY' | 'CONFRONTATION' | 'OBLIVION';

export type GameLevelId = 'LEVEL_1' | 'LEVEL_2' | 'LEVEL_3';

export interface ArenaInfo {
  id: ArenaId;
  displayName: string;
  description: string;
  tagline: string;
}

export interface ArenaConfiguration {
  arena: ArenaId;
  level: GameLevelId;
  aiCapabilityMin: number;
  aiCapabilityMax: number;
  intelTrickinessMultiplier: number;
  maxLocksPerRound: number;
  intelEnabled: boolean;
  scoutEnabled: boolean;
  probabilityPreviewEnabled: boolean;
  humanMovesFirst: boolean;
  aiBluffRate: number;
}

export type GamePhase =
  | 'Intel'
  | 'Placing'
  | 'Revealing'
  | 'Resolving'
  | 'IncomeSummary'
  | 'RoundTransition'
  | 'GameOver';

export type ScoutResult = 'CLEAR' | 'CONTESTED';

export type ClashResultType =
  | 'PlayerWins'
  | 'PlayerUncontested'
  | 'PlayerDefends'
  | 'AIWins'
  | 'AIUncontested'
  | 'AIDefends'
  | 'Tie';

export interface ClashResult {
  type: ClashResultType;
  playerStrength?: number;
  aiStrength?: number;
}

export interface ProbabilityBreakdown {
  win: number;
  lose: number;
  tie: number;
  uncontested: number;
}

export type LateGameEventType =
  | 'NONE'
  | 'GOLDEN_CELL'
  | 'VOLATILE'
  | 'DOUBLE_STAKES'
  | 'FINAL_GAMBIT';

export interface LateGameEvent {
  type: LateGameEventType;
  modifiedBoard: Board;
  modifiedPlayerLocks: Position[];
  modifiedAiLocks: Position[];
  incomeMultiplier: number;
  message: string;
}

export type IntelType = 'TOKEN_REGION' | 'STRATEGY' | 'CELL_VALUE';

export interface IntelHint {
  text: string;
  isTrue: boolean;
  reliabilityLabel: string;
  type: IntelType;
  actualData: {
    tokenValue?: number;
    region?: string;
    strategy?: string;
    cellValue?: number;
  };
}

export type AICapabilityName = 'PEAK' | 'SHARP' | 'STEADY' | 'SLOPPY';

export interface AICapability {
  name: AICapabilityName;
  minModifier: number;
  maxModifier: number;
  displayName: string;
  colorHex: string;
  modifier: number;
}

export type AIStrategy =
  | 'AGGRESSIVE_EXPANSION'
  | 'TERRITORY_FORTIFICATION'
  | 'STEALING_CELLS'
  | 'SECURING_HIGH_VALUE'
  | 'SPREADING_THIN'
  | 'BUILDING_CLUSTER';

export interface AIPlan {
  placements: Record<string, Token>; // key formatted as "r,c"
  locks: Position[];
  strategy: AIStrategy;
}

export interface RoundState {
  roundNumber: number;
  phase: GamePhase;
  board: Board;
  playerPlacements: Record<string, Token>; // key formatted as "r,c"
  aiPlacements: Record<string, Token>;
  playerLocks: Position[];
  aiLocks: Position[];
  scoutUsed: boolean;
  scoutedPosition: Position | null;
  scoutResult: ScoutResult | null;
  intelHint: string;
  intelConfidence: string;
  aiCapabilityName: AICapabilityName;
  aiCapabilityModifier: number;
  intel: IntelHint | null;
  incomeMultiplier: number;
  lateGameMessage: string;
  lateGameEventType: LateGameEventType;
}

export interface Score {
  playerScore: number;
  aiScore: number;
}

export interface AchievementItem {
  id: string;
  title: string;
  description: string;
  unlocked: boolean;
  unlockedAt?: string;
}

export interface UserStats {
  totalGames: number;
  totalWins: number;
  clashesWon: number;
  clashesLost: number;
  scoutEffectiveCount: number;
  intelAnalystCount: number;
  bluffCallerCount: number;
  bothLocksWinCount: number;
  comebackKidCount: number;
  pvpRating: number;
  lastDailyChallengeDate?: string;
  unlockedAchievementIds: string[];
}

export interface UserProfile {
  uid: string;
  displayName: string;
  photoUrl: string;
  stats: UserStats;
  arenaProgress: {
    ASCENDENCY: number; // max level unlocked (1, 2, 3)
    CONFRONTATION: number;
    OBLIVION: number;
  };
}

export interface GameHistoryEntry {
  id: string;
  date: string;
  arena: ArenaId;
  level: GameLevelId;
  playerScore: number;
  aiScore: number;
  result: 'WIN' | 'LOSS' | 'DRAW';
  roundsPlayed: number;
  clashesWon: number;
  clashesLost: number;
  intelAccuracy: number;
  scoutUses: number;
  lockUses: number;
  isDailyChallenge?: boolean;
}

export interface GameSettings {
  soundEnabled: boolean;
  masterVolume: number;
  ambientMusicEnabled: boolean;
  hapticsEnabled: boolean;
  animationSpeed: 'Normal' | 'Fast' | 'Instant';
  aiCapabilityVisible: boolean;
  showProbabilityTooltips: boolean;
  confirmBeforeCommit: boolean;
  displayTheme: 'dark-cosmic' | 'midnight-cyber' | 'obsidian-minimal';
}

export type ClashStage =
  | 'NONE'
  | 'TOKENS_APPEAR'
  | 'STRENGTHS_SHOWN'
  | 'OUTCOME_REVEALED';
