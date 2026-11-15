import type { RealtimeChannel, SupabaseClient } from '@supabase/supabase-js';
import {
  PVP_MOVE_EVENT,
  PvpConnectionStatus,
  PvpMessage,
  PvpPresence,
  isPvpMessage,
  parsePresenceState,
  roomChannelName
} from '../domain/pvpSync';

/**
 * Thin wrapper around a Supabase Realtime channel for one duel room.
 *
 * Kept separate from the screen so the transport can be replaced or faked in
 * tests; nothing here knows about the game engine.
 */

export interface PvpChannelHandlers {
  onMessage: (message: PvpMessage) => void;
  onPresence: (peers: PvpPresence[]) => void;
  onStatus: (status: PvpConnectionStatus) => void;
}

export interface PvpChannel {
  /** Sends a move to the room. No-op until the channel has joined. */
  broadcastMove: (message: PvpMessage) => void;
  /** Untracks presence and leaves the channel. Safe to call more than once. */
  leave: () => void;
}

const STATUS_BY_EVENT: Record<string, PvpConnectionStatus> = {
  SUBSCRIBED: 'connected',
  TIMED_OUT: 'error',
  CHANNEL_ERROR: 'error',
  CLOSED: 'closed'
};

export function joinPvpRoom(
  client: SupabaseClient,
  roomCode: string,
  self: PvpPresence,
  handlers: PvpChannelHandlers
): PvpChannel {
  let joined = false;
  let left = false;

  const channel: RealtimeChannel = client.channel(roomChannelName(roomCode), {
    config: {
      // Moves are only meaningful to the opponent; echoing them back would
      // re-apply our own move.
      broadcast: { self: false },
      presence: { key: self.peerId }
    }
  });

  channel
    .on('broadcast', { event: PVP_MOVE_EVENT }, ({ payload }: { payload?: unknown }) => {
      if (isPvpMessage(payload)) handlers.onMessage(payload);
    })
    .on('presence', { event: 'sync' }, () => {
      handlers.onPresence(parsePresenceState(channel.presenceState(), self.peerId));
    })
    .subscribe((status: string) => {
      const mapped = STATUS_BY_EVENT[status];
      if (mapped) handlers.onStatus(mapped);
      if (status === 'SUBSCRIBED') {
        joined = true;
        void channel.track({ peerId: self.peerId, name: self.name });
      } else if (status === 'CLOSED' || status === 'CHANNEL_ERROR' || status === 'TIMED_OUT') {
        joined = false;
      }
    });

  return {
    broadcastMove(message) {
      if (!joined || left) return;
      void channel.send({ type: 'broadcast', event: PVP_MOVE_EVENT, payload: message });
    },
    leave() {
      if (left) return;
      left = true;
      joined = false;
      void channel.untrack();
      void client.removeChannel(channel);
    }
  };
}
