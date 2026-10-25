import React from 'react';
import { Play, Map, Calendar, Users, BarChart3, Settings, User, Trophy, Shield, Zap } from 'lucide-react';
import { AnimatedBackground } from '../components/AnimatedBackground';
import { UserAvatar } from '../components/UserAvatar';
import { UserProfile } from '../types/game';
import { soundManager } from '../services/soundManager';

interface HomeScreenProps {
  userProfile: UserProfile;
  hasSavedGame: boolean;
  onQuickPlay: () => void;
  onChooseArena: () => void;
  onDailyChallenge: () => void;
  onPvP: () => void;
  onProfile: () => void;
  onStats: () => void;
  onSettings: () => void;
  onResumeGame?: () => void;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({
  userProfile,
  hasSavedGame,
  onQuickPlay,
  onChooseArena,
  onDailyChallenge,
  onPvP,
  onProfile,
  onStats,
  onSettings,
  onResumeGame
}) => {
  return (
    <AnimatedBackground>
      <div className="flex-1 flex flex-col justify-between p-4 max-w-lg mx-auto w-full">
        {/* Top Header with Profile info */}
        <header className="flex items-center justify-between p-3 bg-[#12131A]/90 border border-[#1E2030] rounded-2xl backdrop-blur-md">
          <div
            onClick={() => {
              soundManager.playButtonClick();
              onProfile();
            }}
            className="flex items-center gap-3 cursor-pointer group"
          >
            <UserAvatar displayName={userProfile.displayName} photoUrl={userProfile.photoUrl} size="md" />
            <div className="flex flex-col text-left">
              <span className="text-sm font-bold font-mono text-white group-hover:text-[#2DD4BF] transition-colors">
                {userProfile.displayName}
              </span>
              <div className="flex items-center gap-2 text-[11px] font-mono">
                <span className="text-[#2DD4BF]">RATING {userProfile.stats.pvpRating}</span>
                <span className="text-[#4A5068]">|</span>
                <span className="text-[#C8D0E0]">{userProfile.stats.totalWins} WINS</span>
              </div>
            </div>
          </div>

          <button
            type="button"
            onClick={() => {
              soundManager.playButtonClick();
              onPvP();
            }}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-[#1E2030] hover:bg-[#2A2E44] border border-[#1E2030] text-xs font-mono text-white transition-all cursor-pointer"
          >
            <Users size={14} className="text-[#2DD4BF]" />
            <span>SOCIAL</span>
          </button>
        </header>

        {/* Hero Branding */}
        <div className="my-auto py-6 flex flex-col items-center text-center">
          <div className="relative flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-br from-[#1E2030] to-[#12131A] border border-[#2DD4BF]/50 mb-3 shadow-xl shadow-teal-500/20">
            <Shield size={32} className="text-[#2DD4BF]" />
            <div className="absolute -inset-1 rounded-2xl bg-[#2DD4BF]/20 blur-md -z-10" />
          </div>

          <h1 className="text-3xl sm:text-4xl font-black font-mono tracking-widest text-white">
            GAMBIT
          </h1>
          <p className="text-xs font-mono text-[#2DD4BF] tracking-widest uppercase mt-0.5">
            CALCULATED RISK // TURN-BASED WARFARE
          </p>

          {/* Quick Resume Button if active save */}
          {hasSavedGame && onResumeGame && (
            <button
              type="button"
              onClick={() => {
                soundManager.playButtonClick();
                onResumeGame();
              }}
              className="mt-4 flex items-center gap-2 px-4 py-2 rounded-full font-mono text-xs font-bold bg-amber-500/20 text-amber-300 border border-amber-500/50 hover:bg-amber-500/30 transition-all animate-pulse cursor-pointer"
            >
              <Zap size={14} />
              <span>RESUME ACTIVE SIMULATION</span>
            </button>
          )}
        </div>

        {/* Main Action Buttons Grid */}
        <div className="flex flex-col gap-2.5 w-full mb-4">
          {/* Quick Play */}
          <button
            type="button"
            onClick={() => {
              soundManager.playButtonClick();
              onQuickPlay();
            }}
            className="w-full flex items-center justify-between p-4 rounded-2xl bg-gradient-to-r from-[#2DD4BF] via-teal-400 to-[#2DD4BF] text-black font-mono font-extrabold shadow-xl shadow-teal-500/30 hover:brightness-105 transition-all cursor-pointer group"
          >
            <div className="flex items-center gap-3">
              <div className="p-2 bg-black/10 rounded-xl">
                <Play size={20} className="fill-black" />
              </div>
              <div className="text-left">
                <div className="text-sm font-black tracking-wide">QUICK DEPLOYMENT</div>
                <div className="text-[10px] font-sans font-medium text-black/80">
                  Instant mission in Ascendency Arena
                </div>
              </div>
            </div>
            <span className="text-xs font-bold tracking-widest">START &rarr;</span>
          </button>

          {/* Choose Arena / Mode Selection */}
          <button
            type="button"
            onClick={() => {
              soundManager.playButtonClick();
              onChooseArena();
            }}
            className="w-full flex items-center justify-between p-3.5 rounded-2xl bg-[#12131A] hover:bg-[#1A1C26] border border-[#1E2030] hover:border-[#2DD4BF]/50 text-white font-mono transition-all cursor-pointer group"
          >
            <div className="flex items-center gap-3">
              <div className="p-2 bg-[#1E2030] text-[#2DD4BF] rounded-xl group-hover:bg-[#2DD4BF]/20 transition-colors">
                <Map size={18} />
              </div>
              <div className="text-left">
                <div className="text-xs font-bold tracking-wider">SELECT ARENA & LEVEL</div>
                <div className="text-[10px] font-sans text-[#4A5068]">
                  Ascendency, Confrontation & Oblivion
                </div>
              </div>
            </div>
            <span className="text-xs text-[#2DD4BF] font-mono">&rarr;</span>
          </button>

          {/* Daily Challenge */}
          <button
            type="button"
            onClick={() => {
              soundManager.playButtonClick();
              onDailyChallenge();
            }}
            className="w-full flex items-center justify-between p-3.5 rounded-2xl bg-[#12131A] hover:bg-[#1A1C26] border border-[#1E2030] hover:border-amber-500/50 text-white font-mono transition-all cursor-pointer group"
          >
            <div className="flex items-center gap-3">
              <div className="p-2 bg-[#1E2030] text-amber-400 rounded-xl group-hover:bg-amber-500/20 transition-colors">
                <Calendar size={18} />
              </div>
              <div className="text-left">
                <div className="text-xs font-bold tracking-wider">DAILY CHALLENGE</div>
                <div className="text-[10px] font-sans text-[#4A5068]">
                  Seeded global daily tactical operation
                </div>
              </div>
            </div>
            <span className="text-xs text-amber-400 font-mono">&rarr;</span>
          </button>

          {/* PvP Match / Social */}
          <button
            type="button"
            onClick={() => {
              soundManager.playButtonClick();
              onPvP();
            }}
            className="w-full flex items-center justify-between p-3.5 rounded-2xl bg-[#12131A] hover:bg-[#1A1C26] border border-[#1E2030] hover:border-purple-500/50 text-white font-mono transition-all cursor-pointer group"
          >
            <div className="flex items-center gap-3">
              <div className="p-2 bg-[#1E2030] text-[#A78BFA] rounded-xl group-hover:bg-purple-500/20 transition-colors">
                <Users size={18} />
              </div>
              <div className="text-left">
                <div className="text-xs font-bold tracking-wider">PVP COMBAT & FRIENDS</div>
                <div className="text-[10px] font-sans text-[#4A5068]">
                  Challenge friends or play 2-Player local match
                </div>
              </div>
            </div>
            <span className="text-xs text-[#A78BFA] font-mono">&rarr;</span>
          </button>
        </div>

        {/* Bottom Navigation */}
        <nav className="flex items-center justify-around p-2 bg-[#12131A]/90 border border-[#1E2030] rounded-2xl backdrop-blur-md">
          <button
            type="button"
            onClick={() => {
              soundManager.playButtonClick();
              onProfile();
            }}
            className="flex flex-col items-center gap-1 p-2 text-[#C8D0E0] hover:text-[#2DD4BF] transition-colors"
          >
            <User size={18} />
            <span className="text-[10px] font-mono">PROFILE</span>
          </button>

          <button
            type="button"
            onClick={() => {
              soundManager.playButtonClick();
              onStats();
            }}
            className="flex flex-col items-center gap-1 p-2 text-[#C8D0E0] hover:text-[#2DD4BF] transition-colors"
          >
            <BarChart3 size={18} />
            <span className="text-[10px] font-mono">STATS</span>
          </button>

          <button
            type="button"
            onClick={() => {
              soundManager.playButtonClick();
              onSettings();
            }}
            className="flex flex-col items-center gap-1 p-2 text-[#C8D0E0] hover:text-[#2DD4BF] transition-colors"
          >
            <Settings size={18} />
            <span className="text-[10px] font-mono">SETTINGS</span>
          </button>
        </nav>
      </div>
    </AnimatedBackground>
  );
};
