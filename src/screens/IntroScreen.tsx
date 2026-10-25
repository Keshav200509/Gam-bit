import React, { useState } from 'react';
import { ArrowRight, CheckCircle2, Shield, Radio, Lock, Swords, Sparkles } from 'lucide-react';
import { AnimatedBackground } from '../components/AnimatedBackground';
import { UserAvatar } from '../components/UserAvatar';
import { soundManager } from '../services/soundManager';

interface IntroScreenProps {
  onComplete: (displayName: string, avatar: string) => void;
}

export const IntroScreen: React.FC<IntroScreenProps> = ({ onComplete }) => {
  const [step, setStep] = useState<number>(0);
  const [displayName, setDisplayName] = useState<string>('Commander');
  const [selectedAvatar, setSelectedAvatar] = useState<string>('avatar-1');

  const avatars = ['avatar-1', 'avatar-2', 'avatar-3', 'avatar-4'];

  const tutorialSteps = [
    {
      title: 'TACTICAL TERRITORY CONTROL',
      icon: <Shield size={32} className="text-[#2DD4BF]" />,
      description:
        'Battle the AI on a 5x5 grid over 12 high-stakes rounds. Control cells with high values (1, 2, or 3 points) to maximize your round-by-round income revenue.'
    },
    {
      title: 'SIGNALS INTELLIGENCE & SCOUTING',
      icon: <Radio size={32} className="text-[#A78BFA]" />,
      description:
        'Intercept the AI opponent’s tactical plans each round. Decode subtle intel hints and dispatch reconnaissance scouts to verify whether enemy tokens are targeting specific sectors.'
    },
    {
      title: 'STRATEGIC CLASHES & LOCKS',
      icon: <Swords size={32} className="text-amber-400" />,
      description:
        'Deploy tokens valued 1 through 5. When both commanders clash on a cell, friendly neighbor adjacencies, defensive fortifications, and tactical Locks (+2 strength) decide the victor.'
    }
  ];

  const handleNext = () => {
    soundManager.playButtonClick();
    if (step < 3) {
      setStep(step + 1);
    } else {
      onComplete(displayName.trim() || 'Commander', selectedAvatar);
    }
  };

  return (
    <AnimatedBackground>
      <div className="flex-1 flex flex-col items-center justify-center p-4 max-w-md mx-auto w-full">
        {step < 3 ? (
          /* Tutorial slides */
          <div className="w-full p-6 bg-[#12131A] rounded-2xl border border-[#1E2030] shadow-2xl flex flex-col items-center text-center animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-center w-16 h-16 rounded-2xl bg-[#1E2030] border border-[#2DD4BF]/30 mb-4 shadow-lg shadow-teal-500/10">
              {tutorialSteps[step].icon}
            </div>

            <span className="text-[10px] font-mono text-[#2DD4BF] font-bold tracking-widest uppercase mb-1">
              BRIEFING 0{step + 1} OF 03
            </span>

            <h2 className="text-lg font-mono font-extrabold text-white tracking-wide mb-3">
              {tutorialSteps[step].title}
            </h2>

            <p className="text-xs font-sans text-[#C8D0E0] leading-relaxed mb-6">
              {tutorialSteps[step].description}
            </p>

            {/* Step Indicators */}
            <div className="flex items-center gap-2 mb-6">
              {[0, 1, 2].map(i => (
                <div
                  key={i}
                  className={`h-1.5 rounded-full transition-all ${
                    i === step ? 'w-6 bg-[#2DD4BF]' : 'w-2 bg-[#1E2030]'
                  }`}
                />
              ))}
            </div>

            <button
              type="button"
              onClick={handleNext}
              className="w-full flex items-center justify-center gap-2 py-3 px-4 rounded-xl font-mono text-xs font-extrabold bg-[#2DD4BF] hover:bg-teal-300 text-black shadow-lg shadow-teal-500/30 transition-all cursor-pointer"
            >
              <span>{step === 2 ? 'SET UP CALLSIGN' : 'NEXT INTEL BRIEFING'}</span>
              <ArrowRight size={14} />
            </button>
          </div>
        ) : (
          /* Profile customization slide */
          <div className="w-full p-6 bg-[#12131A] rounded-2xl border border-[#1E2030] shadow-2xl flex flex-col items-center text-center animate-in fade-in zoom-in-95 duration-200">
            <div className="mb-4">
              <UserAvatar displayName={displayName} photoUrl={selectedAvatar} size="lg" />
            </div>

            <h2 className="text-lg font-mono font-extrabold text-white tracking-wide mb-1">
              IDENTIFY YOURSELF
            </h2>
            <p className="text-xs font-mono text-[#4A5068] mb-5">
              SET YOUR COMMAND CALLSIGN & INSIGNIA
            </p>

            {/* Display Name Input */}
            <div className="w-full text-left mb-4">
              <label className="text-[10px] font-mono text-[#2DD4BF] font-bold uppercase block mb-1">
                COMMANDER CALLSIGN
              </label>
              <input
                type="text"
                maxLength={18}
                value={displayName}
                onChange={e => setDisplayName(e.target.value)}
                className="w-full px-3 py-2.5 rounded-xl bg-[#0E1017] border border-[#1E2030] focus:border-[#2DD4BF] text-white font-mono text-sm outline-none transition-colors"
                placeholder="Enter callsign..."
              />
            </div>

            {/* Avatar picker */}
            <div className="w-full text-left mb-6">
              <label className="text-[10px] font-mono text-[#4A5068] font-bold uppercase block mb-2">
                INSIGNIA SELECTION
              </label>
              <div className="flex items-center justify-center gap-3">
                {avatars.map(av => (
                  <button
                    key={av}
                    type="button"
                    onClick={() => {
                      setSelectedAvatar(av);
                      soundManager.playButtonClick();
                    }}
                    className={`p-1 rounded-xl transition-all ${
                      selectedAvatar === av
                        ? 'ring-2 ring-[#2DD4BF] scale-105'
                        : 'opacity-60 hover:opacity-100'
                    }`}
                  >
                    <UserAvatar displayName="P" photoUrl={av} size="md" />
                  </button>
                ))}
              </div>
            </div>

            <button
              type="button"
              onClick={handleNext}
              className="w-full flex items-center justify-center gap-2 py-3 px-4 rounded-xl font-mono text-xs font-extrabold bg-[#2DD4BF] hover:bg-teal-300 text-black shadow-lg shadow-teal-500/30 transition-all cursor-pointer"
            >
              <span>ENTER COMMAND CENTER</span>
              <CheckCircle2 size={16} />
            </button>
          </div>
        )}
      </div>
    </AnimatedBackground>
  );
};
