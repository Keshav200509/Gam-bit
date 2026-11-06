import { describe, expect, it } from 'vitest';
import { calculateIncome } from '../incomeCalculator';
import { makeBoard, setCell } from './helpers';

describe('calculateIncome', () => {
  it('sums owned cell values per side and ignores neutral cells', () => {
    const b = makeBoard();
    setCell(b, 0, 0, { owner: 'PLAYER', value: 3 });
    setCell(b, 0, 1, { owner: 'PLAYER', value: 2 });
    setCell(b, 4, 4, { owner: 'AI', value: 1 });
    expect(calculateIncome(b)).toEqual({ playerIncome: 5, aiIncome: 1 });
  });

  it('Golden Cells pay double', () => {
    const b = setCell(makeBoard(), 1, 1, { owner: 'PLAYER', value: 4, modifier: 'GoldenCell' });
    expect(calculateIncome(b).playerIncome).toBe(8);
  });

  it('Volatile Cells pay double only in round 12', () => {
    const b = setCell(makeBoard(), 1, 1, { owner: 'AI', value: 3, modifier: 'Volatile' });
    expect(calculateIncome(b, 11).aiIncome).toBe(3);
    expect(calculateIncome(b, 12).aiIncome).toBe(6);
  });

  it('applies the round multiplier (Double Stakes)', () => {
    const b = setCell(makeBoard(), 0, 0, { owner: 'PLAYER', value: 2 });
    expect(calculateIncome(b, 11, 2).playerIncome).toBe(4);
  });
});
