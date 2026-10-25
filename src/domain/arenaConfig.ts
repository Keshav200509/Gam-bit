import { ArenaConfiguration, ArenaId, GameLevelId, ArenaInfo } from '../types/game';

export const ARENA_INFOS: Record<ArenaId, ArenaInfo> = {
  ASCENDENCY: {
    id: 'ASCENDENCY',
    displayName: 'Ascendency',
    description: 'Human moves first. AI counters. Use your intelligence tools wisely.',
    tagline: 'Tactical Reconnaissance'
  },
  CONFRONTATION: {
    id: 'CONFRONTATION',
    displayName: 'Confrontation',
    description: 'Both commit at once. Pure psychological warfare and tactical anticipation.',
    tagline: 'Simultaneous Clash'
  },
  OBLIVION: {
    id: 'OBLIVION',
    displayName: 'Oblivion',
    description: 'No hints. No scout. No mercy. Pure tactical intuition and calculated bluffing.',
    tagline: 'Total Blindness'
  }
};

export function getArenaConfig(arena: ArenaId, level: GameLevelId): ArenaConfiguration {
  switch (arena) {
    case 'ASCENDENCY':
      switch (level) {
        case 'LEVEL_1':
          return {
            arena,
            level,
            aiCapabilityMin: -10,
            aiCapabilityMax: 5,
            intelTrickinessMultiplier: 0.7,
            maxLocksPerRound: 2,
            intelEnabled: true,
            scoutEnabled: true,
            probabilityPreviewEnabled: true,
            humanMovesFirst: true,
            aiBluffRate: 0.15
          };
        case 'LEVEL_2':
          return {
            arena,
            level,
            aiCapabilityMin: -5,
            aiCapabilityMax: 10,
            intelTrickinessMultiplier: 1.0,
            maxLocksPerRound: 2,
            intelEnabled: true,
            scoutEnabled: true,
            probabilityPreviewEnabled: true,
            humanMovesFirst: true,
            aiBluffRate: 0.15
          };
        case 'LEVEL_3':
          return {
            arena,
            level,
            aiCapabilityMin: 0,
            aiCapabilityMax: 10,
            intelTrickinessMultiplier: 1.3,
            maxLocksPerRound: 1,
            intelEnabled: true,
            scoutEnabled: true,
            probabilityPreviewEnabled: true,
            humanMovesFirst: true,
            aiBluffRate: 0.15
          };
      }
      break;

    case 'CONFRONTATION':
      switch (level) {
        case 'LEVEL_1':
          return {
            arena,
            level,
            aiCapabilityMin: -10,
            aiCapabilityMax: 10,
            intelTrickinessMultiplier: 1.0,
            maxLocksPerRound: 2,
            intelEnabled: true,
            scoutEnabled: true,
            probabilityPreviewEnabled: true,
            humanMovesFirst: false,
            aiBluffRate: 0.15
          };
        case 'LEVEL_2':
          return {
            arena,
            level,
            aiCapabilityMin: -5,
            aiCapabilityMax: 10,
            intelTrickinessMultiplier: 1.3,
            maxLocksPerRound: 2,
            intelEnabled: true,
            scoutEnabled: true,
            probabilityPreviewEnabled: true,
            humanMovesFirst: false,
            aiBluffRate: 0.15
          };
        case 'LEVEL_3':
          return {
            arena,
            level,
            aiCapabilityMin: 0,
            aiCapabilityMax: 10,
            intelTrickinessMultiplier: 1.5,
            maxLocksPerRound: 1,
            intelEnabled: true,
            scoutEnabled: true,
            probabilityPreviewEnabled: true,
            humanMovesFirst: false,
            aiBluffRate: 0.15
          };
      }
      break;

    case 'OBLIVION':
      switch (level) {
        case 'LEVEL_1':
          return {
            arena,
            level,
            aiCapabilityMin: -10,
            aiCapabilityMax: 5,
            intelTrickinessMultiplier: 1.0,
            maxLocksPerRound: 1,
            intelEnabled: false,
            scoutEnabled: false,
            probabilityPreviewEnabled: false,
            humanMovesFirst: false,
            aiBluffRate: 0.10
          };
        case 'LEVEL_2':
          return {
            arena,
            level,
            aiCapabilityMin: -5,
            aiCapabilityMax: 10,
            intelTrickinessMultiplier: 1.0,
            maxLocksPerRound: 1,
            intelEnabled: false,
            scoutEnabled: false,
            probabilityPreviewEnabled: false,
            humanMovesFirst: false,
            aiBluffRate: 0.20
          };
        case 'LEVEL_3':
          return {
            arena,
            level,
            aiCapabilityMin: 0,
            aiCapabilityMax: 10,
            intelTrickinessMultiplier: 1.0,
            maxLocksPerRound: 0,
            intelEnabled: false,
            scoutEnabled: false,
            probabilityPreviewEnabled: false,
            humanMovesFirst: false,
            aiBluffRate: 0.30
          };
      }
      break;
  }
}
