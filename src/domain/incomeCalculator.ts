import { Board } from '../types/game';

export function calculateIncome(
  board: Board,
  roundNumber: number = 1,
  roundMultiplier: number = 1
): { playerIncome: number; aiIncome: number } {
  let playerIncome = 0;
  let aiIncome = 0;

  for (let r = 0; r < 5; r++) {
    for (let c = 0; c < 5; c++) {
      const cell = board.cells[r][c];
      if (cell.owner === 'NONE') continue;

      let cellIncome = cell.value;

      // Golden Cell generates 2x its value
      if (cell.modifier === 'GoldenCell') {
        cellIncome *= 2;
      }

      // Volatile Cell generates 2x its value ONLY at game end (round 12)
      if (cell.modifier === 'Volatile' && roundNumber === 12) {
        cellIncome *= 2;
      }

      if (cell.owner === 'PLAYER') {
        playerIncome += cellIncome;
      } else if (cell.owner === 'AI') {
        aiIncome += cellIncome;
      }
    }
  }

  return {
    playerIncome: playerIncome * roundMultiplier,
    aiIncome: aiIncome * roundMultiplier
  };
}
