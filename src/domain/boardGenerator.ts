import { Board, Cell, Position } from '../types/game';
import { getNeighbors } from './positionUtils';

export function generateBoard(seedRandom?: () => number): Board {
  const rand = seedRandom || Math.random;

  for (let attempt = 0; attempt < 100; attempt++) {
    const gridValues: number[][] = Array.from({ length: 5 }, () => Array(5).fill(1));

    // 1. Generate 3-point cluster of size 3
    const cluster3Keys = new Set<string>();
    const start3: Position = {
      row: Math.floor(rand() * 5),
      col: Math.floor(rand() * 5)
    };
    cluster3Keys.add(`${start3.row},${start3.col}`);

    while (cluster3Keys.size < 3) {
      const candidates: Position[] = [];
      for (const key of cluster3Keys) {
        const [r, c] = key.split(',').map(Number);
        for (const n of getNeighbors({ row: r, col: c })) {
          const nKey = `${n.row},${n.col}`;
          if (!cluster3Keys.has(nKey) && !candidates.some(c => c.row === n.row && c.col === n.col)) {
            candidates.push(n);
          }
        }
      }
      if (candidates.length === 0) break;
      const next = candidates[Math.floor(rand() * candidates.length)];
      cluster3Keys.add(`${next.row},${next.col}`);
    }

    if (cluster3Keys.size < 3) continue;

    // 2. Generate 2-point region of size 5
    const region2Keys = new Set<string>();
    const validStart2: Position[] = [];
    for (let r = 0; r < 5; r++) {
      for (let c = 0; c < 5; c++) {
        if (!cluster3Keys.has(`${r},${c}`)) {
          validStart2.push({ row: r, col: c });
        }
      }
    }
    if (validStart2.length === 0) continue;

    const start2 = validStart2[Math.floor(rand() * validStart2.length)];
    region2Keys.add(`${start2.row},${start2.col}`);

    let failedToGrow = false;
    while (region2Keys.size < 5) {
      const candidates: Position[] = [];
      for (const key of region2Keys) {
        const [r, c] = key.split(',').map(Number);
        for (const n of getNeighbors({ row: r, col: c })) {
          const nKey = `${n.row},${n.col}`;
          if (!cluster3Keys.has(nKey) && !region2Keys.has(nKey) && !candidates.some(c => c.row === n.row && c.col === n.col)) {
            candidates.push(n);
          }
        }
      }
      if (candidates.length === 0) {
        failedToGrow = true;
        break;
      }
      const next = candidates[Math.floor(rand() * candidates.length)];
      region2Keys.add(`${next.row},${next.col}`);
    }

    if (failedToGrow || region2Keys.size < 5) continue;

    // Apply values
    for (const key of cluster3Keys) {
      const [r, c] = key.split(',').map(Number);
      gridValues[r][c] = 3;
    }
    for (const key of region2Keys) {
      const [r, c] = key.split(',').map(Number);
      gridValues[r][c] = 2;
    }

    // 3. Rest are 1-point with scattered 2s (25% chance)
    for (let r = 0; r < 5; r++) {
      for (let c = 0; c < 5; c++) {
        const key = `${r},${c}`;
        if (!cluster3Keys.has(key) && !region2Keys.has(key)) {
          if (rand() < 0.25) {
            gridValues[r][c] = 2;
          } else {
            gridValues[r][c] = 1;
          }
        }
      }
    }

    const cells: Cell[][] = Array.from({ length: 5 }, (_, r) =>
      Array.from({ length: 5 }, (_, c) => ({
        value: gridValues[r][c],
        owner: 'NONE',
        fortify: 0,
        modifier: 'None'
      }))
    );

    return { cells };
  }

  // Fallback
  const fallbackCells: Cell[][] = Array.from({ length: 5 }, (_, r) =>
    Array.from({ length: 5 }, (_, c) => {
      let val = 1;
      if (r === 2 && c >= 1 && c <= 3) val = 3;
      else if (r === 0) val = 2;
      return {
        value: val,
        owner: 'NONE',
        fortify: 0,
        modifier: 'None'
      };
    })
  );

  return { cells: fallbackCells };
}
