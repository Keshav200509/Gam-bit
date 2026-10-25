import React, { useState } from 'react';
import { ArrowLeft, Trophy, BarChart3, History, Award, CheckCircle2, Lock, Trash2 } from 'lucide-react';
import { AnimatedBackground } from '../components/AnimatedBackground';
import { getStoredHistory, getStoredProfile, clearStoredHistory } from '../services/storage';
import { ALL_ACHIEVEMENTS } from '../domain/achievements';
import { soundManager } from '../services/soundManager';

interface StatsScreenProps {
  onBack: () => void;
}

export const StatsScreen: React.FC<StatsScreenProps> = ({ onBack }) => {
  const [tab, setTab] = useState<'overview' | 'achievements' | 'history'>('overview');
  const [history, setHistory] = useState(getStoredHistory());
  const profile = getStoredProfile();

  const totalGames = profile.stats.totalGames;
  const totalWins = profile.stats.totalWins;
  const winRate = totalGames > 0 ? Math.round((totalWins / totalGames) * 100) : 0;

  const totalClashes = profile.stats.clashesWon + profile.stats.clashesLost;
  const clashRate = totalClashes > 0 ? Math.round((profile.stats.clashesWon / totalClashes) * 100) : 0;

  const unlockedIds = new Set(profile.stats.unlockedAchievementIds);

  const handleClearHistory = () => {
    if (window.confirm('Clear all historical match records?')) {
      clearStoredHistory();
      setHistory([]);
    }
  };

  return (
    <AnimatedBackground>
      <div className="flex-1 flex flex-col justify-between p-4 max-w-md mx-auto w-full">
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
            CAREER DOSSIER
          </h2>

          <div className="w-12" />
        </header>

        {/* Navigation Tabs */}
        <div className="grid grid-cols-3 gap-2 my-3">
          <button
            type="button"
            onClick={() => setTab('overview')}
            className={`py-2 px-3 rounded-xl font-mono text-xs font-bold border transition-all cursor-pointer ${
              tab === 'overview'
                ? 'bg-[#1E2030] border-[#2DD4BF] text-white shadow-lg shadow-teal-500/10'
                : 'bg-[#12131A] border-[#1E2030] text-[#4A5068]'
            }`}
          >
            OVERVIEW
          </button>
          <button
            type="button"
            onClick={() => setTab('achievements')}
            className={`py-2 px-3 rounded-xl font-mono text-xs font-bold border transition-all cursor-pointer ${
              tab === 'achievements'
                ? 'bg-[#1E2030] border-[#2DD4BF] text-white shadow-lg shadow-teal-500/10'
                : 'bg-[#12131A] border-[#1E2030] text-[#4A5068]'
            }`}
          >
            MEDALS ({unlockedIds.size}/{ALL_ACHIEVEMENTS.length})
          </button>
          <button
            type="button"
            onClick={() => setTab('history')}
            className={`py-2 px-3 rounded-xl font-mono text-xs font-bold border transition-all cursor-pointer ${
              tab === 'history'
                ? 'bg-[#1E2030] border-[#2DD4BF] text-white shadow-lg shadow-teal-500/10'
                : 'bg-[#12131A] border-[#1E2030] text-[#4A5068]'
            }`}
          >
            LOGS ({history.length})
          </button>
        </div>

        {/* Tab Content */}
        <div className="flex-1 overflow-y-auto py-2">
          {tab === 'overview' && (
            <div className="flex flex-col gap-3 font-mono">
              {/* Primary Win Rate Hero */}
              <div className="p-4 bg-[#12131A] rounded-2xl border border-[#1E2030] flex items-center justify-between">
                <div>
                  <span className="text-[10px] text-[#4A5068] uppercase font-bold block">
                    TOTAL OPERATIONS WON
                  </span>
                  <div className="flex items-baseline gap-2 mt-1">
                    <span className="text-3xl font-black text-white">{totalWins}</span>
                    <span className="text-xs text-[#4A5068]">/ {totalGames} GAMES</span>
                  </div>
                </div>
                <div className="flex flex-col items-end">
                  <span className="text-2xl font-black text-[#2DD4BF]">{winRate}%</span>
                  <span className="text-[9px] text-[#4A5068]">VICTORY RATE</span>
                </div>
              </div>

              {/* Grid Metrics */}
              <div className="grid grid-cols-2 gap-2.5">
                <div className="p-3 bg-[#12131A] rounded-xl border border-[#1E2030]">
                  <span className="text-[10px] text-[#4A5068] block">CLASH VICTORIES</span>
                  <span className="text-lg font-bold text-white mt-1 block">
                    {profile.stats.clashesWon}
                  </span>
                  <span className="text-[9px] text-[#2DD4BF]">{clashRate}% clash rate</span>
                </div>

                <div className="p-3 bg-[#12131A] rounded-xl border border-[#1E2030]">
                  <span className="text-[10px] text-[#4A5068] block">TACTICAL RATING</span>
                  <span className="text-lg font-bold text-[#A78BFA] mt-1 block">
                    {profile.stats.pvpRating}
                  </span>
                  <span className="text-[9px] text-[#4A5068]">Rank tier: Veteran</span>
                </div>

                <div className="p-3 bg-[#12131A] rounded-xl border border-[#1E2030]">
                  <span className="text-[10px] text-[#4A5068] block">RECON DISPATCHES</span>
                  <span className="text-lg font-bold text-white mt-1 block">
                    {profile.stats.scoutEffectiveCount}
                  </span>
                  <span className="text-[9px] text-emerald-400">Scouted clear</span>
                </div>

                <div className="p-3 bg-[#12131A] rounded-xl border border-[#1E2030]">
                  <span className="text-[10px] text-[#4A5068] block">INTEL TRUST COUNT</span>
                  <span className="text-lg font-bold text-white mt-1 block">
                    {profile.stats.intelAnalystCount}
                  </span>
                  <span className="text-[9px] text-purple-400">Accurate intel wins</span>
                </div>
              </div>
            </div>
          )}

          {tab === 'achievements' && (
            <div className="flex flex-col gap-2 font-mono">
              {ALL_ACHIEVEMENTS.map(ach => {
                const isUnlocked = unlockedIds.has(ach.id);
                return (
                  <div
                    key={ach.id}
                    className={`flex items-start gap-3 p-3 rounded-xl border transition-all ${
                      isUnlocked
                        ? 'bg-[#12131A] border-[#2DD4BF]/40 text-white'
                        : 'bg-[#12131A]/40 border-[#1E2030] text-[#4A5068] opacity-60'
                    }`}
                  >
                    <div
                      className={`p-2 rounded-xl mt-0.5 ${
                        isUnlocked
                          ? 'bg-teal-500/10 text-[#2DD4BF] border border-teal-500/30'
                          : 'bg-[#1E2030] text-[#4A5068]'
                      }`}
                    >
                      {isUnlocked ? <Trophy size={16} /> : <Lock size={16} />}
                    </div>

                    <div className="flex-1 text-left">
                      <div className="flex items-center justify-between">
                        <span className={`text-xs font-bold ${isUnlocked ? 'text-white' : 'text-[#4A5068]'}`}>
                          {ach.title}
                        </span>
                        {isUnlocked && (
                          <span className="text-[9px] text-[#2DD4BF] flex items-center gap-0.5 font-bold">
                            <CheckCircle2 size={10} /> UNLOCKED
                          </span>
                        )}
                      </div>
                      <p className="text-[11px] font-sans text-[#C8D0E0] mt-1 leading-snug">
                        {ach.description}
                      </p>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {tab === 'history' && (
            <div className="flex flex-col gap-2 font-mono">
              {history.length > 0 && (
                <div className="flex justify-end mb-1">
                  <button
                    type="button"
                    onClick={handleClearHistory}
                    className="flex items-center gap-1 text-[10px] text-red-400 hover:text-red-300 cursor-pointer"
                  >
                    <Trash2 size={12} />
                    <span>CLEAR LOGS</span>
                  </button>
                </div>
              )}

              {history.length === 0 ? (
                <div className="p-8 text-center text-xs text-[#4A5068]">
                  No historical mission logs on record.
                </div>
              ) : (
                history.map(item => (
                  <div
                    key={item.id}
                    className="p-3 bg-[#12131A] rounded-xl border border-[#1E2030] flex items-center justify-between"
                  >
                    <div className="flex flex-col text-left">
                      <span className="text-xs font-bold text-white">
                        {item.arena} // {item.level.replace('_', ' ')}
                      </span>
                      <span className="text-[10px] text-[#4A5068]">
                        {item.date} &bull; {item.roundsPlayed} Rounds
                      </span>
                    </div>

                    <div className="flex flex-col items-end">
                      <span
                        className={`text-xs font-black ${
                          item.result === 'WIN' ? 'text-[#2DD4BF]' : 'text-red-400'
                        }`}
                      >
                        {item.result} ({item.playerScore} - {item.aiScore})
                      </span>
                      <span className="text-[9px] text-[#4A5068]">
                        Won {item.clashesWon} clashes
                      </span>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}
        </div>

        {/* Back action */}
        <button
          type="button"
          onClick={() => {
            soundManager.playButtonClick();
            onBack();
          }}
          className="w-full py-3 px-4 rounded-xl bg-[#12131A] hover:bg-[#1E2030] text-white border border-[#1E2030] font-mono text-xs font-bold transition-all cursor-pointer mt-2"
        >
          RETURN TO MAIN BASE
        </button>
      </div>
    </AnimatedBackground>
  );
};
