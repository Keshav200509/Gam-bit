import {
  ArenaConfiguration,
  Board,
  GamePhase,
  Position,
  RoundState,
  Score,
  ScoutResult,
  Token
} from '../types/game';
import { generateBoard } from './boardGenerator';
import { getNeighbors, getRegionName, posKey } from './positionUtils';
import { applyLateGameEvents } from './lateGameEvents';
import { generateIntel } from './intelGenerator';
import {
  getCapabilityFromConfig,
  chooseAIStrategy,
  planStandardPlacements,
  planArena1Placements,
  planArena3Placements
} from './aiEngine';
import { resolveClash } from './clashResolver';
import { calculateIncome } from './incomeCalculator';

export class GameEngine {
  public roundState: RoundState;
  public score: Score;
  public activeConfig: ArenaConfiguration;
  private seedRandom?: () => number;

  constructor(config: ArenaConfiguration, seedRandom?: () => number) {
    this.activeConfig = config;
    this.seedRandom = seedRandom;
    this.score = { playerScore: 0, aiScore: 0 };
    this.roundState = this.initializeEmptyState();
  }

  private initializeEmptyState(): RoundState {
    const board: Board = {
      cells: Array.from({ length: 5 }, () =>
        Array.from({ length: 5 }, () => ({
          value: 1,
          owner: 'NONE',
          fortify: 0,
          modifier: 'None'
        }))
      )
    };

    return {
      roundNumber: 1,
      phase: 'Intel',
      board,
      playerPlacements: {},
      aiPlacements: {},
      playerLocks: [],
      aiLocks: [],
      scoutUsed: false,
      scoutedPosition: null,
      scoutResult: null,
      intelHint: '',
      intelConfidence: '',
      aiCapabilityName: 'STEADY',
      aiCapabilityModifier: 0,
      intel: null,
      incomeMultiplier: 1,
      lateGameMessage: '',
      lateGameEventType: 'NONE'
    };
  }

  public startNewGame(config: ArenaConfiguration, seedRandom?: () => number): RoundState {
    this.activeConfig = config;
    this.seedRandom = seedRandom;
    this.score = { playerScore: 0, aiScore: 0 };

    const initialBoard = generateBoard(this.seedRandom);
    this.roundState = {
      ...this.initializeEmptyState(),
      roundNumber: 1,
      phase: config.intelEnabled ? 'Intel' : 'Placing',
      board: initialBoard
    };

    const aiPlan = this.generateAiPlacementsForCurrentRound();
    this.generateIntelForCurrentRound(aiPlan);

    return this.roundState;
  }

  public restoreGameState(savedScore: Score, savedRoundState: RoundState, config: ArenaConfiguration) {
    this.score = savedScore;
    this.roundState = savedRoundState;
    this.activeConfig = config;
  }

  public startNewRound(): RoundState {
    if (this.roundState.roundNumber >= 12) {
      this.roundState = { ...this.roundState, phase: 'GameOver' };
      return this.roundState;
    }

    const nextRoundNumber = this.roundState.roundNumber + 1;

    // Apply late-game events
    const event = applyLateGameEvents(
      this.roundState.board,
      nextRoundNumber,
      [],
      [],
      this.seedRandom
    );

    this.roundState = {
      roundNumber: nextRoundNumber,
      phase: this.activeConfig.intelEnabled ? 'Intel' : 'Placing',
      board: event.modifiedBoard,
      playerPlacements: {},
      aiPlacements: {},
      playerLocks: event.modifiedPlayerLocks,
      aiLocks: event.modifiedAiLocks,
      scoutUsed: false,
      scoutedPosition: null,
      scoutResult: null,
      intelHint: '',
      intelConfidence: '',
      aiCapabilityName: 'STEADY',
      aiCapabilityModifier: 0,
      intel: null,
      incomeMultiplier: event.incomeMultiplier,
      lateGameMessage: event.message,
      lateGameEventType: event.type
    };

    const aiPlan = this.generateAiPlacementsForCurrentRound();
    this.generateIntelForCurrentRound(aiPlan);

    return this.roundState;
  }

  public transitionToPlacing(): RoundState {
    this.roundState = { ...this.roundState, phase: 'Placing' };
    return this.roundState;
  }

  public applyScout(pos: Position): RoundState {
    const isArena1 = this.activeConfig.arena === 'ASCENDENCY';
    let result: ScoutResult;

    if (isArena1) {
      const targetRegion = getRegionName(pos.row, pos.col);
      const isTargetingRegion = Object.keys(this.roundState.aiPlacements).some(key => {
        const [r, c] = key.split(',').map(Number);
        return getRegionName(r, c) === targetRegion;
      });
      result = isTargetingRegion ? 'CONTESTED' : 'CLEAR';
    } else {
      const key = posKey(pos);
      const hasToken = !!this.roundState.aiPlacements[key];
      result = hasToken ? 'CONTESTED' : 'CLEAR';
    }

    this.roundState = {
      ...this.roundState,
      scoutUsed: true,
      scoutedPosition: pos,
      scoutResult: result
    };
    return this.roundState;
  }

  public commitPlayerPlacements(
    placements: Record<string, Token>,
    locks: Position[]
  ): RoundState {
    this.roundState = {
      ...this.roundState,
      playerPlacements: placements,
      playerLocks: locks,
      phase: 'Revealing'
    };
    return this.roundState;
  }

  public executeArena1AiTurn(playerPlacements: Record<string, Token>): RoundState {
    const capability = getCapabilityFromConfig(this.activeConfig, this.seedRandom);
    const aiPlan = planArena1Placements(
      this.roundState.board,
      playerPlacements,
      capability,
      this.activeConfig,
      this.seedRandom
    );

    this.roundState = {
      ...this.roundState,
      aiPlacements: aiPlan.placements,
      aiLocks: aiPlan.locks,
      aiCapabilityName: capability.name,
      aiCapabilityModifier: capability.modifier
    };
    return this.roundState;
  }

  public transitionToResolving(): RoundState {
    this.roundState = { ...this.roundState, phase: 'Resolving' };
    return this.roundState;
  }

  public resolveRound(): RoundState {
    let updatedBoard = this.roundState.board;

    for (let r = 0; r < 5; r++) {
      for (let c = 0; c < 5; c++) {
        const pos = { row: r, col: c };
        const cell = updatedBoard.cells[r][c];

        const result = resolveClash(
          pos,
          this.roundState.playerPlacements,
          this.roundState.aiPlacements,
          updatedBoard,
          this.roundState.playerLocks,
          this.roundState.aiLocks
        );

        let newOwner = cell.owner;
        switch (result.type) {
          case 'PlayerWins':
          case 'PlayerUncontested':
          case 'PlayerDefends':
            newOwner = 'PLAYER';
            break;
          case 'AIWins':
          case 'AIUncontested':
          case 'AIDefends':
            newOwner = 'AI';
            break;
          case 'Tie':
            newOwner = 'NONE';
            break;
        }

        if (newOwner !== cell.owner) {
          const newCells = updatedBoard.cells.map((rowArr, rowIdx) =>
            rowArr.map((oldCell, colIdx) => {
              if (rowIdx === r && colIdx === c) {
                return { ...oldCell, owner: newOwner };
              }
              return oldCell;
            })
          );
          updatedBoard = { cells: newCells };
        }
      }
    }

    this.roundState = {
      ...this.roundState,
      board: updatedBoard,
      phase: 'IncomeSummary'
    };
    return this.roundState;
  }

  public collectIncome(): { playerIncome: number; aiIncome: number } {
    const { playerIncome, aiIncome } = calculateIncome(
      this.roundState.board,
      this.roundState.roundNumber,
      this.roundState.incomeMultiplier
    );

    this.score = {
      playerScore: this.score.playerScore + playerIncome,
      aiScore: this.score.aiScore + aiIncome
    };

    return { playerIncome, aiIncome };
  }

  public transitionToTransitionOrGameOver(): RoundState {
    const allOwned = this.roundState.board.cells
      .flat()
      .every(c => c.owner !== 'NONE');
    const nextPhase: GamePhase =
      this.roundState.roundNumber >= 12 || allOwned
        ? 'GameOver'
        : 'RoundTransition';
    this.roundState = { ...this.roundState, phase: nextPhase };
    return this.roundState;
  }

  private generateAiPlacementsForCurrentRound() {
    const capability = getCapabilityFromConfig(this.activeConfig, this.seedRandom);

    let plan;
    if (this.activeConfig.arena === 'OBLIVION') {
      plan = planArena3Placements(this.roundState.board, capability, this.activeConfig, this.seedRandom);
    } else if (this.activeConfig.arena === 'ASCENDENCY') {
      const strategy = chooseAIStrategy(this.roundState.board, this.seedRandom);
      plan = planStandardPlacements(this.roundState.board, capability, strategy, this.activeConfig, this.seedRandom);
    } else {
      const strategy = chooseAIStrategy(this.roundState.board, this.seedRandom);
      plan = planStandardPlacements(this.roundState.board, capability, strategy, this.activeConfig, this.seedRandom);
    }

    this.roundState = {
      ...this.roundState,
      aiPlacements: plan.placements,
      aiLocks: plan.locks,
      aiCapabilityName: capability.name,
      aiCapabilityModifier: capability.modifier
    };

    return plan;
  }

  private generateIntelForCurrentRound(aiPlan: any) {
    if (!this.activeConfig.intelEnabled) return;

    let trickiness = (this.roundState.roundNumber - 1) / 11.0;
    trickiness *= this.activeConfig.intelTrickinessMultiplier;
    trickiness = Math.max(0, Math.min(1, trickiness));

    const isArena1 = this.activeConfig.arena === 'ASCENDENCY';

    const intelHintObj = generateIntel(
      aiPlan,
      this.roundState.board,
      trickiness,
      isArena1,
      this.seedRandom
    );

    this.roundState = {
      ...this.roundState,
      intel: intelHintObj,
      intelHint: intelHintObj.text,
      intelConfidence: intelHintObj.reliabilityLabel
    };
  }
}
