import { AICapability, Board, IntelHint, Position, ProbabilityBreakdown, ScoutResult } from '../types/game';
import { getNeighbors, getRegionName } from './positionUtils';

export function calculateProbabilities(
  position: Position,
  playerToken: number,
  board: Board,
  intel: IntelHint | null,
  scoutResult: ScoutResult | null,
  aiCapability: AICapability
): ProbabilityBreakdown {
  const cell = board.cells[position.row][position.col];
  const cellValue = cell.value;

  const neighbors = getNeighbors(position);
  const aiNeighborsCount = neighbors.filter(
    n => board.cells[n.row][n.col].owner === 'AI'
  ).length;
  const playerNeighborsCount = neighbors.filter(
    n => board.cells[n.row][n.col].owner === 'PLAYER'
  ).length;

  // 1. Calculate AI contest probability
  let contestProb =
    0.15 +
    cellValue * 0.06 +
    aiNeighborsCount * 0.05 +
    playerNeighborsCount * 0.04;
  if (cell.owner === 'PLAYER') {
    contestProb += 0.1;
  }

  // 2. If intel mentions this region, add 0.08
  const cellRegion = getRegionName(position.row, position.col);
  if (intel) {
    let mentionsRegion = false;
    if (intel.actualData.region === cellRegion) {
      mentionsRegion = true;
    }
    if (mentionsRegion) {
      contestProb += 0.08;
    }
  }

  // 3. Scout result override
  if (scoutResult) {
    contestProb = scoutResult === 'CLEAR' ? 0.0 : 1.0;
  }

  contestProb = Math.max(0, Math.min(1, contestProb));

  // 4. Estimate AI token distribution
  const weights = [0.2, 0.2, 0.2, 0.2, 0.2];

  if (cellValue === 3) {
    weights[4] += 0.2; // token 5
    weights[3] += 0.1; // token 4
  } else if (cellValue === 1) {
    weights[0] += 0.2; // token 1
    weights[1] += 0.1; // token 2
  }

  if (intel && intel.type === 'TOKEN_REGION' && intel.actualData.region === cellRegion) {
    const tokenVal = intel.actualData.tokenValue;
    if (tokenVal && tokenVal >= 1 && tokenVal <= 5) {
      const idx = tokenVal - 1;
      if (intel.isTrue) {
        weights[idx] += 0.4;
      } else {
        weights[idx] = Math.max(0.05, weights[idx] - 0.15);
      }
    }
  }

  // Normalize
  let sum = weights.reduce((a, b) => a + b, 0);
  if (sum <= 0) sum = 1;
  for (let i = 0; i < 5; i++) weights[i] /= sum;

  // 5. Capability randomness blend
  const modAbs = Math.abs(aiCapability.modifier);
  const blendFactor = 0.3 * (modAbs / 20.0);
  for (let i = 0; i < 5; i++) {
    weights[i] = (1.0 - blendFactor) * weights[i] + blendFactor * 0.2;
  }
  sum = weights.reduce((a, b) => a + b, 0);
  for (let i = 0; i < 5; i++) weights[i] /= sum;

  // 6. Calculate clash outcomes
  const playerFortifyBonus = cell.owner === 'PLAYER' ? cell.fortify : 0;
  const playerStrength = playerToken + playerNeighborsCount + playerFortifyBonus;

  const aiFortifyBonus = cell.owner === 'AI' ? cell.fortify : 0;

  let winProb = 0;
  let loseProb = 0;
  let tieProb = 0;

  for (let aiVal = 1; aiVal <= 5; aiVal++) {
    const aiStrength = aiVal + aiNeighborsCount + aiFortifyBonus;
    const w = weights[aiVal - 1];
    if (playerStrength > aiStrength) winProb += w;
    else if (playerStrength < aiStrength) loseProb += w;
    else tieProb += w;
  }

  const scaledWin = contestProb * winProb;
  const scaledLose = contestProb * loseProb;
  const scaledTie = contestProb * tieProb;
  const uncontested = 1.0 - contestProb;

  const uncontestedPct = Math.max(0, Math.min(100, uncontested * 100));
  const winPct = Math.max(0, Math.min(100, scaledWin * 100));
  const losePct = Math.max(0, Math.min(100, scaledLose * 100));
  const tiePct = Math.max(0, Math.min(100, scaledTie * 100));

  const totalSum = uncontestedPct + winPct + losePct + tiePct;
  const u = Math.round((uncontestedPct / totalSum) * 100);
  const w = Math.round((winPct / totalSum) * 100);
  const l = Math.round((losePct / totalSum) * 100);
  const t = Math.max(0, 100 - u - w - l);

  return { win: w, lose: l, tie: t, uncontested: u };
}
