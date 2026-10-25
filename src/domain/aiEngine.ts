import {
  AICapability,
  AICapabilityName,
  AIPlan,
  AIStrategy,
  ArenaConfiguration,
  Board,
  Position,
  Token
} from '../types/game';
import { getNeighbors, posKey } from './positionUtils';

export function getCapabilityFromConfig(
  config: ArenaConfiguration,
  rand: () => number = Math.random
): AICapability {
  const min = config.aiCapabilityMin;
  const max = config.aiCapabilityMax;
  const modifier =
    max >= min
      ? Math.floor(rand() * (max - min + 1)) + min
      : 0;

  let name: AICapabilityName;
  let displayName: string;
  let colorHex: string;

  if (modifier >= 6) {
    name = 'PEAK';
    displayName = 'AI: PEAK';
    colorHex = '#F59E0B';
  } else if (modifier >= 0) {
    name = 'SHARP';
    displayName = 'AI: SHARP';
    colorHex = '#EF4444';
  } else if (modifier >= -5) {
    name = 'STEADY';
    displayName = 'AI: STEADY';
    colorHex = '#EF4444';
  } else {
    name = 'SLOPPY';
    displayName = 'AI: SLOPPY';
    colorHex = '#A78BFA';
  }

  return {
    name,
    minModifier: min,
    maxModifier: max,
    displayName,
    colorHex,
    modifier
  };
}

export function chooseAIStrategy(board: Board, rand: () => number = Math.random): AIStrategy {
  let aiCount = 0;
  let playerCount = 0;
  let neutralCount = 0;

  for (let r = 0; r < 5; r++) {
    for (let c = 0; c < 5; c++) {
      const owner = board.cells[r][c].owner;
      if (owner === 'AI') aiCount++;
      else if (owner === 'PLAYER') playerCount++;
      else neutralCount++;
    }
  }

  if (aiCount <= playerCount - 2) return 'AGGRESSIVE_EXPANSION';
  if (aiCount >= playerCount + 2) return 'TERRITORY_FORTIFICATION';
  if (playerCount >= 3) {
    return rand() < 0.4 ? 'STEALING_CELLS' : 'SECURING_HIGH_VALUE';
  }
  if (neutralCount > 10) return 'SPREADING_THIN';
  return 'BUILDING_CLUSTER';
}

export function selectAILocks(
  placements: Record<string, Token>,
  board: Board,
  capability: AICapability,
  maxLocks: number,
  rand: () => number = Math.random
): Position[] {
  const keys = Object.keys(placements);
  if (keys.length === 0 || maxLocks <= 0) return [];

  const scored = keys.map(key => {
    const [r, c] = key.split(',').map(Number);
    const pos = { row: r, col: c };
    const cell = board.cells[r][c];
    let score = 0;

    if (cell.owner === 'AI') score += 30;
    score -= 15 * cell.value;

    const playerNeighbors = getNeighbors(pos).filter(
      n => board.cells[n.row][n.col].owner === 'PLAYER'
    ).length;
    score -= 20 * playerNeighbors;

    return { pos, score };
  });

  scored.sort((a, b) => b.score - a.score);

  if (capability.name === 'SLOPPY' && scored.length >= 3 && rand() < 0.3) {
    const temp = scored[0];
    scored[0] = scored[2];
    scored[2] = temp;
  }

  return scored.slice(0, maxLocks).map(s => s.pos);
}

export function planStandardPlacements(
  board: Board,
  capability: AICapability,
  strategy: AIStrategy,
  config: ArenaConfiguration,
  rand: () => number = Math.random
): AIPlan {
  const candidates: Position[] = [];
  for (let r = 0; r < 5; r++) {
    for (let c = 0; c < 5; c++) {
      if (board.cells[r][c].owner !== 'AI') {
        candidates.push({ row: r, col: c });
      }
    }
  }

  const scored = candidates.map(pos => {
    const cell = board.cells[pos.row][pos.col];
    let score = cell.value * 15;

    const neighbors = getNeighbors(pos);
    const aiNeighbors = neighbors.filter(
      n => board.cells[n.row][n.col].owner === 'AI'
    ).length;
    const playerNeighbors = neighbors.filter(
      n => board.cells[n.row][n.col].owner === 'PLAYER'
    ).length;

    score += aiNeighbors * 8;
    score += playerNeighbors * 5;

    if (cell.owner === 'PLAYER') score += 12;

    switch (strategy) {
      case 'AGGRESSIVE_EXPANSION':
        if (cell.owner === 'NONE') score += 10;
        break;
      case 'TERRITORY_FORTIFICATION':
        if (aiNeighbors >= 1) score += 10;
        break;
      case 'STEALING_CELLS':
        if (cell.owner === 'PLAYER') score += 12;
        break;
      case 'SECURING_HIGH_VALUE':
        if (cell.value >= 2) score += 15;
        break;
      case 'SPREADING_THIN':
        if (aiNeighbors <= 1) score += 10;
        break;
      case 'BUILDING_CLUSTER':
        if (aiNeighbors >= 1) score += 10;
        break;
    }

    return { pos, score };
  });

  scored.sort((a, b) => b.score - a.score);

  const sortedPositions = scored.map(s => s.pos);

  if (capability.name === 'SLOPPY' && sortedPositions.length >= 2) {
    const count = Math.min(3, sortedPositions.length);
    for (let i = 0; i < count; i++) {
      const swapIdx = Math.floor(rand() * count);
      const temp = sortedPositions[i];
      sortedPositions[i] = sortedPositions[swapIdx];
      sortedPositions[swapIdx] = temp;
    }
  }

  const placements: Record<string, Token> = {};
  const tokenCount = Math.min(sortedPositions.length, 5);
  const isBluffing = rand() < config.aiBluffRate;

  for (let i = 0; i < tokenCount; i++) {
    let tokenVal = 1;
    if (isBluffing && tokenCount >= 5) {
      if (i === 0) tokenVal = 1;
      else if (i === 1) tokenVal = 4;
      else if (i === 2) tokenVal = 3;
      else if (i === 3) tokenVal = 2;
      else if (i === 4) tokenVal = 5;
    } else {
      if (i === 0) tokenVal = 5;
      else if (i === 1) tokenVal = 4;
      else if (i === 2) tokenVal = 3;
      else if (i === 3) tokenVal = 2;
      else if (i === 4) tokenVal = 1;
    }
    const key = posKey(sortedPositions[i]);
    placements[key] = { value: tokenVal, isLocked: false };
  }

  const locks = selectAILocks(placements, board, capability, config.maxLocksPerRound, rand);

  return { placements, locks, strategy };
}

export function planArena1Placements(
  board: Board,
  playerPlacements: Record<string, Token>,
  capability: AICapability,
  config: ArenaConfiguration,
  rand: () => number = Math.random
): AIPlan {
  const candidates: Position[] = [];
  for (let r = 0; r < 5; r++) {
    for (let c = 0; c < 5; c++) {
      if (board.cells[r][c].owner !== 'AI') {
        candidates.push({ row: r, col: c });
      }
    }
  }

  const scored = candidates.map(pos => {
    const key = posKey(pos);
    const cell = board.cells[pos.row][pos.col];
    let score = cell.value * 15;

    // +20 if player-owned AND undefended (no player token placed on it)
    if (cell.owner === 'PLAYER' && !playerPlacements[key]) {
      score += 20;
    }

    // +12 if adjacent to 2+ player cells
    const neighbors = getNeighbors(pos);
    const playerNeighbors = neighbors.filter(
      n => board.cells[n.row][n.col].owner === 'PLAYER'
    ).length;
    if (playerNeighbors >= 2) {
      score += 12;
    }

    // +8 if high-value neutral
    if (cell.value === 3 && cell.owner === 'NONE') {
      score += 8;
    }

    // +5 per AI-adjacent
    const aiNeighbors = neighbors.filter(
      n => board.cells[n.row][n.col].owner === 'AI'
    ).length;
    score += aiNeighbors * 5;

    // -10 if player-defended
    if (playerPlacements[key]) {
      score -= 10;
    }

    return { pos, score };
  });

  scored.sort((a, b) => b.score - a.score);

  const sortedPositions = scored.map(s => s.pos);

  if (capability.name === 'SLOPPY' && sortedPositions.length >= 2) {
    const count = Math.min(3, sortedPositions.length);
    for (let i = 0; i < count; i++) {
      const swapIdx = Math.floor(rand() * count);
      const temp = sortedPositions[i];
      sortedPositions[i] = sortedPositions[swapIdx];
      sortedPositions[swapIdx] = temp;
    }
  }

  const placements: Record<string, Token> = {};
  const tokenCount = Math.min(sortedPositions.length, 5);
  const isBluffing = rand() < config.aiBluffRate;

  for (let i = 0; i < tokenCount; i++) {
    let tokenVal = 1;
    if (isBluffing && tokenCount >= 5) {
      if (i === 0) tokenVal = 1;
      else if (i === 1) tokenVal = 4;
      else if (i === 2) tokenVal = 3;
      else if (i === 3) tokenVal = 2;
      else if (i === 4) tokenVal = 5;
    } else {
      if (i === 0) tokenVal = 5;
      else if (i === 1) tokenVal = 4;
      else if (i === 2) tokenVal = 3;
      else if (i === 3) tokenVal = 2;
      else if (i === 4) tokenVal = 1;
    }
    const key = posKey(sortedPositions[i]);
    placements[key] = { value: tokenVal, isLocked: false };
  }

  const locks = selectAILocks(placements, board, capability, config.maxLocksPerRound, rand);

  return { placements, locks, strategy: 'STEALING_CELLS' };
}

export function planArena3Placements(
  board: Board,
  capability: AICapability,
  config: ArenaConfiguration,
  rand: () => number = Math.random
): AIPlan {
  const candidates: Position[] = [];
  for (let r = 0; r < 5; r++) {
    for (let c = 0; c < 5; c++) {
      if (board.cells[r][c].owner !== 'AI') {
        candidates.push({ row: r, col: c });
      }
    }
  }

  const scored = candidates.map(pos => {
    const cell = board.cells[pos.row][pos.col];
    let score = cell.value * 20;

    const neighbors = getNeighbors(pos);
    const aiNeighbors = neighbors.filter(
      n => board.cells[n.row][n.col].owner === 'AI'
    ).length;
    score += aiNeighbors * 10;

    if (cell.owner === 'PLAYER') score += 15;
    if (cell.value === 3) score += 8;

    return { pos, score };
  });

  scored.sort((a, b) => b.score - a.score);

  const sortedPositions = scored.map(s => s.pos);

  if (capability.name === 'SLOPPY' && sortedPositions.length >= 2) {
    const count = Math.min(3, sortedPositions.length);
    for (let i = 0; i < count; i++) {
      const swapIdx = Math.floor(rand() * count);
      const temp = sortedPositions[i];
      sortedPositions[i] = sortedPositions[swapIdx];
      sortedPositions[swapIdx] = temp;
    }
  }

  const placements: Record<string, Token> = {};
  const tokenCount = Math.min(sortedPositions.length, 5);
  const isBluffing = rand() < config.aiBluffRate;

  for (let i = 0; i < tokenCount; i++) {
    let tokenVal = 1;
    if (isBluffing && tokenCount >= 5) {
      if (i === 0) tokenVal = 1;
      else if (i === 1) tokenVal = 4;
      else if (i === 2) tokenVal = 3;
      else if (i === 3) tokenVal = 2;
      else if (i === 4) tokenVal = 5;
    } else {
      if (i === 0) tokenVal = 5;
      else if (i === 1) tokenVal = 4;
      else if (i === 2) tokenVal = 3;
      else if (i === 3) tokenVal = 2;
      else if (i === 4) tokenVal = 1;
    }
    const key = posKey(sortedPositions[i]);
    placements[key] = { value: tokenVal, isLocked: false };
  }

  const locks = selectAILocks(placements, board, capability, config.maxLocksPerRound, rand);

  return {
    placements,
    locks,
    strategy: isBluffing ? 'AGGRESSIVE_EXPANSION' : 'STEALING_CELLS'
  };
}
