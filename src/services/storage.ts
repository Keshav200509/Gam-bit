import {
  GameHistoryEntry,
  GameSettings,
  RoundState,
  Score,
  UserProfile,
  ArenaId,
  GameLevelId
} from '../types/game';

const PROFILE_KEY = 'gambit_user_profile';
const SAVE_KEY = 'gambit_active_save';
const HISTORY_KEY = 'gambit_game_history';
const SETTINGS_KEY = 'gambit_settings';
const ONBOARDING_KEY = 'gambit_onboarding_done';
const DAILY_RESULTS_KEY = 'gambit_daily_results';

export interface SavedGameData {
  score: Score;
  roundState: RoundState;
  arena: ArenaId;
  level: GameLevelId;
  isDailyChallenge: boolean;
  dailyChallengeSeed: number;
  timestamp: number;
}

export const DEFAULT_SETTINGS: GameSettings = {
  soundEnabled: true,
  masterVolume: 0.8,
  ambientMusicEnabled: true,
  hapticsEnabled: true,
  animationSpeed: 'Normal',
  aiCapabilityVisible: true,
  showProbabilityTooltips: true,
  confirmBeforeCommit: false,
  displayTheme: 'dark-cosmic'
};

export const DEFAULT_PROFILE: UserProfile = {
  uid: 'usr_' + Math.random().toString(36).substring(2, 9),
  displayName: 'Commander',
  photoUrl: 'avatar-1',
  stats: {
    totalGames: 0,
    totalWins: 0,
    clashesWon: 0,
    clashesLost: 0,
    scoutEffectiveCount: 0,
    intelAnalystCount: 0,
    bluffCallerCount: 0,
    bothLocksWinCount: 0,
    comebackKidCount: 0,
    pvpRating: 1000,
    unlockedAchievementIds: []
  },
  arenaProgress: {
    ASCENDENCY: 3,
    CONFRONTATION: 1,
    OBLIVION: 1
  }
};

export function getStoredOnboardingCompleted(): boolean {
  try {
    return localStorage.getItem(ONBOARDING_KEY) === 'true';
  } catch {
    return false;
  }
}

export function setStoredOnboardingCompleted(completed: boolean): void {
  try {
    localStorage.setItem(ONBOARDING_KEY, completed ? 'true' : 'false');
  } catch {
    // Ignore
  }
}

export function getStoredProfile(): UserProfile {
  try {
    const raw = localStorage.getItem(PROFILE_KEY);
    if (!raw) return DEFAULT_PROFILE;
    return { ...DEFAULT_PROFILE, ...JSON.parse(raw) };
  } catch {
    return DEFAULT_PROFILE;
  }
}

export function saveStoredProfile(profile: UserProfile): void {
  try {
    localStorage.setItem(PROFILE_KEY, JSON.stringify(profile));
  } catch {
    // Ignore
  }
}

export function getStoredSettings(): GameSettings {
  try {
    const raw = localStorage.getItem(SETTINGS_KEY);
    if (!raw) return DEFAULT_SETTINGS;
    return { ...DEFAULT_SETTINGS, ...JSON.parse(raw) };
  } catch {
    return DEFAULT_SETTINGS;
  }
}

export function saveStoredSettings(settings: GameSettings): void {
  try {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
  } catch {
    // Ignore
  }
}

export function getStoredSave(): SavedGameData | null {
  try {
    const raw = localStorage.getItem(SAVE_KEY);
    if (!raw) return null;
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function saveActiveGame(data: SavedGameData): void {
  try {
    localStorage.setItem(SAVE_KEY, JSON.stringify(data));
  } catch {
    // Ignore
  }
}

export function clearActiveGame(): void {
  try {
    localStorage.removeItem(SAVE_KEY);
  } catch {
    // Ignore
  }
}

export function getStoredHistory(): GameHistoryEntry[] {
  try {
    const raw = localStorage.getItem(HISTORY_KEY);
    if (!raw) return [];
    return JSON.parse(raw);
  } catch {
    return [];
  }
}

export function addStoredHistory(entry: GameHistoryEntry): void {
  try {
    const history = getStoredHistory();
    history.unshift(entry);
    if (history.length > 50) history.pop();
    localStorage.setItem(HISTORY_KEY, JSON.stringify(history));
  } catch {
    // Ignore
  }
}

export function clearStoredHistory(): void {
  try {
    localStorage.removeItem(HISTORY_KEY);
  } catch {
    // Ignore
  }
}

export function getDailyChallengeSubmission(date: string): { playerScore: number; aiScore: number; submittedAt: string } | null {
  try {
    const raw = localStorage.getItem(DAILY_RESULTS_KEY);
    if (!raw) return null;
    const map = JSON.parse(raw);
    return map[date] || null;
  } catch {
    return null;
  }
}

export function saveDailyChallengeSubmission(date: string, playerScore: number, aiScore: number): void {
  try {
    const raw = localStorage.getItem(DAILY_RESULTS_KEY);
    const map = raw ? JSON.parse(raw) : {};
    map[date] = { playerScore, aiScore, submittedAt: new Date().toISOString() };
    localStorage.setItem(DAILY_RESULTS_KEY, JSON.stringify(map));
  } catch {
    // Ignore
  }
}
