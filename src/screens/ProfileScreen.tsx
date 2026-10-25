import React, { useState } from 'react';
import { ArrowLeft, Check, Edit2, Shield, Trophy, Award, Zap } from 'lucide-react';
import { AnimatedBackground } from '../components/AnimatedBackground';
import { UserAvatar } from '../components/UserAvatar';
import { getStoredProfile, saveStoredProfile } from '../services/storage';
import { soundManager } from '../services/soundManager';

interface ProfileScreenProps {
  onBack: () => void;
}

export const ProfileScreen: React.FC<ProfileScreenProps> = ({ onBack }) => {
  const [profile, setProfile] = useState(getStoredProfile());
  const [editingName, setEditingName] = useState(false);
  const [nameInput, setNameInput] = useState(profile.displayName);

  const avatars = ['avatar-1', 'avatar-2', 'avatar-3', 'avatar-4'];

  const handleSaveName = () => {
    const trimmed = nameInput.trim() || 'Commander';
    const updated = { ...profile, displayName: trimmed };
    setProfile(updated);
    saveStoredProfile(updated);
    setEditingName(false);
    soundManager.playButtonClick();
  };

  const handleSelectAvatar = (av: string) => {
    const updated = { ...profile, photoUrl: av };
    setProfile(updated);
    saveStoredProfile(updated);
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
            COMMANDER PROFILE
          </h2>

          <div className="w-12" />
        </header>

        {/* Content */}
        <div className="flex-1 overflow-y-auto py-4 flex flex-col items-center gap-4">
          {/* Avatar & Callsign Hero */}
          <div className="flex flex-col items-center">
            <UserAvatar displayName={profile.displayName} photoUrl={profile.photoUrl} size="lg" />

            <div className="flex items-center gap-2 mt-3">
              {editingName ? (
                <div className="flex items-center gap-1.5">
                  <input
                    type="text"
                    maxLength={18}
                    value={nameInput}
                    onChange={e => setNameInput(e.target.value)}
                    className="px-2 py-1 bg-[#1E2030] rounded-lg border border-[#2DD4BF] text-white font-mono text-sm outline-none"
                    autoFocus
                  />
                  <button
                    type="button"
                    onClick={handleSaveName}
                    className="p-1 rounded bg-[#2DD4BF] text-black cursor-pointer"
                  >
                    <Check size={16} />
                  </button>
                </div>
              ) : (
                <div className="flex items-center gap-2">
                  <h3 className="text-lg font-black font-mono text-white">
                    {profile.displayName}
                  </h3>
                  <button
                    type="button"
                    onClick={() => setEditingName(true)}
                    className="p-1 text-[#4A5068] hover:text-[#2DD4BF] cursor-pointer"
                  >
                    <Edit2 size={14} />
                  </button>
                </div>
              )}
            </div>

            <span className="text-xs font-mono text-[#2DD4BF] mt-0.5">
              RATING {profile.stats.pvpRating} &bull; TACTICAL COMMANDER
            </span>
          </div>

          {/* Avatar switcher */}
          <div className="w-full p-4 bg-[#12131A] rounded-2xl border border-[#1E2030] flex flex-col items-center">
            <span className="text-[10px] font-mono text-[#4A5068] uppercase font-bold mb-3">
              CHANGE COMMAND INSIGNIA
            </span>
            <div className="flex items-center gap-4">
              {avatars.map(av => (
                <button
                  key={av}
                  type="button"
                  onClick={() => handleSelectAvatar(av)}
                  className={`p-1 rounded-xl transition-all cursor-pointer ${
                    profile.photoUrl === av
                      ? 'ring-2 ring-[#2DD4BF] scale-110 shadow-lg shadow-teal-500/20'
                      : 'opacity-50 hover:opacity-100'
                  }`}
                >
                  <UserAvatar displayName="P" photoUrl={av} size="md" />
                </button>
              ))}
            </div>
          </div>

          {/* Career Stats Grid */}
          <div className="w-full p-4 bg-[#12131A] rounded-2xl border border-[#1E2030] font-mono">
            <span className="text-[10px] text-[#4A5068] uppercase font-bold block mb-3">
              CAREER DEPLOYMENT RECORDS
            </span>

            <div className="grid grid-cols-2 gap-3 text-xs">
              <div className="p-3 bg-[#0E1017] rounded-xl border border-[#1E2030]">
                <span className="text-[#4A5068] text-[10px] block">TOTAL MISSIONS</span>
                <span className="text-lg font-bold text-white mt-1 block">
                  {profile.stats.totalGames}
                </span>
              </div>

              <div className="p-3 bg-[#0E1017] rounded-xl border border-[#1E2030]">
                <span className="text-[#4A5068] text-[10px] block">VICTORIES</span>
                <span className="text-lg font-bold text-[#2DD4BF] mt-1 block">
                  {profile.stats.totalWins}
                </span>
              </div>

              <div className="p-3 bg-[#0E1017] rounded-xl border border-[#1E2030]">
                <span className="text-[#4A5068] text-[10px] block">CLASH VICTORIES</span>
                <span className="text-lg font-bold text-amber-400 mt-1 block">
                  {profile.stats.clashesWon}
                </span>
              </div>

              <div className="p-3 bg-[#0E1017] rounded-xl border border-[#1E2030]">
                <span className="text-[#4A5068] text-[10px] block">MEDALS EARNED</span>
                <span className="text-lg font-bold text-[#A78BFA] mt-1 block">
                  {profile.stats.unlockedAchievementIds.length} / 12
                </span>
              </div>
            </div>
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
