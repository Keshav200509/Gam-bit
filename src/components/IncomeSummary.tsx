import React from 'react';
import { Coins, ArrowRight, Trophy } from 'lucide-react';

interface IncomeSummaryProps {
  playerIncome: number;
  aiIncome: number;
  playerScore: number;
  aiScore: number;
  roundNumber: number;
  incomeMultiplier: number;
  isGameOver: boolean;
  onContinue: () => void;
}

export const IncomeSummary: React.FC<IncomeSummaryProps> = ({
  playerIncome,
  aiIncome,
  playerScore,
  aiScore,
  roundNumber,
  incomeMultiplier,
  isGameOver,
  onContinue
}) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in duration-200">
      <div className="w-full max-w-sm p-6 bg-[#12131A] rounded-2xl border border-[#2DD4BF]/40 shadow-2xl shadow-teal-950/50 flex flex-col items-center text-center">
        <div className="w-12 h-12 rounded-full bg-teal-500/10 border border-teal-500/30 flex items-center justify-center text-[#2DD4BF] mb-3">
          <Coins size={24} />
        </div>

        <h3 className="text-base font-extrabold font-mono tracking-widest text-white uppercase">
          ROUND {roundNumber} REVENUE
        </h3>
        {incomeMultiplier > 1 && (
          <span className="text-xs font-mono font-bold text-amber-400 bg-amber-500/10 px-2 py-0.5 rounded-full mt-1">
            {incomeMultiplier}× DOUBLE STAKES APPLIED
          </span>
        )}

        {/* Comparison grid */}
        <div className="grid grid-cols-2 gap-3 w-full my-5">
          <div className="p-3 bg-[#0E1017] rounded-xl border border-teal-500/30 flex flex-col items-center">
            <span className="text-[10px] font-mono text-teal-400 font-bold uppercase">
              YOU EARNED
            </span>
            <span className="text-2xl font-extrabold font-mono text-white mt-1">
              +{playerIncome}
            </span>
            <span className="text-[10px] font-mono text-[#4A5068] mt-1">
              Total: {playerScore}
            </span>
          </div>

          <div className="p-3 bg-[#0E1017] rounded-xl border border-red-500/30 flex flex-col items-center">
            <span className="text-[10px] font-mono text-red-400 font-bold uppercase">
              AI EARNED
            </span>
            <span className="text-2xl font-extrabold font-mono text-white mt-1">
              +{aiIncome}
            </span>
            <span className="text-[10px] font-mono text-[#4A5068] mt-1">
              Total: {aiScore}
            </span>
          </div>
        </div>

        <button
          type="button"
          onClick={onContinue}
          className="w-full flex items-center justify-center gap-2 py-3 px-4 rounded-xl font-mono text-sm font-extrabold bg-[#2DD4BF] hover:bg-teal-300 text-black shadow-lg shadow-teal-500/30 transition-all cursor-pointer"
        >
          {isGameOver ? (
            <>
              <Trophy size={16} />
              <span>VIEW FINAL DEPLOYMENT REPORT</span>
            </>
          ) : (
            <>
              <span>PROCEED TO ROUND {roundNumber + 1}</span>
              <ArrowRight size={16} />
            </>
          )}
        </button>
      </div>
    </div>
  );
};
