import React from 'react';
import { AlertCircle, Play, RotateCcw } from 'lucide-react';

interface ResumeDialogProps {
  roundNumber: number;
  arena: string;
  level: string;
  onResume: () => void;
  onDiscard: () => void;
}

export const ResumeDialog: React.FC<ResumeDialogProps> = ({
  roundNumber,
  arena,
  level,
  onResume,
  onDiscard
}) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-in fade-in duration-200">
      <div className="w-full max-w-sm p-6 bg-[#12131A] rounded-2xl border border-amber-500/40 shadow-2xl shadow-amber-950/40 flex flex-col items-center text-center">
        <div className="w-12 h-12 rounded-full bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400 mb-3">
          <AlertCircle size={24} />
        </div>

        <h3 className="text-base font-extrabold font-mono tracking-widest text-white uppercase">
          RECOVER SIMULATION?
        </h3>
        <p className="text-xs font-sans text-[#C8D0E0] mt-2 leading-relaxed">
          An active tactical session from <strong className="text-white">Round {roundNumber}</strong> in{' '}
          <span className="text-[#2DD4BF] font-mono">{arena} ({level})</span> was detected in local persistence.
        </p>

        <div className="flex flex-col gap-2 w-full mt-6">
          <button
            type="button"
            onClick={onResume}
            className="w-full flex items-center justify-center gap-2 py-3 px-4 rounded-xl font-mono text-xs font-bold bg-[#2DD4BF] hover:bg-teal-300 text-black shadow-lg shadow-teal-500/20 transition-all cursor-pointer"
          >
            <Play size={14} />
            <span>RESUME SESSION</span>
          </button>

          <button
            type="button"
            onClick={onDiscard}
            className="w-full flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl font-mono text-xs font-medium bg-[#1E2030] hover:bg-red-950/50 text-red-400 border border-red-500/20 transition-all cursor-pointer"
          >
            <RotateCcw size={14} />
            <span>DISCARD & START NEW RUN</span>
          </button>
        </div>
      </div>
    </div>
  );
};
