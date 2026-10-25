import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  ArenaId,
  GameLevelId,
  Position,
  ProbabilityBreakdown,
  RoundState,
  Score,
  Token,
  ClashResult
} from '../types/game';
import { GameEngine } from '../domain/gameEngine';
import { getArenaConfig } from '../domain/arenaConfig';
import { calculateProbabilities } from '../domain/probabilityCalculator';
import { getCapabilityFromConfig } from '../domain/aiEngine';
import { posKey } from '../domain/positionUtils';
import { resolveClash } from '../domain/clashResolver';
import { soundManager } from '../services/soundManager';
import { haptics } from '../services/haptics';
import {
  saveActiveGame,
  clearActiveGame,
  getStoredSave,
  addStoredHistory,
  getStoredProfile,
  saveStoredProfile,
  getStoredSettings
} from '../services/storage';
import { checkAchievements } from '../domain/achievements';
import { StatusBar } from '../components/StatusBar';
import { RoundProgressDots } from '../components/RoundProgressDots';
import { LateGameEventBanner } from '../components/LateGameEventBanner';
import { IntelPanel } from '../components/IntelPanel';
import { GameBoard } from '../components/GameBoard';
import { TokenBar } from '../components/TokenBar';
import { ActionBar } from '../components/ActionBar';
import { ProbabilityTooltip } from '../components/ProbabilityTooltip';
import { IncomeSummary } from '../components/IncomeSummary';
import { ResumeDialog } from '../components/ResumeDialog';
import { ResultScreen } from './ResultScreen';

interface GameScreenProps {
  arena: ArenaId;
  level: GameLevelId;
  isDailyChallenge?: boolean;
  dailyChallengeSeed?: number;
  onExit: () => void;
  onOpenSettings: () => void;
  onNavigateToStats: () => void;
}

export const GameScreen: React.FC<GameScreenProps> = ({
  arena,
  level,
  isDailyChallenge = false,
  dailyChallengeSeed = 0,
  onExit,
  onOpenSettings,
  onNavigateToStats
}) => {
  const settings = getStoredSettings();
  const config = getArenaConfig(arena, level);

  const engineRef = useRef<GameEngine | null>(null);
  if (!engineRef.current) {
    engineRef.current = new GameEngine(config);
    engineRef.current.startNewGame(config);
  }
  const engine = engineRef.current;

  const [roundState, setRoundState] = useState<RoundState>(engine.roundState);
  const [playerScore, setPlayerScore] = useState<number>(engine.score.playerScore);
  const [aiScore, setAiScore] = useState<number>(engine.score.aiScore);
  const [selectedToken, setSelectedToken] = useState<number | null>(1);

  const [scoutMode, setScoutMode] = useState<boolean>(false);
  const [lockMode, setLockMode] = useState<boolean>(false);

  const [currentResolvingPosition, setCurrentResolvingPosition] = useState<Position | null>(null);
  const [clashResults, setClashResults] = useState<Record<string, ClashResult>>({});

  const [hoveredPosition, setHoveredPosition] = useState<Position | null>(null);
  const [probabilityBreakdown, setProbabilityBreakdown] = useState<ProbabilityBreakdown | null>(null);

  const [playerIncomeDelta, setPlayerIncomeDelta] = useState<number>(0);
  const [aiIncomeDelta, setAiIncomeDelta] = useState<number>(0);
  const [showIncomeSummary, setShowIncomeSummary] = useState<boolean>(false);

  const [showResumeDialog, setShowResumeDialog] = useState<boolean>(false);
  const [timeRemaining, setTimeRemaining] = useState<number>(60);
  const [timerActive, setTimerActive] = useState<boolean>(false);

  const [gameStartTime] = useState<number>(Date.now());
  const [wasBehindAtRound8, setWasBehindAtRound8] = useState<boolean>(false);
  const [usedBothLocksEveryRound, setUsedBothLocksEveryRound] = useState<boolean>(true);

  // Stats trackers
  const [clashesWonCount, setClashesWonCount] = useState<number>(0);
  const [clashesLostCount, setClashesLostCount] = useState<number>(0);
  const [scoutUsesCount, setScoutUsesCount] = useState<number>(0);
  const [lockUsesCount, setLockUsesCount] = useState<number>(0);

  // Check saved game on mount
  useEffect(() => {
    const saved = getStoredSave();
    if (saved && !isDailyChallenge) {
      setShowResumeDialog(true);
    }
  }, [isDailyChallenge]);

  // Sync sounds & haptics with settings
  useEffect(() => {
    soundManager.setEnabled(settings.soundEnabled);
    soundManager.setVolume(settings.masterVolume);
    haptics.setEnabled(settings.hapticsEnabled);
  }, [settings]);

  // Round Timer
  useEffect(() => {
    if (roundState.phase !== 'Placing') {
      setTimerActive(false);
      return;
    }

    setTimeRemaining(60);
    setTimerActive(true);

    const interval = setInterval(() => {
      setTimeRemaining(prev => {
        if (prev <= 1) {
          clearInterval(interval);
          soundManager.playTimerExpired();
          handleCommit(true);
          return 0;
        }
        if (prev === 11) {
          soundManager.playTimerTickSoft();
        } else if (prev <= 6 && prev > 1) {
          soundManager.playTimerTickUrgent();
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [roundState.phase, roundState.roundNumber]);

  // Auto-save active simulation
  useEffect(() => {
    if (roundState.phase !== 'GameOver' && !showResumeDialog) {
      saveActiveGame({
        score: { playerScore, aiScore },
        roundState,
        arena,
        level,
        isDailyChallenge,
        dailyChallengeSeed,
        timestamp: Date.now()
      });
    } else if (roundState.phase === 'GameOver') {
      clearActiveGame();
    }
  }, [roundState, playerScore, aiScore, arena, level, isDailyChallenge, dailyChallengeSeed, showResumeDialog]);

  // Handle Resume Game
  const handleResumeGame = () => {
    const saved = getStoredSave();
    if (saved) {
      engine.restoreGameState(saved.score, saved.roundState, config);
      setRoundState(saved.roundState);
      setPlayerScore(saved.score.playerScore);
      setAiScore(saved.score.aiScore);
    }
    setShowResumeDialog(false);
  };

  const handleDiscardSave = () => {
    clearActiveGame();
    engine.startNewGame(config);
    setRoundState(engine.roundState);
    setPlayerScore(0);
    setAiScore(0);
    setShowResumeDialog(false);
  };

  // Cell clicks & token actions
  const handleCellClick = (pos: Position) => {
    if (roundState.phase !== 'Placing') return;
    const key = posKey(pos);

    // 1. Scout Mode
    if (scoutMode) {
      const updated = engine.applyScout(pos);
      setRoundState(updated);
      setScoutMode(false);
      setScoutUsesCount(prev => prev + 1);
      soundManager.playScout();
      haptics.scoutUsed();
      return;
    }

    // 2. Lock Mode
    if (lockMode) {
      const existingToken = roundState.playerPlacements[key];
      if (!existingToken) return;

      const isLocked = roundState.playerLocks.some(l => l.row === pos.row && l.col === pos.col);
      let updatedLocks: Position[];
      let updatedPlacements = { ...roundState.playerPlacements };

      if (isLocked) {
        updatedLocks = roundState.playerLocks.filter(l => !(l.row === pos.row && l.col === pos.col));
        updatedPlacements[key] = { ...existingToken, isLocked: false };
      } else {
        if (roundState.playerLocks.length >= config.maxLocksPerRound) return;
        updatedLocks = [...roundState.playerLocks, pos];
        updatedPlacements[key] = { ...existingToken, isLocked: true };
        setLockUsesCount(prev => prev + 1);
      }

      setRoundState({
        ...roundState,
        playerPlacements: updatedPlacements,
        playerLocks: updatedLocks
      });
      soundManager.playLock();
      haptics.lockToggled();
      return;
    }

    // 3. Regular Placement / Pickup
    const existing = roundState.playerPlacements[key];
    if (existing) {
      // Pick up token
      const updatedPlacements = { ...roundState.playerPlacements };
      delete updatedPlacements[key];
      const updatedLocks = roundState.playerLocks.filter(l => !(l.row === pos.row && l.col === pos.col));

      setSelectedToken(existing.value);
      setRoundState({
        ...roundState,
        playerPlacements: updatedPlacements,
        playerLocks: updatedLocks
      });
      soundManager.playTokenPlace();
      haptics.tokenPlaced();
    } else if (selectedToken !== null) {
      // Place selected token
      const updatedPlacements = { ...roundState.playerPlacements };

      // Remove from any previous cell if already placed
      for (const [k, tok] of Object.entries(updatedPlacements)) {
        if (tok.value === selectedToken) {
          delete updatedPlacements[k];
        }
      }

      updatedPlacements[key] = { value: selectedToken, isLocked: false };

      // Select next available token
      const placedSet = new Set(Object.values(updatedPlacements).map(t => t.value));
      const nextToken = [1, 2, 3, 4, 5].find(t => !placedSet.has(t)) || null;

      setSelectedToken(nextToken);
      setRoundState({
        ...roundState,
        playerPlacements: updatedPlacements
      });
      soundManager.playTokenPlace();
      haptics.tokenPlaced();
    } else {
      // Pick lowest unplaced token automatically
      const placedSet = new Set(Object.values(roundState.playerPlacements).map(t => t.value));
      const lowest = [1, 2, 3, 4, 5].find(t => !placedSet.has(t));
      if (lowest) {
        const updatedPlacements = { ...roundState.playerPlacements, [key]: { value: lowest, isLocked: false } };
        const newPlacedSet = new Set(Object.values(updatedPlacements).map(t => t.value));
        const nextToken = [1, 2, 3, 4, 5].find(t => !newPlacedSet.has(t)) || null;

        setSelectedToken(nextToken);
        setRoundState({
          ...roundState,
          playerPlacements: updatedPlacements
        });
        soundManager.playTokenPlace();
        haptics.tokenPlaced();
      }
    }
  };

  // Probability calculations on hover
  const handleCellHover = (pos: Position) => {
    if (!config.probabilityPreviewEnabled) return;
    if (roundState.phase !== 'Placing') return;

    setHoveredPosition(pos);
    const key = posKey(pos);
    const tokenVal = roundState.playerPlacements[key]?.value || selectedToken || 1;
    const capability = getCapabilityFromConfig(config);

    const breakdown = calculateProbabilities(
      pos,
      tokenVal,
      roundState.board,
      roundState.intel,
      roundState.scoutedPosition &&
      roundState.scoutedPosition.row === pos.row &&
      roundState.scoutedPosition.col === pos.col
        ? roundState.scoutResult
        : null,
      capability
    );
    setProbabilityBreakdown(breakdown);
  };

  const handleCellLeave = () => {
    setHoveredPosition(null);
    setProbabilityBreakdown(null);
  };

  // Commit Placements & Trigger Step-by-Step Resolution
  const handleCommit = useCallback(
    async (isAutoCommit: boolean = false) => {
      const keys = Object.keys(roundState.playerPlacements);
      if (!isAutoCommit && keys.length === 0) return;

      // Track lock usage for achievement
      if (roundState.playerLocks.length < 2) {
        setUsedBothLocksEveryRound(false);
      }

      // Step 1: Commit player placements
      let committedState = engine.commitPlayerPlacements(
        roundState.playerPlacements,
        roundState.playerLocks
      );
      setRoundState(committedState);
      setScoutMode(false);
      setLockMode(false);

      // Arena 1: AI reacts to player moves
      if (config.arena === 'ASCENDENCY') {
        await new Promise(r => setTimeout(r, 400));
        committedState = engine.executeArena1AiTurn(committedState.playerPlacements);
        setRoundState({ ...committedState });
      }

      // Reveal AI placements for 600ms
      await new Promise(r => setTimeout(r, 600));

      // Step 2: Transition to Resolving
      const resolvingState = engine.transitionToResolving();
      setRoundState(resolvingState);

      // Step 3: Granular Clash Resolution for each active cell
      const activePositions: Position[] = [];
      for (let r = 0; r < 5; r++) {
        for (let c = 0; c < 5; c++) {
          const k = `${r},${c}`;
          if (committedState.playerPlacements[k] || committedState.aiPlacements[k]) {
            activePositions.push({ row: r, col: c });
          }
        }
      }

      let currentBoard = committedState.board;
      const resultsMap: Record<string, ClashResult> = {};

      for (const pos of activePositions) {
        const k = posKey(pos);
        setCurrentResolvingPosition(pos);
        soundManager.playTokenPlace();
        await new Promise(r => setTimeout(r, 300));

        // Calculate cell-specific clash result
        const cellResult = resolveClash(
          pos,
          committedState.playerPlacements,
          committedState.aiPlacements,
          currentBoard,
          committedState.playerLocks,
          committedState.aiLocks
        );
        resultsMap[k] = cellResult;
        setClashResults({ ...resultsMap });

        // Sounds & counters
        if (
          cellResult.type === 'PlayerWins' ||
          cellResult.type === 'PlayerUncontested' ||
          cellResult.type === 'PlayerDefends'
        ) {
          soundManager.playClashWin();
          haptics.clashWon();
          setClashesWonCount(prev => prev + 1);
        } else if (
          cellResult.type === 'AIWins' ||
          cellResult.type === 'AIUncontested' ||
          cellResult.type === 'AIDefends'
        ) {
          soundManager.playClashLose();
          haptics.clashLost();
          setClashesLostCount(prev => prev + 1);
        } else {
          soundManager.playClashTie();
          haptics.clashTied();
        }

        // Update cell ownership on board
        let newOwner = currentBoard.cells[pos.row][pos.col].owner;
        if (
          cellResult.type === 'PlayerWins' ||
          cellResult.type === 'PlayerUncontested' ||
          cellResult.type === 'PlayerDefends'
        ) {
          newOwner = 'PLAYER';
        } else if (
          cellResult.type === 'AIWins' ||
          cellResult.type === 'AIUncontested' ||
          cellResult.type === 'AIDefends'
        ) {
          newOwner = 'AI';
        } else if (cellResult.type === 'Tie') {
          newOwner = 'NONE';
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

        setRoundState(prev => ({ ...prev, board: currentBoard }));
        await new Promise(r => setTimeout(r, 350));
      }

      setCurrentResolvingPosition(null);

      // Step 4: Resolve full round in engine and collect income
      engine.resolveRound();
      const { playerIncome, aiIncome } = engine.collectIncome();

      setPlayerScore(engine.score.playerScore);
      setAiScore(engine.score.aiScore);
      setPlayerIncomeDelta(playerIncome);
      setAiIncomeDelta(aiIncome);
      setShowIncomeSummary(true);

      // Check for Comeback Kid condition at round 8
      if (committedState.roundNumber === 8 && engine.score.aiScore - engine.score.playerScore >= 20) {
        setWasBehindAtRound8(true);
      }
    },
    [roundState, engine, config]
  );

  // Continue to next round from Income Summary
  const handleContinueAfterIncome = () => {
    setShowIncomeSummary(false);
    setPlayerIncomeDelta(0);
    setAiIncomeDelta(0);
    setClashResults({});

    const endState = engine.transitionToTransitionOrGameOver();

    if (endState.phase === 'GameOver') {
      const isWin = engine.score.playerScore > engine.score.aiScore;
      if (isWin) {
        soundManager.playGameOverWin();
      } else {
        soundManager.playGameOverLose();
      }
      haptics.gameOver();

      // Record Game History & Update Profile Stats
      const durationMs = Date.now() - gameStartTime;
      const historyEntry = {
        id: 'gm_' + Date.now(),
        date: new Date().toLocaleDateString(),
        arena,
        level,
        playerScore: engine.score.playerScore,
        aiScore: engine.score.aiScore,
        result: isWin ? ('WIN' as const) : engine.score.playerScore === engine.score.aiScore ? ('DRAW' as const) : ('LOSS' as const),
        roundsPlayed: endState.roundNumber,
        clashesWon: clashesWonCount,
        clashesLost: clashesLostCount,
        intelAccuracy: 0.75,
        scoutUses: scoutUsesCount,
        lockUses: lockUsesCount,
        isDailyChallenge
      };
      addStoredHistory(historyEntry);

      const profile = getStoredProfile();
      profile.stats.totalGames += 1;
      if (isWin) {
        profile.stats.totalWins += 1;
        profile.stats.pvpRating += 25;
      } else {
        profile.stats.pvpRating = Math.max(800, profile.stats.pvpRating - 15);
      }
      profile.stats.clashesWon += clashesWonCount;
      profile.stats.clashesLost += clashesLostCount;
      profile.stats.scoutEffectiveCount += scoutUsesCount;

      const newAchievements = checkAchievements(profile.stats, {
        playerScore: engine.score.playerScore,
        aiScore: engine.score.aiScore,
        durationMs,
        wasBehindAtRound8,
        usedBothLocksEveryRound,
        arena,
        level
      });
      profile.stats.unlockedAchievementIds = [
        ...new Set([...profile.stats.unlockedAchievementIds, ...newAchievements])
      ];

      saveStoredProfile(profile);
      setRoundState({ ...endState });
    } else {
      // Start next round
      const nextState = engine.startNewRound();
      setRoundState(nextState);
      setSelectedToken(1);
    }
  };

  // Play again handler
  const handlePlayAgain = () => {
    engine.startNewGame(config);
    setRoundState(engine.roundState);
    setPlayerScore(0);
    setAiScore(0);
    setSelectedToken(1);
    setClashesWonCount(0);
    setClashesLostCount(0);
    setScoutUsesCount(0);
    setLockUsesCount(0);
    setClashResults({});
  };

  // If game is over, render ResultScreen
  if (roundState.phase === 'GameOver') {
    return (
      <ResultScreen
        playerScore={playerScore}
        aiScore={aiScore}
        board={roundState.board}
        arena={arena}
        level={level}
        clashesWon={clashesWonCount}
        clashesLost={clashesLostCount}
        scoutUses={scoutUsesCount}
        lockUses={lockUsesCount}
        onPlayAgain={handlePlayAgain}
        onReturnToMenu={onExit}
        onViewStats={onNavigateToStats}
      />
    );
  }

  return (
    <div className="flex flex-col min-h-screen bg-[#07080C] text-[#C8D0E0] select-none">
      {/* Resume Dialog */}
      {showResumeDialog && (
        <ResumeDialog
          roundNumber={roundState.roundNumber}
          arena={arena}
          level={level}
          onResume={handleResumeGame}
          onDiscard={handleDiscardSave}
        />
      )}

      {/* Income Summary Modal */}
      {showIncomeSummary && (
        <IncomeSummary
          playerIncome={playerIncomeDelta}
          aiIncome={aiIncomeDelta}
          playerScore={playerScore}
          aiScore={aiScore}
          roundNumber={roundState.roundNumber}
          incomeMultiplier={roundState.incomeMultiplier}
          isGameOver={roundState.roundNumber >= 12}
          onContinue={handleContinueAfterIncome}
        />
      )}

      {/* Top Status Bar */}
      <StatusBar
        playerScore={playerScore}
        aiScore={aiScore}
        playerIncomeDelta={playerIncomeDelta}
        aiIncomeDelta={aiIncomeDelta}
        roundNumber={roundState.roundNumber}
        timeRemaining={timeRemaining}
        timerActive={timerActive}
        aiCapabilityName={roundState.aiCapabilityName}
        aiCapabilityModifier={roundState.aiCapabilityModifier}
        aiCapabilityVisible={settings.aiCapabilityVisible}
        arena={arena}
        level={level}
        soundEnabled={settings.soundEnabled}
        onToggleSound={() => soundManager.setEnabled(!settings.soundEnabled)}
        onOpenSettings={onOpenSettings}
        onExit={onExit}
      />

      {/* Main Play Area */}
      <main className="flex-1 flex flex-col items-center justify-between p-2 sm:p-4 max-w-lg mx-auto w-full gap-2">
        {/* Round Progress Dots */}
        <RoundProgressDots currentRound={roundState.roundNumber} />

        {/* Late Game Event Banner */}
        <LateGameEventBanner
          type={roundState.lateGameEventType}
          message={roundState.lateGameMessage}
        />

        {/* Intel Phase Panel OR Active Game Board */}
        {roundState.phase === 'Intel' ? (
          <div className="flex-1 flex items-center justify-center w-full py-4">
            <IntelPanel
              intel={roundState.intel}
              intelHint={roundState.intelHint}
              intelConfidence={roundState.intelConfidence}
              roundNumber={roundState.roundNumber}
              onProceedToPlacing={() => {
                soundManager.playButtonClick();
                const nextState = engine.transitionToPlacing();
                setRoundState(nextState);
              }}
            />
          </div>
        ) : (
          <>
            {/* Probability Breakdown Tooltip */}
            <ProbabilityTooltip
              position={hoveredPosition}
              breakdown={probabilityBreakdown}
              onDismiss={() => {
                setHoveredPosition(null);
                setProbabilityBreakdown(null);
              }}
            />

            {/* Game Board */}
            <GameBoard
              board={roundState.board}
              playerPlacements={roundState.playerPlacements}
              aiPlacements={roundState.aiPlacements}
              playerLocks={roundState.playerLocks}
              aiLocks={roundState.aiLocks}
              scoutUsed={roundState.scoutUsed}
              scoutedPosition={roundState.scoutedPosition}
              scoutResult={roundState.scoutResult}
              phase={roundState.phase}
              currentResolvingPosition={currentResolvingPosition}
              clashResults={clashResults}
              onCellClick={handleCellClick}
              onMouseEnter={handleCellHover}
              onMouseLeave={handleCellLeave}
              scoutMode={scoutMode}
              lockMode={lockMode}
            />

            {/* Token Bar */}
            <TokenBar
              playerPlacements={roundState.playerPlacements}
              selectedToken={selectedToken}
              onSelectToken={tok => {
                soundManager.playTokenPlace();
                setSelectedToken(prev => (prev === tok ? null : tok));
              }}
              disabled={roundState.phase !== 'Placing'}
            />

            {/* Action Bar */}
            <ActionBar
              phase={roundState.phase}
              scoutEnabled={config.scoutEnabled}
              scoutUsed={roundState.scoutUsed}
              scoutMode={scoutMode}
              onToggleScout={() => {
                soundManager.playButtonClick();
                setScoutMode(!scoutMode);
                setLockMode(false);
              }}
              maxLocks={config.maxLocksPerRound}
              currentLocksCount={roundState.playerLocks.length}
              lockMode={lockMode}
              onToggleLock={() => {
                soundManager.playButtonClick();
                setLockMode(!lockMode);
                setScoutMode(false);
              }}
              tokensPlacedCount={Object.keys(roundState.playerPlacements).length}
              onClear={() => {
                soundManager.playButtonClick();
                setRoundState({
                  ...roundState,
                  playerPlacements: {},
                  playerLocks: []
                });
                setSelectedToken(1);
              }}
              onCommit={() => handleCommit(false)}
            />
          </>
        )}
      </main>
    </div>
  );
};
