import React, { useEffect, useState } from 'react';
import { CloudOff, LogIn, LogOut, RefreshCw, UserCheck } from 'lucide-react';
import type { Session } from '@supabase/supabase-js';
import { supabase } from '../lib/supabase';
import { CloudPlayerStats, syncLocalStatsToCloud } from '../services/cloudSync';
import { soundManager } from '../services/soundManager';

type SyncState =
  | { status: 'idle' }
  | { status: 'syncing' }
  | { status: 'synced'; stats: CloudPlayerStats }
  | { status: 'error'; message: string };

interface AccountPanelProps {
  session: Session | null;
  loading: boolean;
}

const inputClass =
  'w-full px-3 py-2 bg-[#0E1017] rounded-xl border border-[#1E2030] focus:border-[#2DD4BF] text-white font-mono text-xs outline-none transition-all';

export const AccountPanel: React.FC<AccountPanelProps> = ({ session, loading }) => {
  const [mode, setMode] = useState<'signIn' | 'signUp'>('signIn');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<{ tone: 'error' | 'info'; text: string } | null>(null);
  const [sync, setSync] = useState<SyncState>({ status: 'idle' });

  const userId = session?.user.id;

  const runSync = async () => {
    if (!supabase) return;
    setSync({ status: 'syncing' });
    try {
      setSync({ status: 'synced', stats: await syncLocalStatsToCloud(supabase) });
    } catch (err) {
      setSync({ status: 'error', message: err instanceof Error ? err.message : 'Sync failed' });
    }
  };

  useEffect(() => {
    if (userId) runSync();
    else setSync({ status: 'idle' });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!supabase || submitting) return;
    soundManager.playButtonClick();
    setSubmitting(true);
    setMessage(null);
    const credentials = { email: email.trim(), password };
    const { data, error } =
      mode === 'signIn'
        ? await supabase.auth.signInWithPassword(credentials)
        : await supabase.auth.signUp(credentials);
    setSubmitting(false);
    if (error) {
      setMessage({ tone: 'error', text: error.message });
      return;
    }
    setPassword('');
    if (mode === 'signUp' && !data.session) {
      setMessage({ tone: 'info', text: 'Check your email to confirm your account, then sign in.' });
    }
  };

  const handleSignOut = async () => {
    if (!supabase) return;
    soundManager.playButtonClick();
    await supabase.auth.signOut();
  };

  const header = (label: string, online: boolean) => (
    <div className="flex items-center justify-between mb-3">
      <span className="text-[10px] text-[#4A5068] uppercase font-bold">COMMAND UPLINK</span>
      <span
        className={`flex items-center gap-1 px-2 py-0.5 rounded-full border text-[10px] font-bold ${
          online
            ? 'border-[#2DD4BF]/40 text-[#2DD4BF] bg-[#2DD4BF]/10'
            : 'border-[#1E2030] text-[#C8D0E0] bg-[#0E1017]'
        }`}
      >
        {online ? <UserCheck size={12} /> : <CloudOff size={12} />}
        {label}
      </span>
    </div>
  );

  if (!supabase) {
    return (
      <div className="w-full p-4 bg-[#12131A] rounded-2xl border border-[#1E2030] font-mono">
        {header('GUEST MODE', false)}
        <p className="text-[11px] text-[#4A5068]">Online uplink not configured. Progress is saved on this device.</p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="w-full p-4 bg-[#12131A] rounded-2xl border border-[#1E2030] font-mono">
        {header('CHECKING', false)}
        <p className="text-[11px] text-[#4A5068]">Establishing uplink...</p>
      </div>
    );
  }

  if (session) {
    return (
      <div className="w-full p-4 bg-[#12131A] rounded-2xl border border-[#1E2030] font-mono">
        {header('LOGGED IN', true)}
        <p className="text-xs text-white truncate">{session.user.email}</p>

        <div className="mt-3 p-3 bg-[#0E1017] rounded-xl border border-[#1E2030] text-[11px]">
          {sync.status === 'syncing' && <span className="text-[#C8D0E0]">Syncing offline records...</span>}
          {sync.status === 'synced' && (
            <span className="text-[#C8D0E0]">
              CLOUD RECORD <span className="text-[#2DD4BF] font-bold">{sync.stats.wins}W</span> &bull;{' '}
              <span className="font-bold">{sync.stats.losses}L</span> &bull; ELO{' '}
              <span className="text-amber-400 font-bold">{sync.stats.elo}</span>
            </span>
          )}
          {sync.status === 'error' && (
            <div className="flex items-center justify-between gap-2">
              <span className="text-red-400">Sync failed: {sync.message}</span>
              <button
                type="button"
                onClick={() => {
                  soundManager.playButtonClick();
                  runSync();
                }}
                className="flex items-center gap-1 px-2 py-1 rounded-lg bg-[#12131A] hover:bg-[#1E2030] border border-[#1E2030] text-white cursor-pointer transition-all"
              >
                <RefreshCw size={12} className="text-[#2DD4BF]" />
                RETRY
              </button>
            </div>
          )}
          {sync.status === 'idle' && <span className="text-[#4A5068]">Awaiting sync...</span>}
        </div>

        <button
          type="button"
          onClick={handleSignOut}
          className="w-full mt-3 flex items-center justify-center gap-1.5 py-2 rounded-xl bg-[#12131A] hover:bg-[#1E2030] border border-[#1E2030] text-white text-xs font-bold transition-all cursor-pointer"
        >
          <LogOut size={14} className="text-[#2DD4BF]" />
          SIGN OUT
        </button>
      </div>
    );
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="w-full p-4 bg-[#12131A] rounded-2xl border border-[#1E2030] font-mono flex flex-col gap-2"
    >
      {header('GUEST MODE', false)}
      <p className="text-[11px] text-[#4A5068] -mt-1 mb-1">Sign in to back up your career and join online leaderboards.</p>

      <input
        type="email"
        required
        autoComplete="email"
        placeholder="EMAIL"
        value={email}
        onChange={e => setEmail(e.target.value)}
        className={inputClass}
      />
      <input
        type="password"
        required
        minLength={6}
        autoComplete={mode === 'signIn' ? 'current-password' : 'new-password'}
        placeholder="PASSWORD"
        value={password}
        onChange={e => setPassword(e.target.value)}
        className={inputClass}
      />

      {message && (
        <p className={`text-[11px] ${message.tone === 'error' ? 'text-red-400' : 'text-[#2DD4BF]'}`}>{message.text}</p>
      )}

      <button
        type="submit"
        disabled={submitting}
        className="w-full flex items-center justify-center gap-1.5 py-2 rounded-xl bg-[#2DD4BF] hover:bg-[#5EEAD4] disabled:opacity-50 text-black text-xs font-black transition-all cursor-pointer"
      >
        <LogIn size={14} />
        {submitting ? 'CONNECTING...' : mode === 'signIn' ? 'SIGN IN' : 'CREATE ACCOUNT'}
      </button>

      <button
        type="button"
        onClick={() => {
          soundManager.playButtonClick();
          setMode(mode === 'signIn' ? 'signUp' : 'signIn');
          setMessage(null);
        }}
        className="text-[10px] text-[#4A5068] hover:text-[#2DD4BF] transition-all cursor-pointer"
      >
        {mode === 'signIn' ? 'NEW COMMANDER? CREATE ACCOUNT' : 'HAVE AN ACCOUNT? SIGN IN'}
      </button>
    </form>
  );
};
