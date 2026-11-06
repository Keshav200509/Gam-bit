import { describe, expect, it } from 'vitest';
import { generateBoard } from '../boardGenerator';
import { createSeededRandom, generateDailyChallenge } from '../dailyChallenge';

describe('generateBoard', () => {
  it('produces a 5x5 neutral board with values in 1..3', () => {
    for (let seed = 1; seed <= 50; seed++) {
      const b = generateBoard(createSeededRandom(seed));
      expect(b.cells).toHaveLength(5);
      for (const row of b.cells) {
        expect(row).toHaveLength(5);
        for (const cell of row) {
          expect(cell.owner).toBe('NONE');
          expect(cell.value).toBeGreaterThanOrEqual(1);
          expect(cell.value).toBeLessThanOrEqual(3);
        }
      }
    }
  });

  it('is deterministic for the same seed', () => {
    expect(generateBoard(createSeededRandom(42))).toEqual(generateBoard(createSeededRandom(42)));
  });
});

describe('daily challenge', () => {
  it('gives every player the same config for the same date', () => {
    expect(generateDailyChallenge('2026-11-04')).toEqual(generateDailyChallenge('2026-11-04'));
  });

  it('always uses LEVEL_2 and a valid arena', () => {
    for (let d = 1; d <= 28; d++) {
      const cfg = generateDailyChallenge(`2026-02-${String(d).padStart(2, '0')}`);
      expect(cfg.level).toBe('LEVEL_2');
      expect(['ASCENDENCY', 'CONFRONTATION', 'OBLIVION']).toContain(cfg.arena);
    }
  });

  it('seeded RNG is deterministic and in [0, 1)', () => {
    const a = createSeededRandom(7), b = createSeededRandom(7);
    for (let i = 0; i < 100; i++) {
      const x = a();
      expect(x).toBe(b());
      expect(x).toBeGreaterThanOrEqual(0);
      expect(x).toBeLessThan(1);
    }
  });
});
