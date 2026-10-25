import React, { useState } from 'react';
import { ArrowLeft, UserPlus, Users, Swords, Copy, Check, Shield } from 'lucide-react';
import { AnimatedBackground } from '../components/AnimatedBackground';
import { UserAvatar } from '../components/UserAvatar';
import { getStoredProfile } from '../services/storage';
import { soundManager } from '../services/soundManager';

interface FriendsScreenProps {
  onBack: () => void;
  onStartPvPMatch: (opponentName: string) => void;
}

export const FriendsScreen: React.FC<FriendsScreenProps> = ({ onBack, onStartPvPMatch }) => {
  const profile = getStoredProfile();
  const [copiedCode, setCopiedCode] = useState(false);
  const [addCode, setAddCode] = useState('');
  const [friends, setFriends] = useState([
    { id: 'f1', name: 'Commander_Ares', rating: 1120, status: 'ONLINE', avatar: 'avatar-2' },
    { id: 'f2', name: 'Tactical_Valkyrie', rating: 1060, status: 'IN_MATCH', avatar: 'avatar-3' },
    { id: 'f3', name: 'ShadowPilot', rating: 980, status: 'OFFLINE', avatar: 'avatar-4' }
  ]);

  const inviteCode = `GAMBIT-${profile.uid.substring(0, 6).toUpperCase()}`;

  const handleCopy = () => {
    navigator.clipboard?.writeText(inviteCode);
    setCopiedCode(true);
    setTimeout(() => setCopiedCode(false), 2000);
    soundManager.playButtonClick();
  };

  const handleAddFriend = () => {
    if (!addCode.trim()) return;
    const newFriend = {
      id: 'f_' + Date.now(),
      name: addCode.trim(),
      rating: 1000,
      status: 'ONLINE',
      avatar: 'avatar-1'
    };
    setFriends([...friends, newFriend]);
    setAddCode('');
    soundManager.playButtonClick();
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
            COMMUNICATIONS & PVP
          </h2>

          <div className="w-12" />
        </header>

        {/* Content */}
        <div className="flex-1 overflow-y-auto py-4 flex flex-col gap-4 font-mono text-xs">
          {/* Quick Duel Card */}
          <div className="p-4 bg-gradient-to-br from-[#1E2030] to-[#12131A] rounded-2xl border border-purple-500/40 shadow-xl shadow-purple-950/20">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-bold text-white flex items-center gap-1.5">
                <Swords size={16} className="text-[#A78BFA]" />
                <span>LOCAL 2-PLAYER DUEL</span>
              </span>
              <span className="text-[10px] text-[#2DD4BF] bg-teal-500/10 px-2 py-0.5 rounded-full border border-teal-500/30">
                HOTSEAT
              </span>
            </div>
            <p className="text-[11px] font-sans text-[#C8D0E0] mb-3">
              Play face-to-face against another commander on this terminal. Both take turns deploying tokens on the same tactical grid.
            </p>
            <button
              type="button"
              onClick={() => {
                soundManager.playButtonClick();
                onStartPvPMatch('Player 2 (Opponent)');
              }}
              className="w-full py-2.5 px-4 rounded-xl font-mono text-xs font-bold bg-[#A78BFA] hover:bg-purple-300 text-black shadow-lg shadow-purple-500/20 transition-all cursor-pointer"
            >
              LAUNCH 2-PLAYER LOCAL DUEL
            </button>
          </div>

          {/* Commander Frequency Invite Code */}
          <div className="p-4 bg-[#12131A] rounded-2xl border border-[#1E2030]">
            <span className="text-[10px] text-[#4A5068] uppercase font-bold block mb-1">
              YOUR COMM-LINK FREQUENCY
            </span>
            <div className="flex items-center justify-between p-2.5 bg-[#0E1017] rounded-xl border border-[#1E2030]">
              <span className="font-bold text-[#2DD4BF] tracking-wider">{inviteCode}</span>
              <button
                type="button"
                onClick={handleCopy}
                className="flex items-center gap-1 text-[11px] text-[#C8D0E0] hover:text-white px-2 py-1 rounded bg-[#1E2030] cursor-pointer"
              >
                {copiedCode ? <Check size={12} className="text-[#2DD4BF]" /> : <Copy size={12} />}
                <span>{copiedCode ? 'COPIED' : 'COPY'}</span>
              </button>
            </div>
          </div>

          {/* Add Commander */}
          <div className="p-4 bg-[#12131A] rounded-2xl border border-[#1E2030]">
            <span className="text-[10px] text-[#4A5068] uppercase font-bold block mb-2">
              ADD COMMANDER TO FLEET
            </span>
            <div className="flex items-center gap-2">
              <input
                type="text"
                placeholder="Enter callsign or frequency..."
                value={addCode}
                onChange={e => setAddCode(e.target.value)}
                className="flex-1 px-3 py-2 bg-[#0E1017] border border-[#1E2030] rounded-xl text-white outline-none focus:border-[#2DD4BF]"
              />
              <button
                type="button"
                onClick={handleAddFriend}
                className="px-3 py-2 rounded-xl bg-[#2DD4BF] text-black font-bold flex items-center gap-1 cursor-pointer"
              >
                <UserPlus size={14} />
                <span>ADD</span>
              </button>
            </div>
          </div>

          {/* Fleet / Friends List */}
          <div className="flex flex-col gap-2">
            <span className="text-[10px] text-[#4A5068] uppercase font-bold px-1">
              ALLIED COMMANDERS ({friends.length})
            </span>

            {friends.map(f => (
              <div
                key={f.id}
                className="p-3 bg-[#12131A] rounded-xl border border-[#1E2030] flex items-center justify-between"
              >
                <div className="flex items-center gap-3">
                  <UserAvatar displayName={f.name} photoUrl={f.avatar} size="sm" />
                  <div className="flex flex-col text-left">
                    <span className="text-xs font-bold text-white">{f.name}</span>
                    <span className="text-[10px] text-[#4A5068]">RATING {f.rating}</span>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <span
                    className={`text-[9px] px-1.5 py-0.5 rounded font-mono ${
                      f.status === 'ONLINE'
                        ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                        : 'bg-[#1E2030] text-[#4A5068]'
                    }`}
                  >
                    {f.status}
                  </span>

                  <button
                    type="button"
                    onClick={() => {
                      soundManager.playButtonClick();
                      onStartPvPMatch(f.name);
                    }}
                    className="p-1.5 rounded-lg bg-[#1E2030] hover:bg-purple-950/60 text-[#A78BFA] hover:text-purple-300 border border-purple-500/30 transition-all cursor-pointer"
                    title="Challenge to Duel"
                  >
                    <Swords size={14} />
                  </button>
                </div>
              </div>
            ))}
          </div>
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
