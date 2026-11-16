import React, { useState, useEffect, useRef } from 'react';
import { ArrowLeft, UserCheck, Wifi, WifiOff, Users } from 'lucide-react';
import { Board, Position, Token, ClashResult } from '../types/game';
import { generateBoard } from '../domain/boardGenerator';
import {
  GameMovePayload,
  PVP_MOVE_EVENT,
  PvpMessage,
  resolveRound,
  seededRandomFromRoomCode,
  senderRoleForPhase
} from '../domain/pvpSync';
import { posKey } from '../domain/positionUtils';
import { soundManager } from '../services/soundManager';
import { haptics } from '../services/haptics';
import { getStoredProfile } from '../services/storage';
import { usePvpSession } from '../hooks/usePvpSession';
import { GameBoard } from '../components/GameBoard';
import { TokenBar } from '../components/TokenBar';
import { ActionBar } from '../components/ActionBar';
import { RoundProgressDots } from '../components/RoundProgressDots';
import { IncomeSummary } from '../components/IncomeSummary';
import { ResultScreen } from './ResultScreen';

interface PvPGameScreenProps {
  opponentName: string;
  /** Present for an online duel; absent keeps the original local hotseat mode. */
  roomCode?: string;
  onBack: () => void;
}

export const PvPGameScreen: React.FC<PvPGameScreenProps> = ({ opponentName, roomCode, onBack }) => {
  const isOnline = !!roomCode;
  const [myName] = useState(() => getStoredProfile().displayName || 'Commander');

  const [roundNumber, setRoundNumber] = useState<number>(1);
  const [board, setBoard] = useState<Board>(() =>
    isOnline ? generateBoard(seededRandomFromRoomCode(roomCode as string)) : generateBoard()
  );
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
  const [notice, setNotice] = useState<string | null>(null);

  const isP1Turn = turnState === 'P1_PLACING';
  const isP2Turn = turnState === 'P2_PLACING';
  const currentPlacements = isP1Turn ? p1Placements : isP2Turn ? p2Placements : {};
  const currentLocks = isP1Turn ? p1Locks : isP2Turn ? p2Locks : [];

  // ----- Online session ------------------------------------------------------
  // The Realtime callback must always see the current round/phase, so the mutable
  // values it reads live in refs rather than in the closure.
  const turnStateRef = useRef(turnState);
  turnStateRef.current = turnState;
  const roundRef = useRef(roundNumber);
  roundRef.current = roundNumber;
  const boardRef = useRef(board);
  boardRef.current = board;
  /** This client's committed orders for the current round. */
  const localMoveRef = useRef<GameMovePayload | null>(null);
  /** The opponent's orders for the current round. */
  const remoteMoveRef = useRef<GameMovePayload | null>(null);
  /** Moves that arrived before this client finished the previous round's income screen. */
  const pendingMovesRef = useRef<Map<number, GameMovePayload>>(new Map());

  /**
   * Applies the opponent's move to the local engine.
   *
   * Only moves are exchanged, never board state: both clients re-run the same
   * `resolveRound` on the same inputs, so they stay in lockstep.
   */
  const ingestRemoteMove = (payload: GameMovePayload) => {
    const phase = turnStateRef.current;
    // Placement order is fixed (P1 then P2), so the phase tells us who sent this.
    const senderRole = senderRoleForPhase(phase);
    if (!senderRole) return;

    remoteMoveRef.current = payload;

    if (senderRole === 'P1') {
      // The opponent (P1) has committed; it is now this client's (P2) turn.
      setP1Placements(payload.placements);
      setP1Locks(payload.locks);
      setTurnState('P2_PLACING');
      return;
    }

    // The opponent (P2) has committed and this client (P1) already did: resolve.
    setP2Placements(payload.placements);
    setP2Locks(payload.locks);
    const local = localMoveRef.current;
    if (local) {
      void runResolution(local.placements, local.locks, payload.placements, payload.locks, payload.round);
    }
  };

  const handleRemoteMessage = (message: PvpMessage) => {
    const { round } = message.payload;
    if (round === roundRef.current) {
      ingestRemoteMove(message.payload);
    } else if (round === roundRef.current + 1) {
      // Opponent finished the income screen first; hold the move until this
      // client advances a round, otherwise it would be dropped as "stale".
      pendingMovesRef.current.set(round, message.payload);
    }
  };

  const {
    role,
    status,
    opponentName: remoteName,
    hasOpponent,
    opponentLeft,
    sendMove,
    isOnline: connected
  } = usePvpSession(roomCode ?? '', myName, handleRemoteMessage);

  const myRole = isOnline ? role : 'P1';
  const displayOpponent = remoteName ?? opponentName;

  // Apply any move that was buffered while the income screen was open.
  useEffect(() => {
    const pending = pendingMovesRef.current.get(roundNumber);
    if (pending) {
      pendingMovesRef.current.delete(roundNumber);
      ingestRemoteMove(pending);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roundNumber]);

  useEffect(() => {
    if (isOnline && opponentLeft) setNotice(`${displayOpponent} lost the comm-link.`);
  }, [isOnline, opponentLeft, displayOpponent]);

  useEffect(() => {
    if (isOnline && hasOpponent) setNotice(null);
  }, [isOnline, hasOpponent]);

  // ----- Local turn handling -------------------------------------------------
  const canLocalPlace = isOnline
    ? hasOpponent && ((isP1Turn && myRole === 'P1') || (isP2Turn && myRole === 'P2'))
    : isP1Turn || isP2Turn;

  const handleCellClick = (pos: Position) => {
    if (!isP1Turn && !isP2Turn) return;
    if (isOnline && !canLocalPlace) return;
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

  const broadcastMove = (placements: Record<string, Token>, locks: Position[]) => {
    sendMove({ type: PVP_MOVE_EVENT, payload: { round: roundNumber, placements, locks } });
  };

  const handleCommitP1 = () => {
    soundManager.playButtonClick();
    setLockMode(false);
    if (isOnline) {
      localMoveRef.current = { round: roundNumber, placements: p1Placements, locks: p1Locks };
      broadcastMove(p1Placements, p1Locks);
      setTurnState('P2_PLACING');
      return;
    }
    setTurnState('PASS_TO_P2');
    setSelectedToken(1);
  };

  const handleStartP2 = () => {
    soundManager.playButtonClick();
    setTurnState('P2_PLACING');
  };

  const handleCommitP2 = () => {
    soundManager.playButtonClick();
    setLockMode(false);
    if (isOnline) {
      const move: GameMovePayload = { round: roundNumber, placements: p2Placements, locks: p2Locks };
      broadcastMove(p2Placements, p2Locks);
      // P1 can only reach P2_PLACING after its own move arrived, so it is present.
      const remote = remoteMoveRef.current;
      if (remote) void runResolution(remote.placements, remote.locks, move.placements, move.locks, roundNumber);
      return;
    }
    setTurnState('RESOLVING');
    void runResolution(p1Placements, p1Locks, p2Placements, p2Locks, roundNumber);
  };

  /**
   * Resolves a round locally and reveals each clash in sequence. Both clients
   * call this with identical inputs, so they converge on the same board.
   */
  const runResolution = async (
    p1p: Record<string, Token>,
    p1l: Position[],
    p2p: Record<string, Token>,
    p2l: Position[],
    round: number
  ) => {
    setTurnState('RESOLVING');
    const resolution = resolveRound(boardRef.current, round, p1p, p1l, p2p, p2l);

    for (const step of resolution.steps) {
      setCurrentResolvingPosition(step.position);
      soundManager.playTokenPlace();
      await new Promise(r => setTimeout(r, 250));
      setClashResults(prev => ({ ...prev, [posKey(step.position)]: step.result }));
      if (step.result.type.startsWith('Player')) soundManager.playClashWin();
      else if (step.result.type.startsWith('AI')) soundManager.playClashLose();
      else soundManager.playClashTie();
      setBoard(step.board);
      await new Promise(r => setTimeout(r, 300));
    }

    setCurrentResolvingPosition(null);
    setBoard(resolution.board);
    setPlayer1Score(prev => prev + resolution.playerIncome);
    setPlayer2Score(prev => prev + resolution.aiIncome);
    setP1IncomeDelta(resolution.playerIncome);
    setP2IncomeDelta(resolution.aiIncome);
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
    localMoveRef.current = null;
    remoteMoveRef.current = null;

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
          pendingMovesRef.current.clear();
          localMoveRef.current = null;
          remoteMoveRef.current = null;
          setBoard(isOnline ? generateBoard(seededRandomFromRoomCode(roomCode as string)) : generateBoard());
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

        {isOnline ? (
          <div className="flex items-center gap-1 px-2 py-1 rounded-lg bg-[#12131A] border border-[#1E2030] font-mono text-[10px]">
            {connected && hasOpponent ? (
              <>
                <Wifi size={12} className="text-[#2DD4BF]" />
                <span className="text-[#2DD4BF]">LIVE</span>
              </>
            ) : (
              <>
                <WifiOff size={12} className="text-amber-400" />
                <span className="text-amber-400">{status === 'connecting' ? 'LINKING' : 'OFFLINE'}</span>
              </>
            )}
          </div>
        ) : (
          <div className="w-12" />
        )}
      </header>

      {/* Room / connection strip */}
      {isOnline && (
        <div className="px-3 py-1.5 bg-[#0E1017] border-b border-[#1E2030] font-mono text-[10px] flex items-center justify-between">
          <span className="text-[#4A5068]">
            COMM-LINK <span className="text-[#2DD4BF]">room:{(roomCode as string).toUpperCase()}</span>
          </span>
          <span className={hasOpponent ? 'text-[#2DD4BF]' : 'text-amber-400'}>
            <Users size={11} className="inline mr-1 -mt-0.5" />
            {hasOpponent ? `${displayOpponent} LINKED` : 'AWAITING OPPONENT'}
          </span>
        </div>
      )}

      {notice && (
        <div className="px-3 py-1.5 bg-amber-500/10 border-b border-amber-500/30 text-amber-300 font-mono text-[10px]">
          {notice}
        </div>
      )}

      {/* Scores Banner */}
      <div className="grid grid-cols-2 p-3 bg-[#12131A] border-b border-[#1E2030] font-mono text-center">
        <div className="border-r border-[#1E2030]">
          <span className="text-[10px] text-[#2DD4BF] font-bold block">
            {isOnline ? `${myRole ?? 'P1'} (YOU)` : 'PLAYER 1 (YOU)'}
          </span>
          <span className="text-2xl font-black text-white">{player1Score}</span>
        </div>
        <div>
          <span className="text-[10px] text-purple-400 font-bold block">{displayOpponent}</span>
          <span className="text-2xl font-black text-white">{player2Score}</span>
        </div>
      </div>

      {/* Main Area */}
      <main className="flex-1 flex flex-col items-center justify-between p-4 max-w-lg mx-auto w-full gap-2">
        <RoundProgressDots currentRound={roundNumber} />

        {/* Waiting for an opponent to join the room (online only) */}
        {isOnline && !hasOpponent ? (
          <div className="flex-1 flex flex-col items-center justify-center p-6 bg-[#12131A] rounded-2xl border border-teal-500/40 text-center my-6">
            <Wifi size={48} className="text-[#2DD4BF] mb-3 animate-pulse" />
            <h3 className="text-lg font-mono font-extrabold text-white uppercase mb-1">AWAITING OPPONENT</h3>
            <p className="text-xs font-sans text-[#C8D0E0] mb-3">
              Share this frequency so your opponent can join the duel:
            </p>
            <div className="px-4 py-2 bg-[#0E1017] rounded-xl border border-[#1E2030] font-mono text-lg font-black text-[#2DD4BF] tracking-widest">
              {(roomCode as string).toUpperCase()}
            </div>
          </div>
        ) : /* Turn Handoff Interstitial (hotseat only) */
        !isOnline && turnState === 'PASS_TO_P2' ? (
          <div className="flex-1 flex flex-col items-center justify-center p-6 bg-[#12131A] rounded-2xl border border-purple-500/40 text-center my-6">
            <UserCheck size={48} className="text-[#A78BFA] mb-3 animate-pulse" />
            <h3 className="text-lg font-mono font-extrabold text-white uppercase mb-1">
              PLAYER 1 ORDERS LOCKED
            </h3>
            <p className="text-xs font-sans text-[#C8D0E0] mb-6">
              Hand the terminal to <strong>{displayOpponent}</strong> to deploy their 5 tokens secretly.
            </p>
            <button
              type="button"
              onClick={handleStartP2}
              className="py-3 px-6 rounded-xl font-mono text-xs font-black bg-[#A78BFA] text-black shadow-lg shadow-purple-500/30 cursor-pointer"
            >
              READY — START {displayOpponent.toUpperCase()}'S TURN
            </button>
          </div>
        ) : (
          <>
            <div className="flex items-center gap-2 px-3 py-1 bg-[#12131A] rounded-full border border-[#1E2030] text-xs font-mono">
              <span className={`w-2 h-2 rounded-full ${isP1Turn ? 'bg-[#2DD4BF]' : 'bg-[#A78BFA]'} animate-ping`} />
              <span className="font-bold text-white">
                {!canLocalPlace && turnState !== 'RESOLVING'
                  ? 'WAITING: OPPONENT DEPLOYING'
                  : isP1Turn
                  ? 'DEPLOYING: PLAYER 1'
                  : isP2Turn
                  ? `DEPLOYING: ${displayOpponent}`
                  : 'CLASH RESOLUTION'}
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

            {canLocalPlace && (
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
