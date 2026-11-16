import { Board, ClashResult, Position, Token } from '../types/game';
import { resolveClash } from './clashResolver';
import { calculateIncome } from './incomeCalculator';
import { posKey } from './positionUtils';

/**
 * Peer-to-peer duel synchronisation primitives.
 *
 * Everything here is pure and deterministic: both clients run the same engine
 * (`resolveClash` / `calculateIncome`) against the same inputs and must reach the
 * same board, so game state is never shipped over the wire — only moves are.
 */

export const PVP_MOVE_EVENT = 'GAME_MOVE';

export type PvpRole = 'P1' | 'P2';

export type PvpConnectionStatus = 'connecting' | 'connected' | 'error' | 'closed';

/** One player's committed orders for a single round. */
export interface GameMovePayload {
  round: number;
  placements: Record<string, Token>;
  locks: Position[];
}

export interface PvpMessage {
  type: typeof PVP_MOVE_EVENT;
  payload: GameMovePayload;
}

export interface PvpPresence {
  peerId: string;
  name: string;
}

/** A stable per-tab identity; not a secret, so a random string is enough. */
export function createPeerId(): string {
  const rand = Math.random().toString(36).slice(2, 10);
  return `peer_${Date.now().toString(36)}_${rand}`;
}

/** Room codes are compared case-insensitively, so normalise before use. */
export function normalizeRoomCode(raw: string): string {
  return raw.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 24);
}

/** The Realtime topic both clients subscribe to: `room:[code]`. */
export function roomChannelName(code: string): string {
  return `room:${normalizeRoomCode(code)}`;
}

/** FNV-1a, so a room code always maps to the same seed on both clients. */
function hashString(value: string): number {
  let hash = 0x811c9dc5;
  for (let i = 0; i < value.length; i++) {
    hash ^= value.charCodeAt(i);
    hash = Math.imul(hash, 0x01000193);
  }
  return hash >>> 0;
}

/**
 * Both clients seed `generateBoard` from the room code, so the duel starts on an
 * identical board without the board ever being transmitted.
 */
export function seededRandomFromRoomCode(code: string): () => number {
  let state = hashString(normalizeRoomCode(code));
  return () => {
    state |= 0;
    state = (state + 0x6d2b79f5) | 0;
    let t = Math.imul(state ^ (state >>> 15), 1 | state);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

/**
 * Roles are assigned by sorting the peer ids both clients can see, so the result
 * is identical on each side with no negotiation. Callers should only rely on this
 * once both players are present, otherwise the provisional role can change.
 */
export function deriveRoles(
  selfPeerId: string,
  peerIds: string[]
): { self: PvpRole; opponent: PvpRole } | null {
  const all = Array.from(new Set([selfPeerId, ...peerIds])).sort();
  const index = all.indexOf(selfPeerId);
  if (index < 0 || all.length < 2) return null;
  return index === 0 ? { self: 'P1', opponent: 'P2' } : { self: 'P2', opponent: 'P1' };
}

/**
 * Who must have sent an incoming move, given the phase this client is in.
 *
 * Placement is sequential and always P1 first, so the phase identifies the
 * sender without any extra handshake: while P1 is placing, only P1 can commit.
 * Returns null when a move is not expected (resolving, income, game over).
 */
export function senderRoleForPhase(phase: string): PvpRole | null {
  if (phase === 'P1_PLACING') return 'P1';
  if (phase === 'P2_PLACING') return 'P2';
  return null;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function isToken(value: unknown): value is Token {
  return (
    isRecord(value) &&
    typeof value.value === 'number' &&
    Number.isFinite(value.value) &&
    typeof value.isLocked === 'boolean'
  );
}

function isPosition(value: unknown): value is Position {
  return (
    isRecord(value) &&
    typeof value.row === 'number' &&
    Number.isInteger(value.row) &&
    value.row >= 0 &&
    value.row < 5 &&
    typeof value.col === 'number' &&
    Number.isInteger(value.col) &&
    value.col >= 0 &&
    value.col < 5
  );
}

function isPlacements(value: unknown): value is Record<string, Token> {
  if (!isRecord(value)) return false;
  return Object.entries(value).every(([key, token]) => /^\d,\d$/.test(key) && isToken(token));
}

/** Incoming payloads are untrusted: a malformed move must never reach the engine. */
export function isGameMovePayload(value: unknown): value is GameMovePayload {
  return (
    isRecord(value) &&
    typeof value.round === 'number' &&
    Number.isInteger(value.round) &&
    value.round >= 1 &&
    value.round <= 12 &&
    isPlacements(value.placements) &&
    Array.isArray(value.locks) &&
    value.locks.every(isPosition)
  );
}

export function isPvpMessage(value: unknown): value is PvpMessage {
  return isRecord(value) && value.type === PVP_MOVE_EVENT && isGameMovePayload(value.payload);
}

/** Flattens a Realtime presence snapshot into peers, excluding this client. */
export function parsePresenceState(state: unknown, selfPeerId: string): PvpPresence[] {
  if (!isRecord(state)) return [];
  const peers = new Map<string, PvpPresence>();
  for (const [key, presences] of Object.entries(state)) {
    if (!Array.isArray(presences)) continue;
    for (const presence of presences) {
      const peerId = isRecord(presence) && typeof presence.peerId === 'string' ? presence.peerId : key;
      if (peerId === selfPeerId) continue;
      const name =
        isRecord(presence) && typeof presence.name === 'string' && presence.name ? presence.name : 'Opponent';
      peers.set(peerId, { peerId, name });
    }
  }
  return Array.from(peers.values()).sort((a, b) => a.peerId.localeCompare(b.peerId));
}

/** A single clash as it is revealed, with the board state that follows it. */
export interface RoundStep {
  position: Position;
  result: ClashResult;
  board: Board;
}

export interface RoundResolution {
  steps: RoundStep[];
  board: Board;
  results: Record<string, ClashResult>;
  playerIncome: number;
  aiIncome: number;
}

function ownerFor(result: ClashResult): 'PLAYER' | 'AI' | 'NONE' {
  if (result.type === 'PlayerWins' || result.type === 'PlayerUncontested' || result.type === 'PlayerDefends') {
    return 'PLAYER';
  }
  if (result.type === 'AIWins' || result.type === 'AIUncontested' || result.type === 'AIDefends') {
    return 'AI';
  }
  return 'NONE';
}

/**
 * Resolves one round locally. Positions are visited row-major so that both
 * clients capture cells in the same order (capturing changes adjacency bonuses,
 * so the order is part of the result, not an implementation detail).
 */
export function resolveRound(
  board: Board,
  round: number,
  p1Placements: Record<string, Token>,
  p1Locks: Position[],
  p2Placements: Record<string, Token>,
  p2Locks: Position[]
): RoundResolution {
  const steps: RoundStep[] = [];
  const results: Record<string, ClashResult> = {};
  let currentBoard = board;

  for (let row = 0; row < 5; row++) {
    for (let col = 0; col < 5; col++) {
      const key = `${row},${col}`;
      if (!p1Placements[key] && !p2Placements[key]) continue;

      const position = { row, col };
      const result = resolveClash(position, p1Placements, p2Placements, currentBoard, p1Locks, p2Locks);
      results[key] = result;

      const owner = ownerFor(result);
      currentBoard = {
        cells: currentBoard.cells.map((rowArr, rIdx) =>
          rowArr.map((cell, cIdx) => (rIdx === row && cIdx === col ? { ...cell, owner } : cell))
        )
      };

      steps.push({ position, result, board: currentBoard });
    }
  }

  const { playerIncome, aiIncome } = calculateIncome(currentBoard, round);
  return { steps, board: currentBoard, results, playerIncome, aiIncome };
}
