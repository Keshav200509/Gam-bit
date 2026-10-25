import { Board, CellOwner, ClashResult, Position, Token } from '../types/game';
import { getNeighbors, posKey } from './positionUtils';

export function calculateStrength(
  position: Position,
  token: Token,
  owner: CellOwner,
  board: Board,
  isLocked: boolean
): number {
  const cell = board.cells[position.row][position.col];
  const isVolatile = cell.modifier === 'Volatile';
  const isBattleground = cell.modifier === 'Battleground';

  // 1. Token base value
  const base = token.value;

  // 2. Adjacency bonus
  const adjacencyBonus = isVolatile
    ? 0
    : getNeighbors(position).filter(
        n => board.cells[n.row][n.col].owner === owner
      ).length;

  // 3. Fortify bonus (if defending own cell)
  const fortifyBonus = isVolatile
    ? 0
    : cell.owner === owner
    ? cell.fortify
    : 0;

  // 4. Lock bonus (+2 if locked)
  const lockBonus = isLocked ? 2 : 0;

  // 5. Battleground bonus (+1 to defender)
  const battlegroundBonus = isBattleground && cell.owner === owner ? 1 : 0;

  return base + adjacencyBonus + fortifyBonus + lockBonus + battlegroundBonus;
}

export function resolveClash(
  position: Position,
  playerPlacements: Record<string, Token>,
  aiPlacements: Record<string, Token>,
  board: Board,
  playerLocks: Position[],
  aiLocks: Position[]
): ClashResult {
  const key = posKey(position);
  const cell = board.cells[position.row][position.col];
  const playerToken = playerPlacements[key];
  const aiToken = aiPlacements[key];

  if (!playerToken && !aiToken) {
    if (cell.owner === 'PLAYER') return { type: 'PlayerDefends' };
    if (cell.owner === 'AI') return { type: 'AIDefends' };
    return { type: 'Tie' };
  }

  if (playerToken && !aiToken) {
    if (cell.owner === 'PLAYER') return { type: 'PlayerDefends' };
    return { type: 'PlayerUncontested' };
  }

  if (aiToken && !playerToken) {
    if (cell.owner === 'AI') return { type: 'AIDefends' };
    return { type: 'AIUncontested' };
  }

  // Both placed -> Clash
  const isPlayerLocked =
    playerToken.isLocked ||
    playerLocks.some(l => l.row === position.row && l.col === position.col);
  const isAiLocked =
    aiToken.isLocked ||
    aiLocks.some(l => l.row === position.row && l.col === position.col);

  const playerStrength = calculateStrength(
    position,
    playerToken,
    'PLAYER',
    board,
    isPlayerLocked
  );

  const aiStrength = calculateStrength(
    position,
    aiToken,
    'AI',
    board,
    isAiLocked
  );

  if (playerStrength > aiStrength) {
    if (cell.owner === 'PLAYER') {
      return { type: 'PlayerDefends', playerStrength, aiStrength };
    }
    return { type: 'PlayerWins', playerStrength, aiStrength };
  }

  if (aiStrength > playerStrength) {
    if (cell.owner === 'AI') {
      return { type: 'AIDefends', playerStrength, aiStrength };
    }
    return { type: 'AIWins', playerStrength, aiStrength };
  }

  return { type: 'Tie', playerStrength, aiStrength };
}
