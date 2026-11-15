import { describe, expect, it } from 'vitest';
import {
  MAX_CELL_VALUE,
  MAX_POINTS_PER_TURN,
  MAX_TURNS,
  verifyScoreSubmission
} from '../scoreVerification';

const USER = '3f8c1b2e-4a5d-4e6f-8a9b-0c1d2e3f4a5b';
const body = (patch: Record<string, unknown> = {}) => ({
  userId: USER,
  score: 120,
  turns: 12,
  gameMode: 'ASCENDENCY',
  ...patch
});

describe('verifyScoreSubmission', () => {
  it('accepts a plausible submission and narrows the payload', () => {
    const result = verifyScoreSubmission(body());
    expect(result.ok).toBe(true);
    if (result.ok) expect(result.submission).toEqual({ userId: USER, score: 120, turns: 12, gameMode: 'ASCENDENCY' });
  });

  it('trims surrounding whitespace from gameMode', () => {
    const result = verifyScoreSubmission(body({ gameMode: '  OBLIVION  ' }));
    expect(result.ok).toBe(true);
    if (result.ok) expect(result.submission.gameMode).toBe('OBLIVION');
  });

  it('rejects a non-object body', () => {
    for (const bad of [null, undefined, 'x', 42, []]) {
      expect(verifyScoreSubmission(bad).ok).toBe(false);
    }
  });

  it('rejects a missing or malformed userId', () => {
    expect(verifyScoreSubmission(body({ userId: undefined })).ok).toBe(false);
    expect(verifyScoreSubmission(body({ userId: 'not-a-uuid' })).ok).toBe(false);
    expect(verifyScoreSubmission(body({ userId: 123 })).ok).toBe(false);
  });

  it('rejects score <= 0', () => {
    for (const score of [0, -1, -500]) {
      const result = verifyScoreSubmission(body({ score }));
      expect(result.ok).toBe(false);
      if (!result.ok) expect(result.reason).toMatch(/score must be a positive integer/);
    }
  });

  it('rejects turns <= 0', () => {
    for (const turns of [0, -3]) {
      const result = verifyScoreSubmission(body({ turns }));
      expect(result.ok).toBe(false);
      if (!result.ok) expect(result.reason).toMatch(/turns must be a positive integer/);
    }
  });

  it('rejects non-integer and non-finite numbers', () => {
    expect(verifyScoreSubmission(body({ score: 12.5 })).ok).toBe(false);
    expect(verifyScoreSubmission(body({ score: Infinity })).ok).toBe(false);
    expect(verifyScoreSubmission(body({ score: NaN })).ok).toBe(false);
    expect(verifyScoreSubmission(body({ turns: 1.2 })).ok).toBe(false);
  });

  it('rejects turns beyond the match length', () => {
    const result = verifyScoreSubmission(body({ turns: MAX_TURNS + 1, score: 10 }));
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.reason).toMatch(/cannot exceed/);
  });

  it('rejects an empty gameMode', () => {
    expect(verifyScoreSubmission(body({ gameMode: '' })).ok).toBe(false);
    expect(verifyScoreSubmission(body({ gameMode: '   ' })).ok).toBe(false);
    expect(verifyScoreSubmission(body({ gameMode: 'x'.repeat(33) })).ok).toBe(false);
  });

  it('accepts a score exactly at the ceiling', () => {
    const score = MAX_TURNS * MAX_POINTS_PER_TURN;
    expect(verifyScoreSubmission(body({ score, turns: MAX_TURNS })).ok).toBe(true);
  });

  it('rejects a score one point above the ceiling', () => {
    const score = MAX_TURNS * MAX_POINTS_PER_TURN + 1;
    const result = verifyScoreSubmission(body({ score, turns: MAX_TURNS }));
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.reason).toMatch(/exceeds the theoretical maximum/);
  });

  it('scales the ceiling with the number of turns', () => {
    // One turn cannot exceed a single turn's worth of income.
    expect(verifyScoreSubmission(body({ turns: 1, score: MAX_POINTS_PER_TURN })).ok).toBe(true);
    expect(verifyScoreSubmission(body({ turns: 1, score: MAX_POINTS_PER_TURN + 1 })).ok).toBe(false);
  });

  it('derives the ceiling from the board and income model', () => {
    expect(MAX_POINTS_PER_TURN).toBe(400);
    expect(MAX_CELL_VALUE).toBe(4);
  });
});
