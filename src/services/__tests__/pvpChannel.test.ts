import { describe, expect, it, vi } from 'vitest';
import type { SupabaseClient } from '@supabase/supabase-js';
import { PVP_MOVE_EVENT, PvpMessage } from '../../domain/pvpSync';
import { joinPvpRoom } from '../pvpChannel';

/**
 * A stand-in for a Realtime channel. It records the bindings the service
 * registers and lets a test drive subscribe/presence/broadcast callbacks.
 */
function fakeChannel() {
  const bindings: Record<string, (arg: unknown) => void> = {};
  const channel = {
    on(type: string, filter: { event: string }, callback: (arg: unknown) => void) {
      bindings[`${type}:${filter.event}`] = callback;
      return channel;
    },
    subscribe(callback: (status: string) => void) {
      subscribeCallback = callback;
      return channel;
    },
    send: vi.fn(() => Promise.resolve('ok')),
    track: vi.fn(() => Promise.resolve('ok')),
    untrack: vi.fn(() => Promise.resolve('ok')),
    presenceState: vi.fn(() => ({}))
  };
  let subscribeCallback: ((status: string) => void) = () => {};
  return {
    channel,
    bindings,
    fireSubscribe: (status: string) => subscribeCallback(status),
    get send() {
      return channel.send;
    },
    get track() {
      return channel.track;
    },
    get untrack() {
      return channel.untrack;
    }
  };
}

function fakeClient(channel: unknown) {
  const removeChannel = vi.fn(() => Promise.resolve('ok'));
  const client = {
    channel: vi.fn(() => channel),
    removeChannel
  };
  return { client: client as unknown as SupabaseClient, removeChannel, channelFactory: client.channel };
}

const MOVE: PvpMessage = {
  type: PVP_MOVE_EVENT,
  payload: { round: 1, placements: { '0,0': { value: 5, isLocked: false } }, locks: [] }
};

describe('joinPvpRoom', () => {
  it('subscribes to the room topic with presence keyed by peer id', () => {
    const fake = fakeChannel();
    const { client, channelFactory } = fakeClient(fake.channel);
    joinPvpRoom(client, 'GAMBIT-1A2B', { peerId: 'peer_a', name: 'A' }, {
      onMessage: () => {},
      onPresence: () => {},
      onStatus: () => {}
    });
    expect(channelFactory).toHaveBeenCalledWith('room:GAMBIT1A2B', {
      config: { broadcast: { self: false }, presence: { key: 'peer_a' } }
    });
  });

  it('reports connection status changes', () => {
    const fake = fakeChannel();
    const { client } = fakeClient(fake.channel);
    const onStatus = vi.fn();
    joinPvpRoom(client, 'ROOM', { peerId: 'peer_a', name: 'A' }, {
      onMessage: () => {},
      onPresence: () => {},
      onStatus
    });

    fake.fireSubscribe('SUBSCRIBED');
    fake.fireSubscribe('CHANNEL_ERROR');
    expect(onStatus).toHaveBeenNthCalledWith(1, 'connected');
    expect(onStatus).toHaveBeenNthCalledWith(2, 'error');
  });

  it('tracks presence once subscribed', () => {
    const fake = fakeChannel();
    const { client } = fakeClient(fake.channel);
    joinPvpRoom(client, 'ROOM', { peerId: 'peer_a', name: 'Commander Ares' }, {
      onMessage: () => {},
      onPresence: () => {},
      onStatus: () => {}
    });
    fake.fireSubscribe('SUBSCRIBED');
    expect(fake.track).toHaveBeenCalledWith({ peerId: 'peer_a', name: 'Commander Ares' });
  });

  it('delivers only well-formed moves to onMessage', () => {
    const fake = fakeChannel();
    const { client } = fakeClient(fake.channel);
    const onMessage = vi.fn();
    joinPvpRoom(client, 'ROOM', { peerId: 'peer_a', name: 'A' }, {
      onMessage,
      onPresence: () => {},
      onStatus: () => {}
    });

    const handler = fake.bindings[`broadcast:${PVP_MOVE_EVENT}`];
    handler({ payload: MOVE });
    handler({ payload: { type: PVP_MOVE_EVENT, payload: { round: 99 } } });
    handler({ payload: null });

    expect(onMessage).toHaveBeenCalledTimes(1);
    expect(onMessage).toHaveBeenCalledWith(MOVE);
  });

  it('maps presence state into peers', () => {
    const fake = fakeChannel();
    const { client } = fakeClient(fake.channel);
    const onPresence = vi.fn();
    joinPvpRoom(client, 'ROOM', { peerId: 'peer_a', name: 'A' }, {
      onMessage: () => {},
      onPresence,
      onStatus: () => {}
    });

    fake.channel.presenceState.mockReturnValue({
      peer_a: [{ peerId: 'peer_a', name: 'A' }],
      peer_b: [{ peerId: 'peer_b', name: 'B' }]
    });
    fake.bindings['presence:sync'](undefined);
    expect(onPresence).toHaveBeenCalledWith([{ peerId: 'peer_b', name: 'B' }]);
  });

  it('does not send before the channel has joined', () => {
    const fake = fakeChannel();
    const { client } = fakeClient(fake.channel);
    const session = joinPvpRoom(client, 'ROOM', { peerId: 'peer_a', name: 'A' }, {
      onMessage: () => {},
      onPresence: () => {},
      onStatus: () => {}
    });

    session.broadcastMove(MOVE);
    expect(fake.send).not.toHaveBeenCalled();

    fake.fireSubscribe('SUBSCRIBED');
    session.broadcastMove(MOVE);
    expect(fake.send).toHaveBeenCalledWith({ type: 'broadcast', event: PVP_MOVE_EVENT, payload: MOVE });
  });

  it('leaves the channel once, even if leave is called twice', () => {
    const fake = fakeChannel();
    const { client, removeChannel } = fakeClient(fake.channel);
    const session = joinPvpRoom(client, 'ROOM', { peerId: 'peer_a', name: 'A' }, {
      onMessage: () => {},
      onPresence: () => {},
      onStatus: () => {}
    });
    fake.fireSubscribe('SUBSCRIBED');

    session.leave();
    session.leave();
    expect(fake.untrack).toHaveBeenCalledTimes(1);
    expect(removeChannel).toHaveBeenCalledTimes(1);
  });

  it('stops sending after leave', () => {
    const fake = fakeChannel();
    const { client } = fakeClient(fake.channel);
    const session = joinPvpRoom(client, 'ROOM', { peerId: 'peer_a', name: 'A' }, {
      onMessage: () => {},
      onPresence: () => {},
      onStatus: () => {}
    });
    fake.fireSubscribe('SUBSCRIBED');
    session.leave();
    session.broadcastMove(MOVE);
    expect(fake.send).not.toHaveBeenCalled();
  });
});
