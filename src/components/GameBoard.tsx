import React from 'react';
import { Board, ClashResult, GamePhase, Position, ScoutResult, Token } from '../types/game';
import { CellView } from './CellView';
import { posKey } from '../domain/positionUtils';

interface GameBoardProps {
  board: Board;
  playerPlacements: Record<string, Token>;
  aiPlacements: Record<string, Token>;
  playerLocks: Position[];
  aiLocks: Position[];
  scoutUsed: boolean;
  scoutedPosition: Position | null;
  scoutResult: ScoutResult | null;
  phase: GamePhase;
  currentResolvingPosition: Position | null;
  clashResults: Record<string, ClashResult>;
  onCellClick: (pos: Position) => void;
  onMouseEnter: (pos: Position) => void;
  onMouseLeave: () => void;
  scoutMode: boolean;
  lockMode: boolean;
}

export const GameBoard: React.FC<GameBoardProps> = ({
  board,
  playerPlacements,
  aiPlacements,
  playerLocks,
  aiLocks,
  scoutedPosition,
  scoutResult,
  phase,
  currentResolvingPosition,
  clashResults,
  onCellClick,
  onMouseEnter,
  onMouseLeave,
  scoutMode,
  lockMode
}) => {
  return (
    <div className="w-full max-w-[480px] aspect-square p-2 bg-[#0E1017] rounded-2xl border border-[#1E2030] shadow-2xl shadow-black/80">
      <div className="grid grid-cols-5 gap-1.5 sm:gap-2 w-full h-full">
        {board.cells.map((row, r) =>
          row.map((cell, c) => {
            const pos = { row: r, col: c };
            const key = posKey(pos);
            const playerToken = playerPlacements[key];
            const aiToken = aiPlacements[key];

            const isPlayerLocked = playerLocks.some(l => l.row === r && l.col === c);
            const isAiLocked = aiLocks.some(l => l.row === r && l.col === c);

            const isScouted = scoutedPosition && scoutedPosition.row === r && scoutedPosition.col === c;
            const thisScoutResult = isScouted ? scoutResult : null;

            const isResolving =
              currentResolvingPosition !== null &&
              currentResolvingPosition.row === r &&
              currentResolvingPosition.col === c;

            const thisClashResult = clashResults[key];

            return (
              <CellView
                key={key}
                cell={cell}
                position={pos}
                playerToken={playerToken}
                aiToken={aiToken}
                isPlayerLocked={isPlayerLocked}
                isAiLocked={isAiLocked}
                scoutResult={thisScoutResult}
                phase={phase}
                isCurrentResolving={isResolving}
                clashResult={thisClashResult}
                onCellClick={onCellClick}
                onMouseEnter={onMouseEnter}
                onMouseLeave={onMouseLeave}
                scoutMode={scoutMode}
                lockMode={lockMode}
              />
            );
          })
        )}
      </div>
    </div>
  );
};
