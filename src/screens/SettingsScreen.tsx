import React, { useState } from 'react';
import { ArrowLeft, Volume2, VolumeX, Smartphone, Zap, Eye, RotateCcw, ShieldAlert, Palette, Check } from 'lucide-react';
import { AnimatedBackground } from '../components/AnimatedBackground';
import { getStoredSettings, saveStoredSettings, clearStoredHistory, DEFAULT_PROFILE, saveStoredProfile } from '../services/storage';
import { GameSettings } from '../types/game';
import { soundManager } from '../services/soundManager';
import { haptics } from '../services/haptics';

interface SettingsScreenProps {
  onBack: () => void;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({ onBack }) => {
  const [settings, setSettings] = useState<GameSettings>(getStoredSettings());

  const updateSetting = <K extends keyof GameSettings>(key: K, value: GameSettings[K]) => {
    const updated = { ...settings, [key]: value };
    setSettings(updated);
    saveStoredSettings(updated);

    if (key === 'soundEnabled') {
      soundManager.setEnabled(value as boolean);
    } else if (key === 'masterVolume') {
      soundManager.setVolume(value as number);
    } else if (key === 'hapticsEnabled') {
      haptics.setEnabled(value as boolean);
    }
  };

  const handleResetProgress = () => {
    if (window.confirm('Reset career statistics and unlocked medals? This cannot be undone.')) {
      saveStoredProfile(DEFAULT_PROFILE);
      clearStoredHistory();
      alert('Career progress reset to factory defaults.');
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
            SYSTEM SETTINGS
          </h2>

          <div className="w-12" />
        </header>

        {/* Content */}
        <div className="flex-1 overflow-y-auto py-4 flex flex-col gap-4 font-mono text-xs">
          {/* Audio Section */}
          <div className="p-4 bg-[#12131A] rounded-2xl border border-[#1E2030]">
            <div className="flex items-center gap-2 mb-3 pb-2 border-b border-[#1E2030]">
              <Volume2 size={16} className="text-[#2DD4BF]" />
              <span className="font-bold text-white uppercase tracking-wider">AUDIO & SOUND FX</span>
            </div>

            <div className="flex items-center justify-between mb-3">
              <span className="text-[#C8D0E0]">Audio Synthesizer FX</span>
              <button
                type="button"
                onClick={() => updateSetting('soundEnabled', !settings.soundEnabled)}
                className={`w-12 h-6 rounded-full transition-colors relative cursor-pointer ${
                  settings.soundEnabled ? 'bg-[#2DD4BF]' : 'bg-[#1E2030]'
                }`}
              >
                <div
                  className={`w-5 h-5 rounded-full bg-black absolute top-0.5 transition-transform ${
                    settings.soundEnabled ? 'right-0.5' : 'left-0.5'
                  }`}
                />
              </button>
            </div>

            {settings.soundEnabled && (
              <div className="flex flex-col gap-1">
                <div className="flex justify-between text-[11px] text-[#4A5068]">
                  <span>Master Volume</span>
                  <span>{Math.round(settings.masterVolume * 100)}%</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="1"
                  step="0.05"
                  value={settings.masterVolume}
                  onChange={e => updateSetting('masterVolume', parseFloat(e.target.value))}
                  className="w-full accent-[#2DD4BF] bg-[#1E2030] rounded-lg h-2 cursor-pointer"
                />
              </div>
            )}
          </div>

          {/* Gameplay & Interface Section */}
          <div className="p-4 bg-[#12131A] rounded-2xl border border-[#1E2030]">
            <div className="flex items-center gap-2 mb-3 pb-2 border-b border-[#1E2030]">
              <Eye size={16} className="text-[#A78BFA]" />
              <span className="font-bold text-white uppercase tracking-wider">INTERFACE & SIMULATION</span>
            </div>

            <div className="flex items-center justify-between mb-3">
              <div>
                <span className="text-[#C8D0E0] block">AI State Indicator</span>
                <span className="text-[10px] text-[#4A5068]">Display cognitive status in status bar</span>
              </div>
              <button
                type="button"
                onClick={() => updateSetting('aiCapabilityVisible', !settings.aiCapabilityVisible)}
                className={`w-12 h-6 rounded-full transition-colors relative cursor-pointer ${
                  settings.aiCapabilityVisible ? 'bg-[#2DD4BF]' : 'bg-[#1E2030]'
                }`}
              >
                <div
                  className={`w-5 h-5 rounded-full bg-black absolute top-0.5 transition-transform ${
                    settings.aiCapabilityVisible ? 'right-0.5' : 'left-0.5'
                  }`}
                />
              </button>
            </div>

            <div className="flex items-center justify-between mb-3">
              <div>
                <span className="text-[#C8D0E0] block">Probability Forecast</span>
                <span className="text-[10px] text-[#4A5068]">Interactive tooltips when inspecting cells</span>
              </div>
              <button
                type="button"
                onClick={() => updateSetting('showProbabilityTooltips', !settings.showProbabilityTooltips)}
                className={`w-12 h-6 rounded-full transition-colors relative cursor-pointer ${
                  settings.showProbabilityTooltips ? 'bg-[#2DD4BF]' : 'bg-[#1E2030]'
                }`}
              >
                <div
                  className={`w-5 h-5 rounded-full bg-black absolute top-0.5 transition-transform ${
                    settings.showProbabilityTooltips ? 'right-0.5' : 'left-0.5'
                  }`}
                />
              </button>
            </div>

            <div className="flex items-center justify-between">
              <div>
                <span className="text-[#C8D0E0] block">Haptic Feedback</span>
                <span className="text-[10px] text-[#4A5068]">Vibrate on placements and clash results</span>
              </div>
              <button
                type="button"
                onClick={() => updateSetting('hapticsEnabled', !settings.hapticsEnabled)}
                className={`w-12 h-6 rounded-full transition-colors relative cursor-pointer ${
                  settings.hapticsEnabled ? 'bg-[#2DD4BF]' : 'bg-[#1E2030]'
                }`}
              >
                <div
                  className={`w-5 h-5 rounded-full bg-black absolute top-0.5 transition-transform ${
                    settings.hapticsEnabled ? 'right-0.5' : 'left-0.5'
                  }`}
                />
              </button>
            </div>
          </div>

          {/* Reset Progress Section */}
          <div className="p-4 bg-[#12131A] rounded-2xl border border-red-500/20">
            <div className="flex items-center gap-2 mb-2 pb-2 border-b border-[#1E2030]">
              <ShieldAlert size={16} className="text-red-400" />
              <span className="font-bold text-red-400 uppercase tracking-wider">FACTORY RESET</span>
            </div>

            <p className="text-[11px] font-sans text-[#4A5068] mb-3">
              Erase all localized match history, reset win statistics, and relock all earned achievement medals.
            </p>

            <button
              type="button"
              onClick={handleResetProgress}
              className="w-full py-2.5 px-4 rounded-xl font-mono text-xs font-bold bg-red-950/40 hover:bg-red-900/50 text-red-400 border border-red-500/30 transition-all cursor-pointer"
            >
              RESET CAREER RECORDS
            </button>
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
          SAVE & RETURN TO BASE
        </button>
      </div>
    </AnimatedBackground>
  );
};
