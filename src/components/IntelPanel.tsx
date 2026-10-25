import React from 'react';
import { Radio, ArrowRight, ShieldAlert, Cpu } from 'lucide-react';
import { IntelHint } from '../types/game';

interface IntelPanelProps {
  intel: IntelHint | null;
  intelHint: string;
  intelConfidence: string;
  roundNumber: number;
  onProceedToPlacing: () => void;
}

export const IntelPanel: React.FC<IntelPanelProps> = ({
  intel,
  intelHint,
  intelConfidence,
  roundNumber,
  onProceedToPlacing
}) => {
  return (
    <div className="w-full max-w-[480px] p-4 bg-[#12131A] rounded-2xl border border-[#A78BFA]/40 shadow-2xl shadow-purple-950/30">
      {/* Header */}
      <div className="flex items-center justify-between pb-3 border-b border-[#1E2030]">
        <div className="flex items-center gap-2">
          <div className="p-2 rounded-lg bg-purple-500/10 border border-purple-500/30 text-[#A78BFA]">
            <Radio size={18} className="animate-pulse" />
          </div>
          <div>
            <h3 className="text-sm font-bold font-mono tracking-wider text-white uppercase">
              TACTICAL INTEL REPORT
            </h3>
            <p className="text-[11px] font-mono text-[#A78BFA]">
              ROUND {roundNumber} INTERCEPT
            </p>
          </div>
        </div>

        <div className="flex items-center gap-1.5 px-2 py-1 bg-purple-950/40 border border-purple-500/30 rounded-md text-[10px] font-mono text-purple-300">
          <Cpu size={12} />
          <span>SIGNALS RECON</span>
        </div>
      </div>

      {/* Main Content */}
      <div className="my-4 p-3 bg-[#0E1017] rounded-xl border border-[#1E2030] flex flex-col gap-2">
        <p className="text-sm font-sans text-white leading-relaxed">
          "{intelHint || intel?.text || 'No strategic intercepts available for this round.'}"
        </p>

        {intelConfidence && (
          <div className="flex items-center gap-1.5 text-xs font-mono text-[#A78BFA] pt-2 border-t border-[#1E2030]/80">
            <ShieldAlert size={14} className="text-amber-400 shrink-0" />
            <span className="text-[11px] font-semibold">{intelConfidence}</span>
          </div>
        )}
      </div>

      {/* Action Button */}
      <button
        type="button"
        onClick={onProceedToPlacing}
        className="w-full flex items-center justify-center gap-2 py-3 px-4 rounded-xl font-mono text-sm font-bold bg-gradient-to-r from-[#2DD4BF] to-teal-400 text-black hover:brightness-110 shadow-lg shadow-teal-500/20 transition-all cursor-pointer"
      >
        <span>ENTER DEPLOYMENT PHASE</span>
        <ArrowRight size={16} />
      </button>
    </div>
  );
};
