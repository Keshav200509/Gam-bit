import React from 'react';
import { Sparkles, Flame, Zap, Swords } from 'lucide-react';
import { LateGameEventType } from '../types/game';

interface LateGameEventBannerProps {
  type: LateGameEventType;
  message: string;
}

export const LateGameEventBanner: React.FC<LateGameEventBannerProps> = ({ type, message }) => {
  if (type === 'NONE' || !message) return null;

  const getIcon = () => {
    switch (type) {
      case 'GOLDEN_CELL':
        return <Sparkles size={16} className="text-amber-400" />;
      case 'VOLATILE':
        return <Flame size={16} className="text-orange-400" />;
      case 'DOUBLE_STAKES':
        return <Zap size={16} className="text-amber-300" />;
      case 'FINAL_GAMBIT':
        return <Swords size={16} className="text-red-400" />;
      default:
        return null;
    }
  };

  return (
    <div className="w-full max-w-[480px] px-3 py-2 bg-gradient-to-r from-amber-950/60 via-[#1E2030] to-amber-950/60 border border-amber-500/40 rounded-xl flex items-center gap-2.5 text-xs font-mono text-amber-200 shadow-lg shadow-amber-950/30 animate-pulse">
      <div className="p-1 rounded bg-amber-500/20">{getIcon()}</div>
      <span className="font-semibold">{message}</span>
    </div>
  );
};
