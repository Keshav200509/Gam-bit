import { describe, expect, it } from 'vitest';
import { calculateStrength, resolveClash } from '../clashResolver';
import { makeBoard, setCell } from './helpers';

const tok = (value: number, isLocked = false) => ({ value, isLocked });

describe('calculateStrength', () => {
  it('is just the token value on an empty neutral board', () => {
    expect(calculateStrength({ row: 2, col: 2 }, tok(3), 'PLAYER', makeBoard(), false)).toBe(3);
  });

  it('adds +1 per adjacent owned cell, fortify on own cell, and +2 for a lock', () => {
    const b = makeBoard();
    setCell(b, 1, 2, { owner: 'PLAYER' });
    setCell(b, 3, 2, { owner: 'PLAYER' });
    setCell(b, 2, 2, { owner: 'PLAYER', fortify: 2 });
    // 3 token + 2 adjacency + 2 fortify + 2 lock
    expect(calculateStrength({ row: 2, col: 2 }, tok(3), 'PLAYER', b, true)).toBe(9);
  });

  it('ignores fortify when attacking an opponent cell', () => {
    const b = setCell(makeBoard(), 2, 2, { owner: 'AI', fortify: 3 });
    expect(calculateStrength({ row: 2, col: 2 }, tok(2), 'PLAYER', b, false)).toBe(2);
  });

  it('Volatile cells cancel adjacency and fortify', () => {
    const b = makeBoard();
    setCell(b, 1, 2, { owner: 'PLAYER' });
    setCell(b, 2, 2, { owner: 'PLAYER', fortify: 2, modifier: 'Volatile' });
    expect(calculateStrength({ row: 2, col: 2 }, tok(4), 'PLAYER', b, false)).toBe(4);
  });

  it('Battleground gives +1 only to the defender', () => {
    const b = setCell(makeBoard(), 0, 0, { owner: 'AI', modifier: 'Battleground' });
    expect(calculateStrength({ row: 0, col: 0 }, tok(2), 'AI', b, false)).toBe(3);
    expect(calculateStrength({ row: 0, col: 0 }, tok(2), 'PLAYER', b, false)).toBe(2);
  });
});

describe('resolveClash', () => {
  const pos = { row: 2, col: 2 };

  it('uncontested placements on neutral cells', () => {
    const b = makeBoard();
    expect(resolveClash(pos, { '2,2': tok(1) }, {}, b, [], []).type).toBe('PlayerUncontested');
    expect(resolveClash(pos, {}, { '2,2': tok(1) }, b, [], []).type).toBe('AIUncontested');
  });

  it('stronger token wins, equal strength ties', () => {
    const b = makeBoard();
    expect(resolveClash(pos, { '2,2': tok(5) }, { '2,2': tok(3) }, b, [], []).type).toBe('PlayerWins');
    expect(resolveClash(pos, { '2,2': tok(2) }, { '2,2': tok(4) }, b, [], []).type).toBe('AIWins');
    expect(resolveClash(pos, { '2,2': tok(3) }, { '2,2': tok(3) }, b, [], []).type).toBe('Tie');
  });

  it('a lock from the locks list adds +2 and can flip the result', () => {
    const r = resolveClash(pos, { '2,2': tok(3) }, { '2,2': tok(4) }, makeBoard(), [pos], []);
    expect(r).toEqual({ type: 'PlayerWins', playerStrength: 5, aiStrength: 4 });
  });

  it('owner winning their own cell is a defend', () => {
    const b = setCell(makeBoard(), 2, 2, { owner: 'AI' });
    expect(resolveClash(pos, { '2,2': tok(1) }, { '2,2': tok(2) }, b, [], []).type).toBe('AIDefends');
  });
});
