import React, { useCallback, useEffect, useState } from 'react';
import {
  AlertTriangle,
  ArrowLeft,
  Award,
  CloudOff,
  Globe,
  HardDrive,
  Loader2,
  Medal,
  RefreshCw,
  Trophy
} from 'lucide-react';
import { AnimatedBackground } from '../components/AnimatedBackground';
import { getTodayDateString } from '../domain/dailyChallenge';
import { getDailyChallengeSubmission, getStoredProfile } from '../services/storage';
import { GlobalLeaderboardEntry, fetchGlobalLeaderboard, tierForElo } from '../services/leaderboard';
import { supabase } from '../lib/supabase';
import { useSupabaseSession } from '../hooks/useSupabaseSession';
import { soundManager } from '../services/soundManager';

interface LeaderboardScreenProps {
  onBack: () => void;
}

type Tab = 'global' | 'local';

type GlobalState =
  | { status: 'loading' }
  | { status: 'ready'; entries: GlobalLeaderboardEntry[] }
  | { status: 'error'; message: string };

interface LocalEntry {
  name: string;
  score: number;
  aiScore: number;
  badge: string;
  isUser: boolean;
}

/** Device-only standings: seeded community rows plus the player's daily submission. */
function buildLocalEntries(): LocalEntry[] {
  const profile = getStoredProfile();
  const submission = getDailyChallengeSubmission(getTodayDateString());

  const entries: LocalEntry[] = [
    { name: 'Vortex_Prime', score: 142, aiScore: 68, badge: 'Grandmaster', isUser: false },
    { name: 'Aegis_Tactician', score: 136, aiScore: 74, badge: 'Veteran', isUser: false },
    { name: 'Ghost_Recon', score: 128, aiScore: 82, badge: 'Veteran', isUser: false },
    { name: 'NovaStrike', score: 122, aiScore: 88, badge: 'Specialist', isUser: false },
    { name: 'CipherZero', score: 118, aiScore: 92, badge: 'Specialist', isUser: false },
    { name: 'NullSector', score: 110, aiScore: 95, badge: 'Specialist', isUser: false },
    { name: 'QuantumPulse', score: 104, aiScore: 98, badge: 'Operator', isUser: false }
  ];

  if (submission) {
    entries.push({
      name: profile.displayName,
      score: submission.playerScore,
      aiScore: submission.aiScore,
      badge: 'Contender',
      isUser: true
    });
  }

  return entries.sort((a, b) => b.score - a.score);
}

function rankIcon(rank: number) {
  if (rank === 1) return <Trophy size={16} className="text-amber-400" />;
  if (rank === 2) return <Medal size={16} className="text-slate-300" />;
  if (rank === 3) return <Award size={16} className="text-amber-600" />;
  return <span className="font-mono font-bold text-xs">{rank}</span>;
}

interface StandingRowProps {
  rank: number;
  name: string;
  subtitle: string;
  primary: string;
  secondary: string;
  isUser: boolean;
}

const StandingRow: React.FC<StandingRowProps> = ({ rank, name, subtitle, primary, secondary, isUser }) => (
  <div
    className={`flex items-center justify-between p-3 rounded-xl border font-mono transition-all ${
      isUser
        ? 'bg-[#2DD4BF]/10 border-[#2DD4BF] text-white ring-1 ring-[#2DD4BF]/50'
        : rank === 1
        ? 'bg-amber-500/10 border-amber-500/30 text-white'
        : 'bg-[#12131A] border-[#1E2030] text-[#C8D0E0]'
    }`}
  >
    <div className="flex items-center gap-3 min-w-0">
      <div className="w-7 h-7 rounded-lg bg-[#0E1017] flex items-center justify-center shrink-0">{rankIcon(rank)}</div>
      <div className="flex flex-col text-left min-w-0">
        <span className={`text-xs font-bold truncate ${isUser ? 'text-[#2DD4BF]' : 'text-white'}`}>
          {name}
          {isUser && ' (YOU)'}
        </span>
        <span className="text-[9px] text-[#4A5068] truncate">{subtitle}</span>
      </div>
    </div>

    <div className="flex flex-col items-end shrink-0 pl-2">
      <span className="text-sm font-black text-white">{primary}</span>
      <span className="text-[9px] text-[#4A5068]">{secondary}</span>
    </div>
  </div>
);

const GlobalStandings: React.FC<{ state: GlobalState; userId?: string; onRetry: () => void }> = ({
  state,
  userId,
  onRetry
}) => {
  if (!supabase) {
    return (
      <div className="p-4 bg-[#12131A] rounded-2xl border border-[#1E2030] font-mono">
        <div className="flex items-center gap-2 mb-2">
          <CloudOff size={14} className="text-[#4A5068]" />
          <span className="text-[10px] text-[#4A5068] uppercase font-bold">OFFLINE</span>
        </div>
        <p className="text-[11px] text-[#4A5068]">
          Online uplink not configured. Switch to THIS DEVICE to see local standings.
        </p>
      </div>
    );
  }

  if (state.status === 'loading') {
    return (
      <div className="flex flex-col items-center justify-center gap-2 py-10 font-mono">
        <Loader2 size={20} className="text-[#2DD4BF] animate-spin" />
        <span className="text-[11px] text-[#4A5068]">Syncing global standings...</span>
      </div>
    );
  }

  if (state.status === 'error') {
    return (
      <div className="p-4 bg-[#12131A] rounded-2xl border border-red-500/40 font-mono">
        <div className="flex items-center gap-2 mb-2">
          <AlertTriangle size={14} className="text-red-400" />
          <span className="text-[10px] text-red-400 uppercase font-bold">UPLINK FAILED</span>
        </div>
        <p className="text-[11px] text-[#C8D0E0] mb-3 break-words">{state.message}</p>
        <button
          type="button"
          onClick={() => {
            soundManager.playButtonClick();
            onRetry();
          }}
          className="w-full flex items-center justify-center gap-1.5 py-2 rounded-xl bg-[#0E1017] hover:bg-[#1E2030] border border-[#1E2030] text-white text-[11px] font-bold transition-all cursor-pointer"
        >
          <RefreshCw size={12} className="text-[#2DD4BF]" />
          RETRY
        </button>
      </div>
    );
  }

  if (state.entries.length === 0) {
    return (
      <div className="p-4 bg-[#12131A] rounded-2xl border border-[#1E2030] font-mono">
        <p className="text-[11px] text-[#4A5068]">No ranked commanders yet. Win a match to claim the top spot.</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-2">
      {state.entries.map((e, idx) => (
        <StandingRow
          key={e.userId}
          rank={idx + 1}
          name={e.name}
          subtitle={tierForElo(e.elo)}
          primary={`${e.highScore} pts`}
          secondary={`ELO ${e.elo}`}
          isUser={!!userId && e.userId === userId}
        />
      ))}
    </div>
  );
};

export const LeaderboardScreen: React.FC<LeaderboardScreenProps> = ({ onBack }) => {
  const { session } = useSupabaseSession();
  const [tab, setTab] = useState<Tab>(supabase ? 'global' : 'local');
  const [global, setGlobal] = useState<GlobalState>({ status: 'loading' });

  const today = getTodayDateString();
  const localEntries = buildLocalEntries();

  const loadGlobal = useCallback(async () => {
    if (!supabase) return;
    setGlobal({ status: 'loading' });
    try {
      setGlobal({ status: 'ready', entries: await fetchGlobalLeaderboard(supabase) });
    } catch (err) {
      setGlobal({
        status: 'error',
        message: err instanceof Error ? err.message : 'Unable to load global standings'
      });
    }
  }, []);

  useEffect(() => {
    if (tab === 'global') loadGlobal();
  }, [tab, loadGlobal]);

  const switchTab = (next: Tab) => {
    if (next === tab) return;
    soundManager.playButtonClick();
    setTab(next);
  };

  const tabs: { id: Tab; label: string; Icon: typeof Globe }[] = [
    { id: 'global', label: 'GLOBAL', Icon: Globe },
    { id: 'local', label: 'THIS DEVICE', Icon: HardDrive }
  ];

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

          <h2 className="text-sm font-black font-mono tracking-widest text-white uppercase">LEADERBOARD</h2>

          <div className="w-12" />
        </header>

        {/* Content */}
        <div className="flex-1 flex flex-col py-4 overflow-y-auto">
          {/* Global / Local toggle */}
          <div className="grid grid-cols-2 gap-1 p-1 bg-[#12131A] rounded-xl border border-[#1E2030] font-mono mb-3">
            {tabs.map(({ id, label, Icon }) => {
              const active = tab === id;
              return (
                <button
                  key={id}
                  type="button"
                  onClick={() => switchTab(id)}
                  className={`flex items-center justify-center gap-1.5 py-1.5 rounded-lg text-[10px] font-bold transition-all cursor-pointer ${
                    active ? 'bg-[#2DD4BF] text-black' : 'text-[#4A5068] hover:text-white'
                  }`}
                >
                  <Icon size={12} />
                  {label}
                </button>
              );
            })}
          </div>

          <div className="flex items-center justify-between px-2 py-1 mb-3 text-xs font-mono text-[#4A5068]">
            <span>{tab === 'global' ? 'GLOBAL STANDINGS' : `OPERATION: ${today}`}</span>
            {tab === 'global' && supabase ? (
              <button
                type="button"
                onClick={() => {
                  soundManager.playButtonClick();
                  loadGlobal();
                }}
                disabled={global.status === 'loading'}
                className="flex items-center gap-1 text-[10px] text-[#4A5068] hover:text-[#2DD4BF] disabled:opacity-50 transition-all cursor-pointer"
              >
                <RefreshCw size={11} className={global.status === 'loading' ? 'animate-spin' : ''} />
                REFRESH
              </button>
            ) : (
              <span>RANKED STANDINGS</span>
            )}
          </div>

          {tab === 'local' ? (
            <div className="flex flex-col gap-2">
              {localEntries.map((e, idx) => (
                <StandingRow
                  key={e.name + idx}
                  rank={idx + 1}
                  name={e.name}
                  subtitle={e.badge}
                  primary={`${e.score} pts`}
                  secondary={`vs AI ${e.aiScore}`}
                  isUser={e.isUser}
                />
              ))}
            </div>
          ) : (
            <GlobalStandings state={global} userId={session?.user.id} onRetry={loadGlobal} />
          )}
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
