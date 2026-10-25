import React from 'react';
import { Eye, Lock, RotateCcw, Send } from 'lucide-react';
import { GamePhase } from '../types/game';

interface ActionBarProps {
  phase: GamePhase;
  scoutEnabled: boolean;
  scoutUsed: boolean;
  scoutMode: boolean;
  onToggleScout: () => void;
  maxLocks: number;
  currentLocksCount: number;
  lockMode: boolean;
  onToggleLock: () => void;
  tokensPlacedCount: number;
  onClear: () => void;
  onCommit: () => void;
}

export const ActionBar: React.FC<ActionBarProps> = ({
  phase,
  scoutEnabled,
  scoutUsed,
  scoutMode,
  onToggleScout,
  maxLocks,
  currentLocksCount,
  lockMode,
  onToggleLock,
  tokensPlacedCount,
  onClear,
  onCommit
}) => {
  const isPlacing = phase === 'Placing';
  const canCommit = isPlacing && tokensPlacedCount > 0;
  const allPlaced = tokensPlacedCount === 5;

  return (
    <div className="w-full max-w-[480px] grid grid-cols-4 gap-2 p-2 bg-[#12131A] rounded-xl border border-[#1E2030]">
      {/* Scout Button */}
      <button
        type="button"
        disabled={!isPlacing || !scoutEnabled || scoutUsed}
        onClick={onToggleScout}
        className={`flex flex-col items-center justify-center p-2 rounded-lg font-mono text-xs border transition-all ${
          scoutMode
            ? 'bg-blue-600 text-white border-blue-400 ring-2 ring-blue-500/50'
            : scoutUsed
            ? 'bg-[#1E2030]/30 text-[#4A5068] border-transparent opacity-40'
            : 'bg-[#1E2030] text-[#C8D0E0] hover:text-white border-[#1E2030]'
        } disabled:pointer-events-none`}
      >
        <Eye size={18} className={scoutMode ? 'animate-pulse' : ''} />
        <span className="text-[10px] mt-1 font-bold">SCOUT</span>
        <span className="text-[8px] opacity-75">{scoutUsed ? 'USED' : scoutEnabled ? 'READY' : 'OFF'}</span>
      </button>

      {/* Lock Button */}
      <button
        type="button"
        disabled={!isPlacing || maxLocks === 0}
        onClick={onToggleLock}
        className={`flex flex-col items-center justify-center p-2 rounded-lg font-mono text-xs border transition-all ${
          lockMode
            ? 'bg-amber-600 text-black border-amber-300 ring-2 ring-amber-400/50'
            : maxLocks === 0
            ? 'bg-[#1E2030]/30 text-[#4A5068] border-transparent opacity-40'
            : 'bg-[#1E2030] text-[#C8D0E0] hover:text-white border-[#1E2030]'
        } disabled:pointer-events-none`}
      >
        <Lock size={18} className={lockMode ? 'animate-bounce' : ''} />
        <span className="text-[10px] mt-1 font-bold">LOCK</span>
        <span className="text-[8px] opacity-75">
          {maxLocks > 0 ? `${currentLocksCount}/${maxLocks}` : '0/0'}
        </span>
      </button>

      {/* Clear Button */}
      <button
        type="button"
        disabled={!isPlacing || tokensPlacedCount === 0}
        onClick={onClear}
        className="flex flex-col items-center justify-center p-2 rounded-lg font-mono text-xs bg-[#1E2030] hover:bg-[#2A2E44] text-[#C8D0E0] hover:text-white border border-[#1E2030] transition-colors disabled:opacity-40 disabled:pointer-events-none"
      >
        <RotateCcw size={18} />
        <span className="text-[10px] mt-1 font-bold">CLEAR</span>
        <span className="text-[8px] opacity-75">{tokensPlacedCount}/5</span>
      </button>

      {/* Commit Button */}
      <button
        type="button"
        disabled={!canCommit}
        onClick={onCommit}
        className={`flex flex-col items-center justify-center p-2 rounded-lg font-mono text-xs border transition-all ${
          allPlaced
            ? 'bg-gradient-to-r from-[#2DD4BF] to-teal-400 text-black font-extrabold border-teal-300 shadow-lg shadow-teal-500/40 animate-pulse'
            : canCommit
            ? 'bg-[#2DD4BF]/80 text-black font-bold border-teal-500'
            : 'bg-[#1E2030]/50 text-[#4A5068] border-transparent opacity-40'
        } disabled:pointer-events-none`}
      >
        <Send size={18} />
        <span className="text-[10px] mt-1 font-extrabold">COMMIT</span>
        <span className="text-[8px] opacity-90">{allPlaced ? 'ALL PLACED' : `${tokensPlacedCount}/5`}</span>
      </button>
    </div>
  );
};
