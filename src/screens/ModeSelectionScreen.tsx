import React, { useState } from 'react';
import { ArrowLeft, Play, Radio, Eye, Lock, Shield, Cpu, Zap, AlertTriangle } from 'lucide-react';
import { AnimatedBackground } from '../components/AnimatedBackground';
import { ArenaId, GameLevelId } from '../types/game';
import { ARENA_INFOS, getArenaConfig } from '../domain/arenaConfig';
import { soundManager } from '../services/soundManager';

interface ModeSelectionScreenProps {
  onBack: () => void;
  onPlay: (arena: ArenaId, level: GameLevelId) => void;
}

export const ModeSelectionScreen: React.FC<ModeSelectionScreenProps> = ({ onBack, onPlay }) => {
  const [selectedArena, setSelectedArena] = useState<ArenaId>('ASCENDENCY');
  const [selectedLevel, setSelectedLevel] = useState<GameLevelId>('LEVEL_1');

  const arenas: ArenaId[] = ['ASCENDENCY', 'CONFRONTATION', 'OBLIVION'];
  const levels: { id: GameLevelId; label: string; sub: string }[] = [
    { id: 'LEVEL_1', label: 'ROOKIE', sub: 'Standard difficulty' },
    { id: 'LEVEL_2', label: 'VETERAN', sub: 'Calculated opponent' },
    { id: 'LEVEL_3', label: 'MASTER', sub: 'Aggressive & unforgiving' }
  ];

  const currentConfig = getArenaConfig(selectedArena, selectedLevel);
  const currentInfo = ARENA_INFOS[selectedArena];

  const handleStart = () => {
    soundManager.playButtonClick();
    onPlay(selectedArena, selectedLevel);
  };

  return (
    <AnimatedBackground>
      <div className="flex-1 flex flex-col justify-between p-4 max-w-lg mx-auto w-full">
        {/* Header */}
        <header className="flex items-center justify-between pb-3 border-b border-[#1E2030]">
          <button
            type="button"
            onClick={() => {
              soundManager.playButtonClick();
              onBack();
            }}
            className="flex items-center gap-1 px-3 py-1.5 rounded-xl bg-[#12131A] hover:bg-[#1E2030] border border-[#1E2030] text-xs font-mono text-white transition-all cursor-pointer"
          >
            <ArrowLeft size={16} className="text-[#2DD4BF]" />
            <span>BACK</span>
          </button>

          <h2 className="text-sm font-black font-mono tracking-widest text-white uppercase">
            CHOOSE ARENA
          </h2>

          <div className="w-12" />
        </header>

        {/* Content */}
        <div className="flex-1 flex flex-col gap-4 py-4 overflow-y-auto">
          {/* Arena Tabs */}
          <div className="flex flex-col gap-2">
            <span className="text-[10px] font-mono font-bold text-[#4A5068] uppercase tracking-wider">
              1. THEATER OF OPERATIONS
            </span>
            <div className="grid grid-cols-3 gap-2">
              {arenas.map(ar => {
                const info = ARENA_INFOS[ar];
                const isSelected = selectedArena === ar;
                return (
                  <button
                    key={ar}
                    type="button"
                    onClick={() => {
                      soundManager.playButtonClick();
                      setSelectedArena(ar);
                    }}
                    className={`flex flex-col items-center justify-center p-3 rounded-2xl border text-center transition-all cursor-pointer ${
                      isSelected
                        ? 'bg-[#1E2030] border-[#2DD4BF] text-white ring-2 ring-[#2DD4BF]/40 shadow-lg shadow-teal-500/20'
                        : 'bg-[#12131A] border-[#1E2030] text-[#C8D0E0] hover:border-[#4A5068]'
                    }`}
                  >
                    <span className="text-xs font-mono font-black">{info.displayName}</span>
                    <span className="text-[9px] font-mono text-[#2DD4BF] mt-0.5">{info.tagline}</span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Arena Description card */}
          <div className="p-3 bg-[#12131A] rounded-xl border border-[#1E2030]">
            <p className="text-xs font-sans text-white leading-relaxed">
              {currentInfo.description}
            </p>
          </div>

          {/* Difficulty Level Tabs */}
          <div className="flex flex-col gap-2">
            <span className="text-[10px] font-mono font-bold text-[#4A5068] uppercase tracking-wider">
              2. ENGAGEMENT DIFFICULTY
            </span>
            <div className="grid grid-cols-3 gap-2">
              {levels.map(lvl => {
                const isSelected = selectedLevel === lvl.id;
                return (
                  <button
                    key={lvl.id}
                    type="button"
                    onClick={() => {
                      soundManager.playButtonClick();
                      setSelectedLevel(lvl.id);
                    }}
                    className={`flex flex-col items-center justify-center p-2.5 rounded-2xl border text-center transition-all cursor-pointer ${
                      isSelected
                        ? 'bg-[#1E2030] border-[#2DD4BF] text-white ring-2 ring-[#2DD4BF]/40 shadow-lg shadow-teal-500/20'
                        : 'bg-[#12131A] border-[#1E2030] text-[#C8D0E0] hover:border-[#4A5068]'
                    }`}
                  >
                    <span className="text-xs font-mono font-extrabold">{lvl.label}</span>
                    <span className="text-[9px] font-sans text-[#4A5068] mt-0.5">{lvl.sub}</span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Active Configuration Tactical Parameters */}
          <div className="p-4 bg-[#12131A] rounded-2xl border border-[#1E2030]">
            <div className="flex items-center gap-2 mb-3 pb-2 border-b border-[#1E2030]">
              <Shield size={16} className="text-[#2DD4BF]" />
              <h3 className="text-xs font-bold font-mono text-white tracking-wider uppercase">
                TACTICAL PARAMETERS
              </h3>
            </div>

            <div className="grid grid-cols-2 gap-2.5 font-mono text-xs">
              <div className="flex items-center justify-between p-2 bg-[#0E1017] rounded-lg">
                <span className="text-[#4A5068] text-[11px] flex items-center gap-1">
                  <Radio size={12} /> INTEL HINTS
                </span>
                <span className={currentConfig.intelEnabled ? 'text-[#2DD4BF] font-bold' : 'text-red-400 font-bold'}>
                  {currentConfig.intelEnabled ? 'ONLINE' : 'BLIND'}
                </span>
              </div>

              <div className="flex items-center justify-between p-2 bg-[#0E1017] rounded-lg">
                <span className="text-[#4A5068] text-[11px] flex items-center gap-1">
                  <Eye size={12} /> SCOUT DRONE
                </span>
                <span className={currentConfig.scoutEnabled ? 'text-[#2DD4BF] font-bold' : 'text-red-400 font-bold'}>
                  {currentConfig.scoutEnabled ? 'AVAILABLE' : 'OFFLINE'}
                </span>
              </div>

              <div className="flex items-center justify-between p-2 bg-[#0E1017] rounded-lg">
                <span className="text-[#4A5068] text-[11px] flex items-center gap-1">
                  <Lock size={12} /> MAX LOCKS
                </span>
                <span className="text-white font-bold">{currentConfig.maxLocksPerRound}/ROUND</span>
              </div>

              <div className="flex items-center justify-between p-2 bg-[#0E1017] rounded-lg">
                <span className="text-[#4A5068] text-[11px] flex items-center gap-1">
                  <Cpu size={12} /> AI RANGE
                </span>
                <span className="text-[#A78BFA] font-bold">
                  [{currentConfig.aiCapabilityMin}, +{currentConfig.aiCapabilityMax}]
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Start Game Button */}
        <button
          type="button"
          onClick={handleStart}
          className="w-full flex items-center justify-center gap-2 py-4 px-6 rounded-2xl bg-gradient-to-r from-[#2DD4BF] to-teal-400 hover:brightness-110 text-black font-mono text-sm font-black shadow-xl shadow-teal-500/30 transition-all cursor-pointer"
        >
          <Play size={18} className="fill-black" />
          <span>START OPERATION ({ARENA_INFOS[selectedArena].displayName})</span>
        </button>
      </div>
    </AnimatedBackground>
  );
};
