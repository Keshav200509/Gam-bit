import React from 'react';
import { Lock, Shield, Sparkles, Flame, Swords, Eye, CheckCircle2, AlertTriangle } from 'lucide-react';
import { Cell, ClashResult, GamePhase, Position, ScoutResult, Token } from '../types/game';

interface CellViewProps {
  cell: Cell;
  position: Position;
  playerToken?: Token;
  aiToken?: Token;
  isPlayerLocked: boolean;
  isAiLocked: boolean;
  scoutResult: ScoutResult | null;
  phase: GamePhase;
  isCurrentResolving: boolean;
  clashResult?: ClashResult;
  onCellClick: (pos: Position) => void;
  onMouseEnter: (pos: Position) => void;
  onMouseLeave: () => void;
  scoutMode: boolean;
  lockMode: boolean;
}

export const CellView: React.FC<CellViewProps> = ({
  cell,
  position,
  playerToken,
  aiToken,
  isPlayerLocked,
  isAiLocked,
  scoutResult,
  phase,
  isCurrentResolving,
  clashResult,
  onCellClick,
  onMouseEnter,
  onMouseLeave,
  scoutMode,
  lockMode
}) => {
  const isOwnerPlayer = cell.owner === 'PLAYER';
  const isOwnerAI = cell.owner === 'AI';

  // Base background & border styling
  let containerBorder = 'border-[#1E2030] hover:border-[#2DD4BF]/50';
  let containerBg = 'bg-[#12131A]/90';

  if (isOwnerPlayer) {
    containerBorder = 'border-[#2DD4BF] shadow-[inset_0_0_12px_rgba(45,212,191,0.15)]';
    containerBg = 'bg-[#12131A]';
  } else if (isOwnerAI) {
    containerBorder = 'border-[#EF4444] shadow-[inset_0_0_12px_rgba(239,68,68,0.15)]';
    containerBg = 'bg-[#12131A]';
  }

  if (isCurrentResolving) {
    containerBorder = 'border-amber-400 ring-2 ring-amber-400/60 scale-105 z-20 shadow-lg shadow-amber-500/30';
  }

  // Modifiers
  const isGolden = cell.modifier === 'GoldenCell';
  const isVolatile = cell.modifier === 'Volatile';
  const isBattleground = cell.modifier === 'Battleground';

  const showAiToken = (phase === 'Revealing' || phase === 'Resolving' || phase === 'IncomeSummary') && aiToken;

  return (
    <div
      onClick={() => onCellClick(position)}
      onMouseEnter={() => onMouseEnter(position)}
      onMouseLeave={onMouseLeave}
      className={`relative flex flex-col items-center justify-between p-1.5 sm:p-2 rounded-xl border transition-all duration-200 cursor-pointer select-none aspect-square ${containerBg} ${containerBorder}`}
    >
      {/* Top Bar inside Cell: Value + Modifier Badge + Fortify */}
      <div className="w-full flex items-center justify-between text-[10px] sm:text-xs font-mono">
        {/* Cell points value */}
        <span
          className={`font-bold px-1 rounded ${
            cell.value === 3
              ? 'text-amber-400 bg-amber-500/10'
              : cell.value === 2
              ? 'text-blue-400 bg-blue-500/10'
              : 'text-[#4A5068]'
          }`}
        >
          {cell.value}pt
        </span>

        {/* Modifier icons */}
        <div className="flex items-center gap-1">
          {isGolden && (
            <span title="Golden Cell: 2x points">
              <Sparkles size={12} className="text-amber-400 animate-pulse" />
            </span>
          )}
          {isVolatile && (
            <span title="Volatile: 2x at Round 12">
              <Flame size={12} className="text-orange-500" />
            </span>
          )}
          {isBattleground && (
            <span title="Battleground: +1 defending">
              <Swords size={12} className="text-purple-400" />
            </span>
          )}
          {cell.fortify > 0 && (
            <div className="flex items-center text-[9px] text-teal-300 bg-teal-900/40 px-1 rounded font-mono">
              <Shield size={10} className="mr-0.5" />
              <span>+{cell.fortify}</span>
            </div>
          )}
        </div>
      </div>

      {/* Center content: Placed Tokens (Player / AI) */}
      <div className="flex-1 flex items-center justify-center gap-1 w-full my-1">
        {/* Player Token */}
        {playerToken && (
          <div
            className={`relative flex items-center justify-center w-7 h-7 sm:w-8 sm:h-8 rounded-full font-bold font-mono text-xs sm:text-sm shadow-md transition-all ${
              isPlayerLocked
                ? 'bg-gradient-to-br from-amber-400 to-amber-600 text-black ring-2 ring-amber-300 shadow-amber-500/50'
                : 'bg-gradient-to-br from-[#2DD4BF] to-teal-700 text-black shadow-teal-500/30'
            }`}
          >
            <span>{playerToken.value}</span>
            {isPlayerLocked && (
              <Lock size={10} className="absolute -top-1 -right-1 text-black bg-amber-300 rounded-full p-0.5" />
            )}
          </div>
        )}

        {/* AI Token (when visible) */}
        {showAiToken && (
          <div
            className={`relative flex items-center justify-center w-7 h-7 sm:w-8 sm:h-8 rounded-full font-bold font-mono text-xs sm:text-sm shadow-md animate-in fade-in zoom-in duration-300 ${
              isAiLocked
                ? 'bg-gradient-to-br from-rose-500 to-red-800 text-white ring-2 ring-amber-400 shadow-red-500/50'
                : 'bg-gradient-to-br from-red-500 to-red-800 text-white shadow-red-500/30'
            }`}
          >
            <span>{aiToken.value}</span>
            {isAiLocked && (
              <Lock size={10} className="absolute -top-1 -right-1 text-white bg-red-900 rounded-full p-0.5" />
            )}
          </div>
        )}
      </div>

      {/* Bottom Status: Scout status or Ownership status */}
      <div className="w-full flex items-center justify-between text-[9px] font-mono">
        {scoutResult && (
          <div
            className={`flex items-center gap-0.5 px-1 py-0.2 rounded font-bold ${
              scoutResult === 'CLEAR'
                ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40'
                : 'bg-rose-500/20 text-rose-300 border border-rose-500/40'
            }`}
          >
            {scoutResult === 'CLEAR' ? <CheckCircle2 size={9} /> : <AlertTriangle size={9} />}
            <span>{scoutResult}</span>
          </div>
        )}

        <div className="ml-auto flex items-center gap-1">
          {isOwnerPlayer && <span className="text-[#2DD4BF] font-bold">YOU</span>}
          {isOwnerAI && <span className="text-[#EF4444] font-bold">AI</span>}
        </div>
      </div>

      {/* Action Mode Overlays */}
      {scoutMode && (
        <div className="absolute inset-0 bg-blue-500/10 border-2 border-dashed border-blue-400/50 rounded-xl flex items-center justify-center pointer-events-none">
          <Eye size={18} className="text-blue-400 animate-pulse" />
        </div>
      )}

      {lockMode && playerToken && (
        <div className="absolute inset-0 bg-amber-500/10 border-2 border-dashed border-amber-400/50 rounded-xl flex items-center justify-center pointer-events-none">
          <Lock size={18} className="text-amber-400 animate-pulse" />
        </div>
      )}
    </div>
  );
};
