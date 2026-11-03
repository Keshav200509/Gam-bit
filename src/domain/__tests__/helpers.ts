import { Board, Cell, CellModifier, CellOwner } from '../../types/game';

export function makeBoard(fill: Partial<Cell> = {}): Board {
  return {
    cells: Array.from({ length: 5 }, () =>
      Array.from({ length: 5 }, () => ({ value: 1, owner: 'NONE' as CellOwner, fortify: 0, modifier: 'None' as CellModifier, ...fill }))
    )
  };
}

export function setCell(board: Board, row: number, col: number, patch: Partial<Cell>): Board {
  board.cells[row][col] = { ...board.cells[row][col], ...patch };
  return board;
}
