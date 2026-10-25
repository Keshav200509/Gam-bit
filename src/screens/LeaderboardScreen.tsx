import React from 'react';
import { ArrowLeft, Trophy, Medal, Award, User } from 'lucide-react';
import { AnimatedBackground } from '../components/AnimatedBackground';
import { getTodayDateString } from '../domain/dailyChallenge';
import { getDailyChallengeSubmission, getStoredProfile } from '../services/storage';
import { soundManager } from '../services/soundManager';

interface LeaderboardScreenProps {
  onBack: () => void;
}

export const LeaderboardScreen: React.FC<LeaderboardScreenProps> = ({ onBack }) => {
  const today = getTodayDateString();
  const profile = getStoredProfile();
  const submission = getDailyChallengeSubmission(today);

  // Seeded / mock realistic community entries + user's entry if completed
  const defaultEntries = [
    { rank: 1, name: 'Vortex_Prime', score: 142, aiScore: 68, badge: 'Grandmaster' },
    { rank: 2, name: 'Aegis_Tactician', score: 136, aiScore: 74, badge: 'Veteran' },
    { rank: 3, name: 'Ghost_Recon', score: 128, aiScore: 82, badge: 'Veteran' },
    { rank: 4, name: 'NovaStrike', score: 122, aiScore: 88, badge: 'Specialist' },
    { rank: 5, name: 'CipherZero', score: 118, aiScore: 92, badge: 'Specialist' },
    { rank: 6, name: 'NullSector', score: 110, aiScore: 95, badge: 'Specialist' },
    { rank: 7, name: 'QuantumPulse', score: 104, aiScore: 98, badge: 'Operator' }
  ];

  const entries = [...defaultEntries];
  if (submission) {
    entries.push({
      rank: submission.playerScore > 130 ? 3 : 8,
      name: profile.displayName + ' (YOU)',
      score: submission.playerScore,
      aiScore: submission.aiScore,
      badge: 'Contender'
    });
    entries.sort((a, b) => b.score - a.score);
    entries.forEach((e, idx) => {
      e.rank = idx + 1;
    });
  }

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
            DAILY LEADERBOARD
          </h2>

          <div className="w-12" />
        </header>

        {/* Content */}
        <div className="flex-1 flex flex-col py-4 overflow-y-auto">
          <div className="flex items-center justify-between px-2 py-1 mb-3 text-xs font-mono text-[#4A5068]">
            <span>OPERATION: {today}</span>
            <span>RANKED STANDINGS</span>
          </div>

          <div className="flex flex-col gap-2">
            {entries.map(e => {
              const isTop1 = e.rank === 1;
              const isTop2 = e.rank === 2;
              const isTop3 = e.rank === 3;
              const isUser = e.name.includes('(YOU)');

              let rankIcon = <span className="font-mono font-bold text-xs">{e.rank}</span>;
              if (isTop1) rankIcon = <Trophy size={16} className="text-amber-400" />;
              else if (isTop2) rankIcon = <Medal size={16} className="text-slate-300" />;
              else if (isTop3) rankIcon = <Award size={16} className="text-amber-600" />;

              return (
                <div
                  key={e.name + e.rank}
                  className={`flex items-center justify-between p-3 rounded-xl border font-mono transition-all ${
                    isUser
                      ? 'bg-[#2DD4BF]/10 border-[#2DD4BF] text-white ring-1 ring-[#2DD4BF]/50'
                      : isTop1
                      ? 'bg-amber-500/10 border-amber-500/30 text-white'
                      : 'bg-[#12131A] border-[#1E2030] text-[#C8D0E0]'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div className="w-7 h-7 rounded-lg bg-[#0E1017] flex items-center justify-center shrink-0">
                      {rankIcon}
                    </div>
                    <div className="flex flex-col text-left">
                      <span className={`text-xs font-bold ${isUser ? 'text-[#2DD4BF]' : 'text-white'}`}>
                        {e.name}
                      </span>
                      <span className="text-[9px] text-[#4A5068]">{e.badge}</span>
                    </div>
                  </div>

                  <div className="flex flex-col items-end">
                    <span className="text-sm font-black text-white">{e.score} pts</span>
                    <span className="text-[9px] text-[#4A5068]">vs AI {e.aiScore}</span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Back action */}
        <button
          type="button"
          onClick={() => {
            soundManager.playButtonClick();
            onBack();
          }}
          className="w-full py-3 px-4 rounded-xl bg-[#12131A] hover:bg-[#1E2030] text-white border border-[#1E2030] font-mono text-xs font-bold transition-all cursor-pointer"
        >
          RETURN TO MAIN BASE
        </button>
      </div>
    </AnimatedBackground>
  );
};
