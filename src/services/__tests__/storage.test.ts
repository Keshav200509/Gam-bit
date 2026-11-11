import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  addStoredHistory,
  clearStoredHistory,
  DEFAULT_PROFILE,
  getStoredHistory,
  getStoredProfile,
  LocalStorageAdapter,
  saveStoredProfile,
  storage
} from '../storage';
import { GameHistoryEntry } from '../../types/game';

class MemoryStorage {
  private data = new Map<string, string>();
  getItem(key: string) {
    return this.data.has(key) ? this.data.get(key)! : null;
  }
  setItem(key: string, value: string) {
    this.data.set(key, value);
  }
  removeItem(key: string) {
    this.data.delete(key);
  }
}

const entry = (id: string): GameHistoryEntry => ({
  id,
  date: '1/1/2026',
  arena: 'ASCENDENCY',
  level: 'LEVEL_1',
  playerScore: 10,
  aiScore: 5,
  result: 'WIN',
  roundsPlayed: 12,
  clashesWon: 3,
  clashesLost: 1,
  intelAccuracy: 0.75,
  scoutUses: 1,
  lockUses: 2
});

beforeEach(() => {
  vi.stubGlobal('localStorage', new MemoryStorage());
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('LocalStorageAdapter', () => {
  it('is the default `storage` singleton', () => {
    expect(storage).toBeInstanceOf(LocalStorageAdapter);
  });

  it('returns default stats when nothing is stored', () => {
    expect(new LocalStorageAdapter().getStats()).toEqual(DEFAULT_PROFILE.stats);
  });

  it('saveStats round-trips and keeps the rest of the profile', () => {
    saveStoredProfile({ ...getStoredProfile(), displayName: 'Ada' });
    const adapter = new LocalStorageAdapter();
    adapter.saveStats({ ...adapter.getStats(), totalWins: 7 });

    expect(adapter.getStats().totalWins).toBe(7);
    expect(getStoredProfile().displayName).toBe('Ada');
  });

  it('saveGameResult prepends newest first and caps history at 50', () => {
    const adapter = new LocalStorageAdapter();
    for (let i = 0; i < 55; i++) adapter.saveGameResult(entry(`g${i}`));

    const history = adapter.getHistory();
    expect(history).toHaveLength(50);
    expect(history[0].id).toBe('g54');
    expect(history[49].id).toBe('g5');
  });

  it('clearHistory removes all history', () => {
    const adapter = new LocalStorageAdapter();
    adapter.saveGameResult(entry('g1'));
    adapter.clearHistory();
    expect(adapter.getHistory()).toEqual([]);
  });

  it('returns [] for corrupt history', () => {
    localStorage.setItem('gambit_game_history', '{not json');
    expect(new LocalStorageAdapter().getHistory()).toEqual([]);
  });
});

describe('legacy history functions', () => {
  it('delegate to the storage singleton', () => {
    const save = vi.spyOn(storage, 'saveGameResult');
    const get = vi.spyOn(storage, 'getHistory');

    addStoredHistory(entry('a'));
    expect(getStoredHistory().map(e => e.id)).toEqual(['a']);
    expect(save).toHaveBeenCalledOnce();
    expect(get).toHaveBeenCalled();
  });

  it('clearStoredHistory delegates to the storage singleton', () => {
    const clear = vi.spyOn(storage, 'clearHistory');
    addStoredHistory(entry('a'));
    clearStoredHistory();
    expect(clear).toHaveBeenCalledOnce();
    expect(getStoredHistory()).toEqual([]);
  });
});
