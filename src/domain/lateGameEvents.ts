import { Board, LateGameEvent, Position } from '../types/game';

export function applyLateGameEvents(
  board: Board,
  roundNumber: number,
  playerLocks: Position[],
  aiLocks: Position[],
  seedRandom?: () => number
): LateGameEvent {
  const rand = seedRandom || Math.random;

  switch (roundNumber) {
    case 8: {
      const neutralCells: Position[] = [];
      for (let r = 0; r < 5; r++) {
        for (let c = 0; c < 5; c++) {
          if (board.cells[r][c].owner === 'NONE') {
            neutralCells.push({ row: r, col: c });
          }
        }
      }

      const targetPos: Position =
        neutralCells.length > 0
          ? neutralCells[Math.floor(rand() * neutralCells.length)]
          : { row: Math.floor(rand() * 5), col: Math.floor(rand() * 5) };

      const newCells = board.cells.map((row, r) =>
        row.map((cell, c) => {
          if (r === targetPos.row && c === targetPos.col) {
            return {
              ...cell,
              value: 4,
              modifier: 'GoldenCell' as const
            };
          }
          return cell;
        })
      );

      return {
        type: 'GOLDEN_CELL',
        modifiedBoard: { cells: newCells },
        modifiedPlayerLocks: playerLocks,
        modifiedAiLocks: aiLocks,
        incomeMultiplier: 1,
        message: `GOLDEN CELL APPEARED at (${targetPos.row},${targetPos.col}) — worth double points`
      };
    }

    case 10: {
      const ownedCells: Position[] = [];
      for (let r = 0; r < 5; r++) {
        for (let c = 0; c < 5; c++) {
          if (board.cells[r][c].owner !== 'NONE') {
            ownedCells.push({ row: r, col: c });
          }
        }
      }

      const targetPos: Position =
        ownedCells.length > 0
          ? ownedCells[Math.floor(rand() * ownedCells.length)]
          : { row: Math.floor(rand() * 5), col: Math.floor(rand() * 5) };

      const newCells = board.cells.map((row, r) =>
        row.map((cell, c) => {
          if (r === targetPos.row && c === targetPos.col) {
            return {
              ...cell,
              modifier: 'Volatile' as const
            };
          }
          return cell;
        })
      );

      return {
        type: 'VOLATILE',
        modifiedBoard: { cells: newCells },
        modifiedPlayerLocks: playerLocks,
        modifiedAiLocks: aiLocks,
        incomeMultiplier: 1,
        message: `VOLATILE CELL at (${targetPos.row},${targetPos.col}) — 2× points if held at game end`
      };
    }

    case 11:
      return {
        type: 'DOUBLE_STAKES',
        modifiedBoard: board,
        modifiedPlayerLocks: playerLocks,
        modifiedAiLocks: aiLocks,
        incomeMultiplier: 2,
        message: 'DOUBLE STAKES — all income doubled this round'
      };

    case 12:
      return {
        type: 'FINAL_GAMBIT',
        modifiedBoard: board,
        modifiedPlayerLocks: [],
        modifiedAiLocks: [],
        incomeMultiplier: 1,
        message: 'FINAL GAMBIT — all locks removed'
      };

    default:
      return {
        type: 'NONE',
        modifiedBoard: board,
        modifiedPlayerLocks: playerLocks,
        modifiedAiLocks: aiLocks,
        incomeMultiplier: 1,
        message: ''
      };
  }
}
