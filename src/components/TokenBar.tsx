import React from 'react';
import { Token } from '../types/game';

interface TokenBarProps {
  playerPlacements: Record<string, Token>;
  selectedToken: number | null;
  onSelectToken: (tokenVal: number) => void;
  disabled?: boolean;
}

export const TokenBar: React.FC<TokenBarProps> = ({
  playerPlacements,
  selectedToken,
  onSelectToken,
  disabled = false
}) => {
  const placedValues = new Set(Object.values(playerPlacements).map(t => t.value));
  const tokens = [1, 2, 3, 4, 5];

  return (
    <div className="w-full max-w-[480px] flex items-center justify-between p-2 bg-[#12131A] rounded-xl border border-[#1E2030]">
      <div className="flex items-center gap-1.5 pl-1">
        <span className="text-[11px] font-mono font-bold text-[#4A5068] uppercase tracking-wider">
          TOKENS
        </span>
      </div>

      <div className="flex items-center gap-2 sm:gap-3">
        {tokens.map(val => {
          const isPlaced = placedValues.has(val);
          const isSelected = selectedToken === val;

          let btnClass = 'bg-[#1E2030] text-[#C8D0E0] hover:border-[#2DD4BF]/50 border-[#1E2030]';

          if (isSelected) {
            btnClass =
              'bg-[#2DD4BF] text-black font-extrabold ring-4 ring-[#2DD4BF]/30 scale-110 shadow-lg shadow-teal-500/50 border-[#2DD4BF]';
          } else if (isPlaced) {
            btnClass = 'bg-[#1E2030]/50 text-[#4A5068] border-dashed border-[#1E2030] opacity-50';
          }

          return (
            <button
              key={val}
              type="button"
              disabled={disabled}
              onClick={() => onSelectToken(val)}
              className={`relative flex items-center justify-center w-9 h-9 sm:w-10 sm:h-10 rounded-full font-mono text-sm font-bold border transition-all duration-200 ${btnClass} disabled:pointer-events-none`}
            >
              <span>{val}</span>
              {isPlaced && !isSelected && (
                <span className="absolute -bottom-1 text-[8px] font-mono text-teal-400">
                  ON GRID
                </span>
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
};
