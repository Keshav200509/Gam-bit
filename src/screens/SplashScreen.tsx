import React, { useEffect, useState } from 'react';
import { Shield } from 'lucide-react';
import { soundManager } from '../services/soundManager';

interface SplashScreenProps {
  onFinished: () => void;
}

export const SplashScreen: React.FC<SplashScreenProps> = ({ onFinished }) => {
  const [scaleUp, setScaleUp] = useState(false);

  useEffect(() => {
    soundManager.playLogoAppear();
    const t1 = setTimeout(() => setScaleUp(true), 150);
    const t2 = setTimeout(() => onFinished(), 1600);
    return () => {
      clearTimeout(t1);
      clearTimeout(t2);
    };
  }, [onFinished]);

  return (
    <div className="fixed inset-0 z-50 flex flex-col items-center justify-center bg-[#07080C] text-[#C8D0E0] select-none">
      <div
        className={`flex flex-col items-center gap-4 transition-all duration-700 ${
          scaleUp ? 'opacity-100 scale-100' : 'opacity-0 scale-90'
        }`}
      >
        <div className="relative flex items-center justify-center w-24 h-24 rounded-2xl bg-gradient-to-br from-[#1E2030] to-[#12131A] border-2 border-[#2DD4BF] shadow-2xl shadow-teal-500/30">
          <Shield size={48} className="text-[#2DD4BF] animate-pulse" />
          <div className="absolute -inset-1 rounded-2xl bg-[#2DD4BF]/20 blur-lg -z-10" />
        </div>

        <div className="flex flex-col items-center">
          <h1 className="text-4xl sm:text-5xl font-black font-mono tracking-widest text-white">
            GAMBIT
          </h1>
          <p className="text-xs font-mono tracking-[0.3em] text-[#2DD4BF] uppercase mt-1">
            TACTICAL GRID PROTOCOL
          </p>
        </div>

        <div className="flex items-center gap-1.5 mt-4">
          <div className="w-1.5 h-1.5 rounded-full bg-[#2DD4BF] animate-ping" />
          <span className="text-[10px] font-mono text-[#4A5068] uppercase tracking-wider">
            INITIALIZING CORE ENGINE...
          </span>
        </div>
      </div>
    </div>
  );
};
