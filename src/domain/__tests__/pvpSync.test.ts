import { describe, expect, it } from 'vitest';
import { Board, Token } from '../../types/game';
import { generateBoard } from '../boardGenerator';
import { calculateIncome } from '../incomeCalculator';
import {
  PVP_MOVE_EVENT,
  deriveRoles,
  isGameMovePayload,
  isPvpMessage,
  normalizeRoomCode,
  parsePresenceState,
  resolveRound,
  roomChannelName,
  seededRandomFromRoomCode,
  senderRoleForPhase
} from '../pvpSync';

function emptyBoard(): Board {
  return {
    cells: Array.from({ length: 5 }, () =>
      Array.from({ length: 5 }, () => ({ value: 1, owner: 'NONE' as const, fortify: 0, modifier: 'None' as const }))
    )
  };
}

const token = (value: number, isLocked = false): Token => ({ value, isLocked });

describe('room codes', () => {
  it('normalises case and strips punctuation', () => {
    expect(normalizeRoomCode('gambit-1a2b')).toBe('GAMBIT1A2B');
    expect(normalizeRoomCode('  ab_12! ')).toBe('AB12');
  });

  it('builds the room:[code] topic', () => {
    expect(roomChannelName('gambit-1a2b')).toBe('room:GAMBIT1A2B');
  });

  it('caps absurdly long codes', () => {
    expect(normalizeRoomCode('A'.repeat(100))).toHaveLength(24);
  });
});

describe('seeded boards', () => {
  it('produces the same board on both clients for a room code', () => {
    const a = generateBoard(seededRandomFromRoomCode('room:GAMBIT7Q'));
    const b = generateBoard(seededRandomFromRoomCode('room:gambit7q'));
    expect(a).toEqual(b);
  });

  it('differs between room codes', () => {
    const a = generateBoard(seededRandomFromRoomCode('ROOM1'));
    const b = generateBoard(seededRandomFromRoomCode('ROOM2'));
    expect(a).not.toEqual(b);
  });
});

describe('role assignment', () => {
  it('is symmetric: exactly one side is P1', () => {
    const a = deriveRoles('peer_a', ['peer_b']);
    const b = deriveRoles('peer_b', ['peer_a']);
    expect(a).toEqual({ self: 'P1', opponent: 'P2' });
    expect(b).toEqual({ self: 'P2', opponent: 'P1' });
  });

  it('is order-independent', () => {
    expect(deriveRoles('peer_b', ['peer_a', 'peer_c'])).toEqual(deriveRoles('peer_b', ['peer_c', 'peer_a']));
  });

  it('returns null until an opponent is present', () => {
    expect(deriveRoles('peer_a', [])).toBeNull();
  });
});

describe('payload validation', () => {
  const valid = {
    type: PVP_MOVE_EVENT,
    payload: { round: 3, placements: { '1,2': token(4, true) }, locks: [{ row: 1, col: 2 }] }
  };

  it('accepts a well-formed move', () => {
    expect(isPvpMessage(valid)).toBe(true);
  });

  it('rejects unknown event types', () => {
    expect(isPvpMessage({ ...valid, type: 'SOMETHING_ELSE' })).toBe(false);
  });

  it('rejects out-of-range rounds', () => {
    expect(isGameMovePayload({ ...valid.payload, round: 0 })).toBe(false);
    expect(isGameMovePayload({ ...valid.payload, round: 13 })).toBe(false);
    expect(isGameMovePayload({ ...valid.payload, round: 1.5 })).toBe(false);
  });

  it('rejects malformed tokens and cells', () => {
    expect(isGameMovePayload({ ...valid.payload, placements: { '1,2': { value: '4' } } })).toBe(false);
    expect(isGameMovePayload({ ...valid.payload, placements: { 'nope': token(1) } })).toBe(false);
    expect(isGameMovePayload({ ...valid.payload, locks: [{ row: 9, col: 0 }] })).toBe(false);
    expect(isGameMovePayload({ ...valid.payload, locks: [{ row: 0 }] })).toBe(false);
  });

  it('rejects non-objects and null', () => {
    expect(isPvpMessage(null)).toBe(false);
    expect(isPvpMessage('GAME_MOVE')).toBe(false);
    expect(isGameMovePayload(undefined)).toBe(false);
  });
});

describe('presence parsing', () => {
  it('flattens peers and excludes this client', () => {
    const state = {
      peer_self: [{ peerId: 'peer_self', name: 'Me' }],
      peer_other: [{ peerId: 'peer_other', name: 'Rival' }]
    };
    expect(parsePresenceState(state, 'peer_self')).toEqual([{ peerId: 'peer_other', name: 'Rival' }]);
  });

  it('tolerates a missing name and junk entries', () => {
    const state = { peer_other: [{ peerId: 'peer_other' }], broken: 'not-an-array', peer_third: [null] };
    expect(parsePresenceState(state, 'peer_self')).toEqual([
      { peerId: 'peer_other', name: 'Opponent' },
      { peerId: 'peer_third', name: 'Opponent' }
    ]);
  });

  it('returns an empty list for a non-object', () => {
    expect(parsePresenceState(null, 'peer_self')).toEqual([]);
  });
});

describe('resolveRound', () => {
  it('awards an uncontested cell to the placing side', () => {
    const board = emptyBoard();
    const out = resolveRound(board, 1, { '0,0': token(3) }, [], {}, []);
    expect(out.results['0,0'].type).toBe('PlayerUncontested');
    expect(out.board.cells[0][0].owner).toBe('PLAYER');
  });

  it('does not mutate the board it was given', () => {
    const board = emptyBoard();
    resolveRound(board, 1, { '0,0': token(3) }, [], {}, []);
    expect(board.cells[0][0].owner).toBe('NONE');
  });

  it('is deterministic, so both clients reach the same board', () => {
    const board = emptyBoard();
    const p1 = { '0,0': token(5, true), '2,2': token(2) };
    const p2 = { '0,0': token(3), '1,1': token(4) };
    const first = resolveRound(board, 1, p1, [{ row: 0, col: 0 }], p2, []);
    const second = resolveRound(board, 1, p1, [{ row: 0, col: 0 }], p2, []);
    expect(first).toEqual(second);
  });

  it('visits cells in row-major order', () => {
    const board = emptyBoard();
    const out = resolveRound(board, 1, { '2,1': token(1), '0,3': token(1) }, [], {}, []);
    expect(out.steps.map(step => `${step.position.row},${step.position.col}`)).toEqual(['0,3', '2,1']);
  });

  it('reports income consistent with calculateIncome', () => {
    const board = emptyBoard();
    const p1 = { '0,0': token(3), '1,1': token(3) };
    const p2 = { '4,4': token(2) };
    const out = resolveRound(board, 1, p1, [], p2, []);
    const expected = calculateIncome(out.board, 1);
    expect(out.playerIncome).toBe(expected.playerIncome);
    expect(out.aiIncome).toBe(expected.aiIncome);
    expect(out.playerIncome).toBe(2);
    expect(out.aiIncome).toBe(1);
  });

  it('gives the clash to the stronger side', () => {
    const board = emptyBoard();
    const out = resolveRound(board, 1, { '2,2': token(5) }, [], { '2,2': token(1) }, []);
    expect(out.results['2,2'].type).toBe('PlayerWins');
    expect(out.board.cells[2][2].owner).toBe('PLAYER');
  });

  it('maps a phase to the only peer that can be sending', () => {
    expect(senderRoleForPhase('P1_PLACING')).toBe('P1');
    expect(senderRoleForPhase('P2_PLACING')).toBe('P2');
    expect(senderRoleForPhase('RESOLVING')).toBeNull();
    expect(senderRoleForPhase('GAME_OVER')).toBeNull();
  });
});

describe('peer-to-peer convergence', () => {
  /**
   * Both clients seed from the room code and only exchange moves, so they must
   * finish every round with byte-identical boards and scores.
   */
  it('keeps both clients on the same board after several rounds', () => {
    const code = 'room:GAMBIT7Q';
    let boardA = generateBoard(seededRandomFromRoomCode(code));
    let boardB = generateBoard(seededRandomFromRoomCode(code));
    let scoreA = { player: 0, ai: 0 };
    let scoreB = { player: 0, ai: 0 };

    const rounds: Array<{
      p1: Record<string, Token>;
      p1l: { row: number; col: number }[];
      p2: Record<string, Token>;
      p2l: { row: number; col: number }[];
    }> = [
      { p1: { '0,0': token(3) }, p1l: [], p2: { '0,0': token(1), '4,4': token(2) }, p2l: [] },
      { p1: { '1,1': token(5, true) }, p1l: [{ row: 1, col: 1 }], p2: { '1,1': token(4) }, p2l: [] },
      { p1: { '2,2': token(2) }, p1l: [], p2: { '2,2': token(2) }, p2l: [] }
    ];

    rounds.forEach((move, index) => {
      const round = index + 1;
      const outA = resolveRound(boardA, round, move.p1, move.p1l, move.p2, move.p2l);
      const outB = resolveRound(boardB, round, move.p1, move.p1l, move.p2, move.p2l);
      boardA = outA.board;
      boardB = outB.board;
      scoreA = { player: scoreA.player + outA.playerIncome, ai: scoreA.ai + outA.aiIncome };
      scoreB = { player: scoreB.player + outB.playerIncome, ai: scoreB.ai + outB.aiIncome };
      expect(outA).toEqual(outB);
    });

    expect(boardA).toEqual(boardB);
    expect(scoreA).toEqual(scoreB);
  });
});
