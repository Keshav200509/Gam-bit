import React from 'react';
import { Volume2, VolumeX, Clock, ArrowLeft, Settings, ShieldAlert, Cpu } from 'lucide-react';
import { AICapabilityName, ArenaId, GameLevelId } from '../types/game';

interface StatusBarProps {
  playerScore: number;
  aiScore: number;
  playerIncomeDelta?: number;
  aiIncomeDelta?: number;
  roundNumber: number;
  timeRemaining: number;
  timerActive: boolean;
  aiCapabilityName: AICapabilityName;
  aiCapabilityModifier: number;
  aiCapabilityVisible: boolean;
  arena: ArenaId;
  level: GameLevelId;
  soundEnabled: boolean;
  onToggleSound: () => void;
  onOpenSettings: () => void;
  onExit: () => void;
}

export const StatusBar: React.FC<StatusBarProps> = ({
  playerScore,
  aiScore,
  playerIncomeDelta,
  aiIncomeDelta,
  roundNumber,
  timeRemaining,
  timerActive,
  aiCapabilityName,
  aiCapabilityModifier,
  aiCapabilityVisible,
  arena,
  level,
  soundEnabled,
  onToggleSound,
  onOpenSettings,
  onExit
}) => {
  const capabilityColors: Record<AICapabilityName, { bg: string; text: string; border: string }> = {
    PEAK: { bg: 'bg-amber-500/10', text: 'text-amber-400', border: 'border-amber-500/30' },
    SHARP: { bg: 'bg-red-500/10', text: 'text-red-400', border: 'border-red-500/30' },
    STEADY: { bg: 'bg-rose-500/10', text: 'text-rose-400', border: 'border-rose-500/30' },
    SLOPPY: { bg: 'bg-purple-500/10', text: 'text-purple-400', border: 'border-purple-500/30' }
  };

  const capStyle = capabilityColors[aiCapabilityName] || capabilityColors.STEADY;

  return (
    <header className="w-full flex flex-col gap-2 p-3 bg-[#0A0B10]/95 border-b border-[#1E2030] backdrop-blur-md">
      {/* Top action row */}
      <div className="flex items-center justify-between">
        <button
          onClick={onExit}
          className="flex items-center gap-1.5 px-2.5 py-1 text-xs font-mono font-medium text-[#C8D0E0] hover:text-white bg-[#12131A] hover:bg-[#1E2030] border border-[#1E2030] rounded-lg transition-colors"
          title="Back to Menu"
        >
          <ArrowLeft size={14} className="text-[#2DD4BF]" />
          <span>ESC</span>
        </button>

        <div className="flex items-center gap-2">
          <span className="text-xs font-bold font-mono tracking-widest text-[#2DD4BF]">
            {arena}
          </span>
          <span className="text-[10px] px-1.5 py-0.5 rounded font-mono bg-[#1E2030] text-[#A78BFA]">
            {level.replace('_', ' ')}
          </span>
          <span className="text-xs font-mono text-[#4A5068]">|</span>
          <span className="text-xs font-mono text-white font-semibold">
            ROUND {roundNumber}/12
          </span>
        </div>

        <div className="flex items-center gap-2">
          {aiCapabilityVisible && (
            <div
              className={`hidden sm:flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-mono border ${capStyle.bg} ${capStyle.text} ${capStyle.border}`}
              title={`AI Modifier: ${aiCapabilityModifier > 0 ? '+' : ''}${aiCapabilityModifier}`}
            >
              <Cpu size={12} />
              <span>{aiCapabilityName}</span>
              <span className="text-[9px] opacity-75">
                ({aiCapabilityModifier > 0 ? `+${aiCapabilityModifier}` : aiCapabilityModifier})
              </span>
            </div>
          )}

          <button
            onClick={onToggleSound}
            className="p-1.5 rounded-lg bg-[#12131A] hover:bg-[#1E2030] border border-[#1E2030] text-[#C8D0E0] hover:text-white transition-colors"
            title={soundEnabled ? 'Mute Audio' : 'Unmute Audio'}
          >
            {soundEnabled ? <Volume2 size={16} /> : <VolumeX size={16} className="text-red-400" />}
          </button>

          <button
            onClick={onOpenSettings}
            className="p-1.5 rounded-lg bg-[#12131A] hover:bg-[#1E2030] border border-[#1E2030] text-[#C8D0E0] hover:text-white transition-colors"
            title="Settings"
          >
            <Settings size={16} />
          </button>
        </div>
      </div>

      {/* Main Score & Timer Display */}
      <div className="grid grid-cols-3 items-center px-2 py-1.5 bg-[#12131A] rounded-xl border border-[#1E2030]">
        {/* Player Score */}
        <div className="flex flex-col items-start pl-2">
          <span className="text-[10px] font-mono font-bold tracking-wider text-[#2DD4BF] uppercase">
            YOU (PLAYER)
          </span>
          <div className="flex items-baseline gap-1.5">
            <span className="text-2xl sm:text-3xl font-extrabold font-mono text-white tracking-tight">
              {playerScore}
            </span>
            {playerIncomeDelta !== undefined && playerIncomeDelta > 0 && (
              <span className="text-xs font-mono font-bold text-teal-400 animate-bounce">
                +{playerIncomeDelta}
              </span>
            )}
          </div>
        </div>

        {/* Center Timer / Round Status */}
        <div className="flex flex-col items-center justify-center">
          {timerActive ? (
            <div
              className={`flex items-center gap-1.5 px-3 py-1 rounded-full font-mono text-xs font-bold transition-colors ${
                timeRemaining <= 10
                  ? 'bg-red-500/20 text-red-400 border border-red-500/50 animate-pulse'
                  : 'bg-[#1E2030] text-[#C8D0E0] border border-[#1E2030]'
              }`}
            >
              <Clock size={12} className={timeRemaining <= 10 ? 'text-red-400' : 'text-[#2DD4BF]'} />
              <span>{timeRemaining}s</span>
            </div>
          ) : (
            <div className="flex items-center gap-1.5 text-xs font-mono text-[#4A5068]">
              <span className="w-2 h-2 rounded-full bg-[#2DD4BF] animate-ping" />
              <span>SIMULATION ACTIVE</span>
            </div>
          )}
        </div>

        {/* AI Score */}
        <div className="flex flex-col items-end pr-2">
          <span className="text-[10px] font-mono font-bold tracking-wider text-[#EF4444] uppercase">
            OPPONENT (AI)
          </span>
          <div className="flex items-baseline gap-1.5">
            {aiIncomeDelta !== undefined && aiIncomeDelta > 0 && (
              <span className="text-xs font-mono font-bold text-red-400 animate-bounce">
                +{aiIncomeDelta}
              </span>
            )}
            <span className="text-2xl sm:text-3xl font-extrabold font-mono text-white tracking-tight">
              {aiScore}
            </span>
          </div>
        </div>
      </div>
    </header>
  );
};
