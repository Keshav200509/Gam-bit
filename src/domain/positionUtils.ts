import { Position } from '../types/game';

export function posKey(pos: Position): string {
  return `${pos.row},${pos.col}`;
}

export function parsePosKey(key: string): Position {
  const [row, col] = key.split(',').map(Number);
  return { row, col };
}

export function getNeighbors(pos: Position): Position[] {
  const list: Position[] = [];
  if (pos.row > 0) list.push({ row: pos.row - 1, col: pos.col });
  if (pos.row < 4) list.push({ row: pos.row + 1, col: pos.col });
  if (pos.col > 0) list.push({ row: pos.row, col: pos.col - 1 });
  if (pos.col < 4) list.push({ row: pos.row, col: pos.col + 1 });
  return list;
}

export function getRegionName(row: number, col: number): string {
  if (row === 2 && col === 2) return 'center';
  if (row < 2 && col < 2) return 'northwest';
  if (row < 2 && col > 2) return 'northeast';
  if (row > 2 && col < 2) return 'southwest';
  if (row > 2 && col > 2) return 'southeast';
  if (row < 2) return 'north';
  if (row > 2) return 'south';
  if (col < 2) return 'west';
  return 'east';
}
