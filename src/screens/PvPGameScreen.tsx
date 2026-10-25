import React, { useState, useEffect } from 'react';
import { ArrowLeft, Swords, CheckCircle2, UserCheck, Shield } from 'lucide-react';
import { Board, Position, Token, ClashResult } from '../types/game';
import { generateBoard } from '../domain/boardGenerator';
import { calculateIncome } from '../domain/incomeCalculator';
import { resolveClash } from '../domain/clashResolver';
import { posKey } from '../domain/positionUtils';
import { soundManager } from '../services/soundManager';
import { haptics } from '../services/haptics';
import { GameBoard } from '../components/GameBoard';
import { TokenBar } from '../components/TokenBar';
import { ActionBar } from '../components/ActionBar';
import { RoundProgressDots } from '../components/RoundProgressDots';
import { IncomeSummary } from '../components/IncomeSummary';
import { ResultScreen } from './ResultScreen';

interface PvPGameScreenProps {
  opponentName: string;
  onBack: () => void;
}

export const PvPGameScreen: React.FC<PvPGameScreenProps> = ({ opponentName, onBack }) => {
  const [roundNumber, setRoundNumber] = useState<number>(1);
  const [board, setBoard] = useState<Board>(() => generateBoard());
  const [player1Score, setPlayer1Score] = useState<number>(0);
  const [player2Score, setPlayer2Score] = useState<number>(0);

  // Turns: 'P1_PLACING' | 'PASS_TO_P2' | 'P2_PLACING' | 'RESOLVING' | 'INCOME' | 'GAME_OVER'
  const [turnState, setTurnState] = useState<string>('P1_PLACING');

  const [p1Placements, setP1Placements] = useState<Record<string, Token>>({});
  const [p1Locks, setP1Locks] = useState<Position[]>([]);
  const [p2Placements, setP2Placements] = useState<Record<string, Token>>({});
  const [p2Locks, setP2Locks] = useState<Position[]>([]);

  const [selectedToken, setSelectedToken] = useState<number | null>(1);
  const [lockMode, setLockMode] = useState<boolean>(false);

  const [currentResolvingPosition, setCurrentResolvingPosition] = useState<Position | null>(null);
  const [clashResults, setClashResults] = useState<Record<string, ClashResult>>({});

  const [p1IncomeDelta, setP1IncomeDelta] = useState<number>(0);
  const [p2IncomeDelta, setP2IncomeDelta] = useState<number>(0);
  const [showIncome, setShowIncome] = useState<boolean>(false);

  const isP1Turn = turnState === 'P1_PLACING';
  const isP2Turn = turnState === 'P2_PLACING';
  const currentPlacements = isP1Turn ? p1Placements : isP2Turn ? p2Placements : {};
  const currentLocks = isP1Turn ? p1Locks : isP2Turn ? p2Locks : [];

  const handleCellClick = (pos: Position) => {
    if (!isP1Turn && !isP2Turn) return;
    const key = posKey(pos);

    if (lockMode) {
      const existingToken = currentPlacements[key];
      if (!existingToken) return;

      const isLocked = currentLocks.some(l => l.row === pos.row && l.col === pos.col);
      if (isLocked) {
        const updatedLocks = currentLocks.filter(l => !(l.row === pos.row && l.col === pos.col));
        const updatedPlacements = { ...currentPlacements, [key]: { ...existingToken, isLocked: false } };
        if (isP1Turn) {
          setP1Locks(updatedLocks);
          setP1Placements(updatedPlacements);
        } else {
          setP2Locks(updatedLocks);
          setP2Placements(updatedPlacements);
        }
      } else {
        if (currentLocks.length >= 2) return;
        const updatedLocks = [...currentLocks, pos];
        const updatedPlacements = { ...currentPlacements, [key]: { ...existingToken, isLocked: true } };
        if (isP1Turn) {
          setP1Locks(updatedLocks);
          setP1Placements(updatedPlacements);
        } else {
          setP2Locks(updatedLocks);
          setP2Placements(updatedPlacements);
        }
      }
      soundManager.playLock();
      haptics.lockToggled();
      return;
    }

    // Place / Pickup token
    const existing = currentPlacements[key];
    if (existing) {
      const updated = { ...currentPlacements };
      delete updated[key];
      const updatedLocks = currentLocks.filter(l => !(l.row === pos.row && l.col === pos.col));
      setSelectedToken(existing.value);
      if (isP1Turn) {
        setP1Placements(updated);
        setP1Locks(updatedLocks);
      } else {
        setP2Placements(updated);
        setP2Locks(updatedLocks);
      }
      soundManager.playTokenPlace();
      haptics.tokenPlaced();
    } else if (selectedToken !== null) {
      const updated = { ...currentPlacements };
      for (const [k, tok] of Object.entries(updated)) {
        if (tok.value === selectedToken) delete updated[k];
      }
      updated[key] = { value: selectedToken, isLocked: false };

      const placedSet = new Set(Object.values(updated).map(t => t.value));
      const nextToken = [1, 2, 3, 4, 5].find(t => !placedSet.has(t)) || null;
      setSelectedToken(nextToken);

      if (isP1Turn) setP1Placements(updated);
      else setP2Placements(updated);

      soundManager.playTokenPlace();
      haptics.tokenPlaced();
    }
  };

  const handleCommitP1 = () => {
    soundManager.playButtonClick();
    setTurnState('PASS_TO_P2');
    setSelectedToken(1);
    setLockMode(false);
  };

  const handleStartP2 = () => {
    soundManager.playButtonClick();
    setTurnState('P2_PLACING');
  };

  const handleCommitP2 = async () => {
    soundManager.playButtonClick();
    setTurnState('RESOLVING');
    setLockMode(false);

    // Clash resolution
    const activePositions: Position[] = [];
    for (let r = 0; r < 5; r++) {
      for (let c = 0; c < 5; c++) {
        const k = `${r},${c}`;
        if (p1Placements[k] || p2Placements[k]) {
          activePositions.push({ row: r, col: c });
        }
      }
    }

    let currentBoard = board;
    const resultsMap: Record<string, ClashResult> = {};

    for (const pos of activePositions) {
      const k = posKey(pos);
      setCurrentResolvingPosition(pos);
      soundManager.playTokenPlace();
      await new Promise(r => setTimeout(r, 250));

      const res = resolveClash(pos, p1Placements, p2Placements, currentBoard, p1Locks, p2Locks);
      resultsMap[k] = res;
      setClashResults({ ...resultsMap });

      let newOwner = currentBoard.cells[pos.row][pos.col].owner;
      if (res.type === 'PlayerWins' || res.type === 'PlayerUncontested' || res.type === 'PlayerDefends') {
        newOwner = 'PLAYER';
        soundManager.playClashWin();
      } else if (res.type === 'AIWins' || res.type === 'AIUncontested' || res.type === 'AIDefends') {
        newOwner = 'AI'; // Player 2 acts as "AI" ownership on board
        soundManager.playClashLose();
      } else {
        newOwner = 'NONE';
        soundManager.playClashTie();
      }

      currentBoard = {
        cells: currentBoard.cells.map((rowArr, rIdx) =>
          rowArr.map((cellObj, cIdx) => {
            if (rIdx === pos.row && cIdx === pos.col) {
              return { ...cellObj, owner: newOwner };
            }
            return cellObj;
          })
        )
      };

      setBoard(currentBoard);
      await new Promise(r => setTimeout(r, 300));
    }

    setCurrentResolvingPosition(null);

    // Calculate revenue
    const { playerIncome, aiIncome } = calculateIncome(currentBoard, roundNumber);
    setPlayer1Score(prev => prev + playerIncome);
    setPlayer2Score(prev => prev + aiIncome);
    setP1IncomeDelta(playerIncome);
    setP2IncomeDelta(aiIncome);
    setShowIncome(true);
  };

  const handleContinueAfterIncome = () => {
    setShowIncome(false);
    setP1IncomeDelta(0);
    setP2IncomeDelta(0);
    setP1Placements({});
    setP2Placements({});
    setP1Locks([]);
    setP2Locks([]);
    setClashResults({});

    if (roundNumber >= 12) {
      setTurnState('GAME_OVER');
      if (player1Score > player2Score) soundManager.playGameOverWin();
      else soundManager.playGameOverLose();
    } else {
      setRoundNumber(prev => prev + 1);
      setTurnState('P1_PLACING');
      setSelectedToken(1);
    }
  };

  if (turnState === 'GAME_OVER') {
    return (
      <ResultScreen
        playerScore={player1Score}
        aiScore={player2Score}
        board={board}
        arena="PVP DUEL"
        level="HEAD-TO-HEAD"
        clashesWon={Object.values(clashResults).filter(c => c.type.startsWith('Player')).length}
        clashesLost={Object.values(clashResults).filter(c => c.type.startsWith('AI')).length}
        scoutUses={0}
        lockUses={p1Locks.length}
        onPlayAgain={() => {
          setBoard(generateBoard());
          setPlayer1Score(0);
          setPlayer2Score(0);
          setRoundNumber(1);
          setTurnState('P1_PLACING');
          setSelectedToken(1);
        }}
        onReturnToMenu={onBack}
        onViewStats={onBack}
      />
    );
  }

  return (
    <div className="flex flex-col min-h-screen bg-[#07080C] text-[#C8D0E0] select-none">
      {/* Income Summary Modal */}
      {showIncome && (
        <IncomeSummary
          playerIncome={p1IncomeDelta}
          aiIncome={p2IncomeDelta}
          playerScore={player1Score}
          aiScore={player2Score}
          roundNumber={roundNumber}
          incomeMultiplier={1}
          isGameOver={roundNumber >= 12}
          onContinue={handleContinueAfterIncome}
        />
      )}

      {/* Top Header */}
      <header className="flex items-center justify-between p-3 bg-[#0A0B10] border-b border-[#1E2030]">
        <button
          type="button"
          onClick={onBack}
          className="flex items-center gap-1 px-3 py-1.5 rounded-xl bg-[#12131A] hover:bg-[#1E2030] text-xs font-mono text-white"
        >
          <ArrowLeft size={16} className="text-[#2DD4BF]" />
          <span>EXIT</span>
        </button>

        <div className="flex items-center gap-2 font-mono text-xs">
          <span className="font-bold text-[#A78BFA]">PVP DUEL</span>
          <span className="text-[#4A5068]">|</span>
          <span className="text-white font-bold">ROUND {roundNumber}/12</span>
        </div>

        <div className="w-12" />
      </header>

      {/* Scores Banner */}
      <div className="grid grid-cols-2 p-3 bg-[#12131A] border-b border-[#1E2030] font-mono text-center">
        <div className="border-r border-[#1E2030]">
          <span className="text-[10px] text-[#2DD4BF] font-bold block">PLAYER 1 (YOU)</span>
          <span className="text-2xl font-black text-white">{player1Score}</span>
        </div>
        <div>
          <span className="text-[10px] text-purple-400 font-bold block">{opponentName}</span>
          <span className="text-2xl font-black text-white">{player2Score}</span>
        </div>
      </div>

      {/* Main Area */}
      <main className="flex-1 flex flex-col items-center justify-between p-4 max-w-lg mx-auto w-full gap-2">
        <RoundProgressDots currentRound={roundNumber} />

        {/* Turn Handoff Interstitial */}
        {turnState === 'PASS_TO_P2' ? (
          <div className="flex-1 flex flex-col items-center justify-center p-6 bg-[#12131A] rounded-2xl border border-purple-500/40 text-center my-6">
            <UserCheck size={48} className="text-[#A78BFA] mb-3 animate-pulse" />
            <h3 className="text-lg font-mono font-extrabold text-white uppercase mb-1">
              PLAYER 1 ORDERS LOCKED
            </h3>
            <p className="text-xs font-sans text-[#C8D0E0] mb-6">
              Hand the terminal to <strong>{opponentName}</strong> to deploy their 5 tokens secretly.
            </p>
            <button
              type="button"
              onClick={handleStartP2}
              className="py-3 px-6 rounded-xl font-mono text-xs font-black bg-[#A78BFA] text-black shadow-lg shadow-purple-500/30 cursor-pointer"
            >
              READY — START {opponentName}'S TURN
            </button>
          </div>
        ) : (
          <>
            <div className="flex items-center gap-2 px-3 py-1 bg-[#12131A] rounded-full border border-[#1E2030] text-xs font-mono">
              <span className={`w-2 h-2 rounded-full ${isP1Turn ? 'bg-[#2DD4BF]' : 'bg-[#A78BFA]'} animate-ping`} />
              <span className="font-bold text-white">
                {isP1Turn ? 'DEPLOYING: PLAYER 1' : isP2Turn ? `DEPLOYING: ${opponentName}` : 'CLASH RESOLUTION'}
              </span>
            </div>

            <GameBoard
              board={board}
              playerPlacements={isP1Turn ? p1Placements : turnState === 'RESOLVING' ? p1Placements : {}}
              aiPlacements={isP2Turn ? p2Placements : turnState === 'RESOLVING' ? p2Placements : {}}
              playerLocks={isP1Turn ? p1Locks : turnState === 'RESOLVING' ? p1Locks : []}
              aiLocks={isP2Turn ? p2Locks : turnState === 'RESOLVING' ? p2Locks : []}
              scoutUsed={false}
              scoutedPosition={null}
              scoutResult={null}
              phase={turnState === 'RESOLVING' ? 'Resolving' : 'Placing'}
              currentResolvingPosition={currentResolvingPosition}
              clashResults={clashResults}
              onCellClick={handleCellClick}
              onMouseEnter={() => {}}
              onMouseLeave={() => {}}
              scoutMode={false}
              lockMode={lockMode}
            />

            {(isP1Turn || isP2Turn) && (
              <>
                <TokenBar
                  playerPlacements={currentPlacements}
                  selectedToken={selectedToken}
                  onSelectToken={setSelectedToken}
                />

                <ActionBar
                  phase="Placing"
                  scoutEnabled={false}
                  scoutUsed={true}
                  scoutMode={false}
                  onToggleScout={() => {}}
                  maxLocks={2}
                  currentLocksCount={currentLocks.length}
                  lockMode={lockMode}
                  onToggleLock={() => setLockMode(!lockMode)}
                  tokensPlacedCount={Object.keys(currentPlacements).length}
                  onClear={() => {
                    if (isP1Turn) {
                      setP1Placements({});
                      setP1Locks([]);
                    } else {
                      setP2Placements({});
                      setP2Locks([]);
                    }
                    setSelectedToken(1);
                  }}
                  onCommit={isP1Turn ? handleCommitP1 : handleCommitP2}
                />
              </>
            )}
          </>
        )}
      </main>
    </div>
  );
};
