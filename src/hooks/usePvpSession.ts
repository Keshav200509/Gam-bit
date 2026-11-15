import { useCallback, useEffect, useRef, useState } from 'react';
import {
  PvpConnectionStatus,
  PvpMessage,
  PvpPresence,
  PvpRole,
  createPeerId,
  deriveRoles
} from '../domain/pvpSync';
import { PvpChannel, joinPvpRoom } from '../services/pvpChannel';
import { supabase } from '../lib/supabase';

/**
 * Connects this client to a duel room and exposes the room's live state.
 *
 * The hook owns the connection only; it does not touch game state. Incoming
 * moves are handed to `onMove`, which the screen applies to its local engine.
 */
export function usePvpSession(
  roomCode: string,
  displayName: string,
  onMove: (message: PvpMessage) => void
): {
  selfPeerId: string;
  role: PvpRole | null;
  peers: PvpPresence[];
  status: PvpConnectionStatus;
  opponentName: string | null;
  hasOpponent: boolean;
  opponentLeft: boolean;
  sendMove: (message: PvpMessage) => void;
  isOnline: boolean;
} {
  const [selfPeerId] = useState(createPeerId);
  const [peers, setPeers] = useState<PvpPresence[]>([]);
  const [status, setStatus] = useState<PvpConnectionStatus>(supabase ? 'connecting' : 'closed');
  const [opponentLeft, setOpponentLeft] = useState(false);
  const [hasConnected, setHasConnected] = useState(false);

  // Keep the latest callback without tearing down the channel on every render.
  const onMoveRef = useRef(onMove);
  onMoveRef.current = onMove;
  const channelRef = useRef<PvpChannel | null>(null);
  const hadOpponentRef = useRef(false);

  useEffect(() => {
    if (!supabase || !roomCode) return;

    const channel = joinPvpRoom(
      supabase,
      roomCode,
      { peerId: selfPeerId, name: displayName },
      {
        onMessage: message => onMoveRef.current(message),
        onPresence: next => {
          setPeers(next);
          if (next.length > 0) {
            hadOpponentRef.current = true;
            setOpponentLeft(false);
          } else if (hadOpponentRef.current) {
            setOpponentLeft(true);
          }
        },
        onStatus: next => {
          setStatus(next);
          if (next === 'connected') setHasConnected(true);
        }
      }
    );

    channelRef.current = channel;
    return () => {
      channelRef.current = null;
      channel.leave();
    };
  }, [roomCode, selfPeerId, displayName]);

  const sendMove = useCallback((message: PvpMessage) => {
    channelRef.current?.broadcastMove(message);
  }, []);

  const role = deriveRoles(selfPeerId, peers.map(peer => peer.peerId));
  const opponentName = peers[0]?.name ?? null;

  return {
    selfPeerId,
    role: role?.self ?? null,
    peers,
    status,
    opponentName,
    hasOpponent: peers.length > 0,
    opponentLeft,
    sendMove,
    isOnline: status === 'connected' && hasConnected
  };
}
