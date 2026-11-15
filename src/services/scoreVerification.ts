/**
 * Score bounds validation for submitted game results.
 *
 * The ceiling is derived from the scoring model in `src/domain`:
 *   - `calculateIncome` awards, per owned cell per round, `cell.value` x2 for a
 *     GoldenCell (and x2 again at round 12 for a Volatile cell);
 *   - the round income multiplier reaches x2 (late-game event, round 11);
 *   - `boardGenerator` seeds values 1..3, and the golden-cell event raises one
 *     cell to 4.
 * So the most a single turn can ever pay out is 25 cells x 4 value x 2 modifier
 * x 2 multiplier = 400, which is the bound used below. It is deliberately
 * generous: the goal is to reject absurd submissions, not to re-simulate a game.
 */

/** 5x5 board. */
export const CELL_COUNT = 25;
/** Highest cell value reachable (golden-cell event raises a cell to 4). */
export const MAX_CELL_VALUE = 4;
/** GoldenCell / round-12 Volatile double a cell's value. */
export const MAX_CELL_MODIFIER_MULTIPLIER = 2;
/** Round income multiplier peaks at 2x. */
export const MAX_ROUND_INCOME_MULTIPLIER = 2;

/** Upper bound on points a single turn can produce. */
export const MAX_POINTS_PER_TURN =
  CELL_COUNT * MAX_CELL_VALUE * MAX_CELL_MODIFIER_MULTIPLIER * MAX_ROUND_INCOME_MULTIPLIER;

/** Longest possible match, in turns (12 rounds, or earlier when all cells are owned). */
export const MAX_TURNS = 12;

export interface ScoreSubmission {
  userId: string;
  score: number;
  turns: number;
  gameMode: string;
}

export type VerificationResult =
  | { ok: true; submission: ScoreSubmission }
  | { ok: false; reason: string };

function isPositiveInt(value: unknown): value is number {
  return typeof value === 'number' && Number.isInteger(value) && value > 0;
}

function isUuid(value: unknown): value is string {
  return (
    typeof value === 'string' &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value)
  );
}

/**
 * Validates an untrusted submission. Returns the narrowed payload on success,
 * or a human-readable reason on failure. Never throws.
 */
export function verifyScoreSubmission(input: unknown): VerificationResult {
  if (typeof input !== 'object' || input === null) {
    return { ok: false, reason: 'Body must be a JSON object' };
  }

  const { userId, score, turns, gameMode } = input as Record<string, unknown>;

  if (!isUuid(userId)) {
    return { ok: false, reason: 'userId must be a UUID' };
  }
  if (typeof score !== 'number' || !Number.isFinite(score) || !isPositiveInt(score)) {
    return { ok: false, reason: 'score must be a positive integer' };
  }
  if (!isPositiveInt(turns)) {
    return { ok: false, reason: 'turns must be a positive integer' };
  }
  if (turns > MAX_TURNS) {
    return { ok: false, reason: `turns cannot exceed ${MAX_TURNS}` };
  }
  if (typeof gameMode !== 'string' || gameMode.trim().length === 0 || gameMode.length > 32) {
    return { ok: false, reason: 'gameMode must be a non-empty string' };
  }

  // Points-per-turn ceiling: score may not outrun what the scoring model can produce.
  const ceiling = turns * MAX_POINTS_PER_TURN;
  if (score > ceiling) {
    return {
      ok: false,
      reason: `score ${score} exceeds the theoretical maximum ${ceiling} for ${turns} turn(s)`
    };
  }

  return { ok: true, submission: { userId, score, turns, gameMode: gameMode.trim() } };
}
