import React from 'react';
import { Trophy, Skull, ArrowRight, RotateCcw, BarChart3, Home, Shield, Swords } from 'lucide-react';
import { Board } from '../types/game';
import { AnimatedBackground } from '../components/AnimatedBackground';
import { soundManager } from '../services/soundManager';

interface ResultScreenProps {
  playerScore: number;
  aiScore: number;
  board: Board;
  arena: string;
  level: string;
  clashesWon: number;
  clashesLost: number;
  scoutUses: number;
  lockUses: number;
  onPlayAgain: () => void;
  onReturnToMenu: () => void;
  onViewStats: () => void;
}

export const ResultScreen: React.FC<ResultScreenProps> = ({
  playerScore,
  aiScore,
  board,
  arena,
  level,
  clashesWon,
  clashesLost,
  scoutUses,
  lockUses,
  onPlayAgain,
  onReturnToMenu,
  onViewStats
}) => {
  const isWin = playerScore > aiScore;
  const isDraw = playerScore === aiScore;
  const pointDiff = Math.abs(playerScore - aiScore);

  // Calculate territory control
  let playerCells = 0;
  let aiCells = 0;
  let neutralCells = 0;

  for (let r = 0; r < 5; r++) {
    for (let c = 0; c < 5; c++) {
      const owner = board.cells[r][c].owner;
      if (owner === 'PLAYER') playerCells++;
      else if (owner === 'AI') aiCells++;
      else neutralCells++;
    }
  }

  const playerPct = Math.round((playerCells / 25) * 100);
  const aiPct = Math.round((aiCells / 25) * 100);

  return (
    <AnimatedBackground>
      <div className="flex-1 flex flex-col items-center justify-between p-4 max-w-md mx-auto w-full">
        {/* Result Header */}
        <div className="w-full text-center pt-4">
          <div className="inline-flex items-center justify-center w-20 h-20 rounded-2xl bg-[#12131A] border-2 border-[#2DD4BF]/50 mb-3 shadow-2xl shadow-teal-500/20">
            {isWin ? (
              <Trophy size={42} className="text-amber-400 animate-bounce" />
            ) : isDraw ? (
              <Shield size={42} className="text-teal-400" />
            ) : (
              <Skull size={42} className="text-red-500 animate-pulse" />
            )}
          </div>

          <h1
            className={`text-3xl font-black font-mono tracking-widest uppercase ${
              isWin ? 'text-[#2DD4BF]' : isDraw ? 'text-amber-400' : 'text-red-500'
            }`}
          >
            {isWin ? 'TACTICAL VICTORY' : isDraw ? 'TACTICAL STALEMATE' : 'MISSION FAILED'}
          </h1>
          <p className="text-xs font-mono text-[#4A5068] tracking-widest mt-1">
            {arena} // {level.replace('_', ' ')}
          </p>
        </div>

        {/* Score Card */}
        <div className="w-full p-4 bg-[#12131A] rounded-2xl border border-[#1E2030] shadow-xl my-4">
          <div className="grid grid-cols-2 gap-4 pb-4 border-b border-[#1E2030] text-center font-mono">
            <div>
              <span className="text-[10px] text-[#2DD4BF] font-bold uppercase block">
                PLAYER SCORE
              </span>
              <span className="text-4xl font-black text-white mt-1 block">
                {playerScore}
              </span>
            </div>
            <div>
              <span className="text-[10px] text-red-400 font-bold uppercase block">
                AI OPPONENT
              </span>
              <span className="text-4xl font-black text-white mt-1 block">
                {aiScore}
              </span>
            </div>
          </div>

          <div className="text-center pt-2">
            <span className="text-xs font-mono text-[#C8D0E0]">
              MARGIN OF OUTCOME:{' '}
              <strong className={isWin ? 'text-[#2DD4BF]' : 'text-red-400'}>
                {pointDiff} POINTS {isWin ? 'LEAD' : 'DEFICIT'}
              </strong>
            </span>
          </div>
        </div>

        {/* Territory & Clash Analytics */}
        <div className="w-full p-4 bg-[#12131A] rounded-2xl border border-[#1E2030] shadow-xl mb-4 font-mono">
          <h3 className="text-xs font-bold text-white uppercase tracking-wider mb-3">
            DEPLOYMENT ANALYTICS
          </h3>

          {/* Territory control bar */}
          <div className="mb-3">
            <div className="flex justify-between text-[10px] text-[#4A5068] mb-1">
              <span>TERRITORY CONTROL</span>
              <span>
                <strong className="text-[#2DD4BF]">{playerPct}%</strong> vs{' '}
                <strong className="text-red-400">{aiPct}%</strong>
              </span>
            </div>
            <div className="w-full h-2.5 bg-[#1E2030] rounded-full overflow-hidden flex">
              <div style={{ width: `${playerPct}%` }} className="h-full bg-[#2DD4BF]" />
              <div style={{ width: `${100 - playerPct - aiPct}%` }} className="h-full bg-[#12131A]" />
              <div style={{ width: `${aiPct}%` }} className="h-full bg-red-500" />
            </div>
          </div>

          {/* Metric grid */}
          <div className="grid grid-cols-3 gap-2 text-center text-xs">
            <div className="p-2 bg-[#0E1017] rounded-xl border border-[#1E2030]">
              <span className="text-[10px] text-[#4A5068] block">CLASHES WON</span>
              <span className="text-sm font-bold text-[#2DD4BF]">{clashesWon}</span>
            </div>
            <div className="p-2 bg-[#0E1017] rounded-xl border border-[#1E2030]">
              <span className="text-[10px] text-[#4A5068] block">CLASHES LOST</span>
              <span className="text-sm font-bold text-red-400">{clashesLost}</span>
            </div>
            <div className="p-2 bg-[#0E1017] rounded-xl border border-[#1E2030]">
              <span className="text-[10px] text-[#4A5068] block">SCOUT USES</span>
              <span className="text-sm font-bold text-white">{scoutUses}</span>
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="w-full flex flex-col gap-2 pb-2">
          <button
            type="button"
            onClick={() => {
              soundManager.playButtonClick();
              onPlayAgain();
            }}
            className="w-full flex items-center justify-center gap-2 py-3.5 px-4 rounded-xl font-mono text-xs font-black bg-[#2DD4BF] hover:bg-teal-300 text-black shadow-lg shadow-teal-500/30 transition-all cursor-pointer"
          >
            <RotateCcw size={16} />
            <span>PLAY AGAIN</span>
          </button>

          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              onClick={() => {
                soundManager.playButtonClick();
                onViewStats();
              }}
              className="flex items-center justify-center gap-1.5 py-2.5 px-3 rounded-xl font-mono text-xs font-bold bg-[#12131A] hover:bg-[#1E2030] text-[#C8D0E0] border border-[#1E2030] transition-all cursor-pointer"
            >
              <BarChart3 size={14} className="text-[#2DD4BF]" />
              <span>CAREER STATS</span>
            </button>

            <button
              type="button"
              onClick={() => {
                soundManager.playButtonClick();
                onReturnToMenu();
              }}
              className="flex items-center justify-center gap-1.5 py-2.5 px-3 rounded-xl font-mono text-xs font-bold bg-[#12131A] hover:bg-[#1E2030] text-[#C8D0E0] border border-[#1E2030] transition-all cursor-pointer"
            >
              <Home size={14} />
              <span>MAIN BASE</span>
            </button>
          </div>
        </div>
      </div>
    </AnimatedBackground>
  );
};
