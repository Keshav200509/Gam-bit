import React from 'react';
import { Position, ProbabilityBreakdown } from '../types/game';

interface ProbabilityTooltipProps {
  position: Position | null;
  breakdown: ProbabilityBreakdown | null;
  onDismiss: () => void;
}

export const ProbabilityTooltip: React.FC<ProbabilityTooltipProps> = ({
  position,
  breakdown,
  onDismiss
}) => {
  if (!position || !breakdown) return null;

  return (
    <div
      onClick={onDismiss}
      className="w-full max-w-[480px] p-3 bg-[#12131A]/95 rounded-xl border border-[#1E2030] shadow-xl backdrop-blur-md animate-in fade-in zoom-in-95 duration-150"
    >
      <div className="flex items-center justify-between pb-1.5 mb-2 border-b border-[#1E2030]">
        <span className="text-xs font-mono font-bold text-white uppercase tracking-wider">
          TACTICAL PROBABILITY // CELL ({position.row},{position.col})
        </span>
        <span className="text-[10px] font-mono text-[#4A5068]">CLICK TO DISMISS</span>
      </div>

      {/* Probability Segmented Bar */}
      <div className="w-full h-3 bg-[#1E2030] rounded-full overflow-hidden flex mb-2">
        {breakdown.win > 0 && (
          <div
            style={{ width: `${breakdown.win}%` }}
            className="h-full bg-[#2DD4BF] transition-all"
            title={`Win: ${breakdown.win}%`}
          />
        )}
        {breakdown.uncontested > 0 && (
          <div
            style={{ width: `${breakdown.uncontested}%` }}
            className="h-full bg-emerald-700 transition-all"
            title={`Uncontested: ${breakdown.uncontested}%`}
          />
        )}
        {breakdown.tie > 0 && (
          <div
            style={{ width: `${breakdown.tie}%` }}
            className="h-full bg-amber-400 transition-all"
            title={`Tie: ${breakdown.tie}%`}
          />
        )}
        {breakdown.lose > 0 && (
          <div
            style={{ width: `${breakdown.lose}%` }}
            className="h-full bg-red-500 transition-all"
            title={`Lose: ${breakdown.lose}%`}
          />
        )}
      </div>

      {/* Legend & Percentages */}
      <div className="grid grid-cols-4 gap-2 text-center font-mono text-[11px]">
        <div className="bg-[#1E2030]/40 p-1 rounded">
          <span className="text-[#2DD4BF] font-bold block">{breakdown.win}%</span>
          <span className="text-[#4A5068] text-[9px]">CLASH WIN</span>
        </div>
        <div className="bg-[#1E2030]/40 p-1 rounded">
          <span className="text-emerald-400 font-bold block">{breakdown.uncontested}%</span>
          <span className="text-[#4A5068] text-[9px]">UNCONTESTED</span>
        </div>
        <div className="bg-[#1E2030]/40 p-1 rounded">
          <span className="text-amber-400 font-bold block">{breakdown.tie}%</span>
          <span className="text-[#4A5068] text-[9px]">TIE</span>
        </div>
        <div className="bg-[#1E2030]/40 p-1 rounded">
          <span className="text-red-400 font-bold block">{breakdown.lose}%</span>
          <span className="text-[#4A5068] text-[9px]">CLASH LOSE</span>
        </div>
      </div>
    </div>
  );
};
