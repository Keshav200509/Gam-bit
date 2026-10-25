import React from 'react';
import { Sparkles, Flame, Zap, Swords } from 'lucide-react';

interface RoundProgressDotsProps {
  currentRound: number;
}

export const RoundProgressDots: React.FC<RoundProgressDotsProps> = ({ currentRound }) => {
  return (
    <div className="flex items-center justify-center gap-1.5 px-3 py-1.5 bg-[#12131A]/80 border border-[#1E2030] rounded-full backdrop-blur-md">
      {Array.from({ length: 12 }, (_, i) => {
        const roundNum = i + 1;
        const isPast = roundNum < currentRound;
        const isCurrent = roundNum === currentRound;

        let specialIcon = null;
        if (roundNum === 8) specialIcon = <Sparkles size={10} className="text-amber-400" />;
        else if (roundNum === 10) specialIcon = <Flame size={10} className="text-orange-400" />;
        else if (roundNum === 11) specialIcon = <Zap size={10} className="text-amber-300" />;
        else if (roundNum === 12) specialIcon = <Swords size={10} className="text-red-400" />;

        let dotClass = 'bg-[#1E2030] text-[#4A5068]';
        if (isCurrent) {
          dotClass = 'bg-[#2DD4BF] text-black ring-2 ring-[#2DD4BF]/50 scale-110 font-bold';
        } else if (isPast) {
          dotClass = 'bg-[#2DD4BF]/20 text-[#2DD4BF] border border-[#2DD4BF]/40';
        }

        return (
          <div
            key={roundNum}
            title={`Round ${roundNum}`}
            className={`relative flex items-center justify-center w-5 h-5 rounded-full text-[10px] font-mono transition-all duration-300 ${dotClass}`}
          >
            {isCurrent ? roundNum : specialIcon || roundNum}
          </div>
        );
      })}
    </div>
  );
};
