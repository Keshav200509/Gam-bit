import React from 'react';
import { ArrowLeft, Calendar, Play, Trophy, CheckCircle, Shield } from 'lucide-react';
import { AnimatedBackground } from '../components/AnimatedBackground';
import { generateDailyChallenge, getTodayDateString } from '../domain/dailyChallenge';
import { getDailyChallengeSubmission } from '../services/storage';
import { ArenaId, GameLevelId } from '../types/game';
import { ARENA_INFOS } from '../domain/arenaConfig';
import { soundManager } from '../services/soundManager';

interface DailyChallengeScreenProps {
  onBack: () => void;
  onStartChallenge: (arena: ArenaId, level: GameLevelId, seed: number) => void;
  onViewLeaderboard: () => void;
}

export const DailyChallengeScreen: React.FC<DailyChallengeScreenProps> = ({
  onBack,
  onStartChallenge,
  onViewLeaderboard
}) => {
  const today = getTodayDateString();
  const challenge = generateDailyChallenge(today);
  const previousSubmission = getDailyChallengeSubmission(today);
  const arenaInfo = ARENA_INFOS[challenge.arena];

  const handleLaunch = () => {
    soundManager.playButtonClick();
    onStartChallenge(challenge.arena, challenge.level, challenge.boardSeed);
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
            DAILY CHALLENGE
          </h2>

          <button
            type="button"
            onClick={() => {
              soundManager.playButtonClick();
              onViewLeaderboard();
            }}
            className="p-1.5 rounded-xl bg-[#12131A] hover:bg-[#1E2030] border border-[#1E2030] text-amber-400 cursor-pointer"
            title="Leaderboard"
          >
            <Trophy size={16} />
          </button>
        </header>

        {/* Content */}
        <div className="my-auto py-6 flex flex-col items-center text-center">
          <div className="flex items-center justify-center w-16 h-16 rounded-2xl bg-amber-500/10 border-2 border-amber-400/40 mb-3 shadow-xl shadow-amber-500/20">
            <Calendar size={32} className="text-amber-400" />
          </div>

          <span className="text-[10px] font-mono text-amber-400 font-bold uppercase tracking-widest">
            SYNCHRONIZED DAILY SEED
          </span>

          <h3 className="text-2xl font-black font-mono text-white mt-1">
            {today}
          </h3>

          {/* Operation Briefing */}
          <div className="w-full p-5 bg-[#12131A] rounded-2xl border border-[#1E2030] shadow-xl my-6 text-left">
            <div className="flex items-center justify-between mb-3 pb-2 border-b border-[#1E2030]">
              <div className="flex items-center gap-2">
                <Shield size={16} className="text-[#2DD4BF]" />
                <span className="text-xs font-mono font-bold text-white uppercase">
                  OPERATION SPECIFICATIONS
                </span>
              </div>
              <span className="text-[10px] font-mono text-[#2DD4BF] bg-teal-500/10 px-2 py-0.5 rounded-full border border-teal-500/30">
                OFFICIAL RUN
              </span>
            </div>

            <div className="grid grid-cols-2 gap-3 text-xs font-mono mb-4">
              <div className="p-2.5 bg-[#0E1017] rounded-xl border border-[#1E2030]">
                <span className="text-[#4A5068] text-[10px] block">THEATER</span>
                <span className="text-white font-bold">{arenaInfo.displayName}</span>
              </div>
              <div className="p-2.5 bg-[#0E1017] rounded-xl border border-[#1E2030]">
                <span className="text-[#4A5068] text-[10px] block">DIFFICULTY</span>
                <span className="text-amber-400 font-bold">VETERAN (LEVEL 2)</span>
              </div>
            </div>

            <p className="text-xs font-sans text-[#C8D0E0] leading-relaxed">
              Every commander across the globe encounters the exact same board generation and strategic AI profile. Your score will be permanently posted to today’s global leaderboard.
            </p>

            {previousSubmission && (
              <div className="mt-4 p-3 bg-emerald-500/10 border border-emerald-500/30 rounded-xl flex items-center gap-2 text-xs font-mono text-emerald-300">
                <CheckCircle size={16} className="text-emerald-400 shrink-0" />
                <span>
                  ALREADY COMPLETED TODAY: Score {previousSubmission.playerScore} vs AI {previousSubmission.aiScore}
                </span>
              </div>
            )}
          </div>
        </div>

        {/* Buttons */}
        <div className="flex flex-col gap-2.5 w-full pb-2">
          <button
            type="button"
            onClick={handleLaunch}
            className="w-full flex items-center justify-center gap-2 py-4 px-6 rounded-2xl bg-gradient-to-r from-amber-400 to-amber-500 hover:brightness-110 text-black font-mono text-xs font-black shadow-xl shadow-amber-500/30 transition-all cursor-pointer"
          >
            <Play size={16} className="fill-black" />
            <span>{previousSubmission ? 'REPLAY DAILY OPERATION' : 'COMMENCE DAILY OPERATION'}</span>
          </button>

          <button
            type="button"
            onClick={() => {
              soundManager.playButtonClick();
              onViewLeaderboard();
            }}
            className="w-full flex items-center justify-center gap-1.5 py-3 px-4 rounded-xl bg-[#12131A] hover:bg-[#1E2030] text-[#C8D0E0] border border-[#1E2030] font-mono text-xs font-bold transition-all cursor-pointer"
          >
            <Trophy size={14} className="text-amber-400" />
            <span>VIEW TODAY’S LEADERBOARD</span>
          </button>
        </div>
      </div>
    </AnimatedBackground>
  );
};
