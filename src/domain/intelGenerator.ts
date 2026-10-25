import { AIPlan, Board, IntelHint, IntelType } from '../types/game';
import { getRegionName, parsePosKey } from './positionUtils';

const REGIONS = [
  'northwest',
  'northeast',
  'southwest',
  'southeast',
  'north',
  'south',
  'east',
  'west',
  'center'
];

export function generateIntel(
  aiPlan: AIPlan,
  board: Board,
  trickiness: number,
  isArena1: boolean = false,
  seedRandom?: () => number
): IntelHint {
  const rand = seedRandom || Math.random;

  const isTrue = rand() < 0.7;

  const reliabilityLabel =
    trickiness <= 0.15
      ? 'Source confidence: HIGH'
      : trickiness <= 0.35
      ? 'Source confidence: MODERATE — corroborate before committing'
      : trickiness <= 0.6
      ? 'Source confidence: MIXED — partial corroboration only'
      : trickiness <= 0.85
      ? 'Source confidence: LOW — single unverified source'
      : 'Source confidence: SUSPECT — possible counter-intel';

  const placementEntries = Object.entries(aiPlan.placements);

  if (isArena1) {
    const typeIndex = Math.floor(rand() * 3);
    if (typeIndex === 0) {
      let region = REGIONS[Math.floor(rand() * REGIONS.length)];
      if (isTrue && placementEntries.length > 0) {
        const [key] = placementEntries[Math.floor(rand() * placementEntries.length)];
        const pos = parsePosKey(key);
        region = getRegionName(pos.row, pos.col);
      }
      return {
        text: `The AI plans to focus on ${region} this round`,
        isTrue,
        reliabilityLabel,
        type: 'TOKEN_REGION',
        actualData: { region, tokenValue: 5 }
      };
    } else if (typeIndex === 1) {
      const strategy = aiPlan.strategy;
      let desc: string;
      if (isTrue) {
        switch (strategy) {
          case 'STEALING_CELLS':
            desc = 'steal your cells';
            break;
          case 'BUILDING_CLUSTER':
            desc = 'build adjacent clusters';
            break;
          case 'SECURING_HIGH_VALUE':
            desc = 'secure high-value zones';
            break;
          default:
            desc = 'expand territory';
            break;
        }
      } else {
        const fakeDesc = [
          'steal your cells',
          'build adjacent clusters',
          'secure high-value zones',
          'fortify positions'
        ];
        desc = fakeDesc[Math.floor(rand() * fakeDesc.length)];
      }
      return {
        text: `The AI intends to ${desc} this round`,
        isTrue,
        reliabilityLabel,
        type: 'STRATEGY',
        actualData: { strategy }
      };
    } else {
      let desc: string;
      if (isTrue) {
        switch (aiPlan.strategy) {
          case 'STEALING_CELLS':
            desc = 'undefended';
            break;
          case 'SECURING_HIGH_VALUE':
            desc = 'high-value';
            break;
          default:
            desc = 'contested';
            break;
        }
      } else {
        const fake = ['high-value', 'contested', 'undefended'];
        desc = fake[Math.floor(rand() * fake.length)];
      }
      return {
        text: `The AI's strategy targets ${desc} cells this round`,
        isTrue,
        reliabilityLabel,
        type: 'CELL_VALUE',
        actualData: { region: 'center', cellValue: 3 }
      };
    }
  }

  // Arena 2 / Confrontation / General
  const types: IntelType[] = ['TOKEN_REGION', 'STRATEGY', 'CELL_VALUE'];
  const chosenType = types[Math.floor(rand() * types.length)];

  switch (chosenType) {
    case 'TOKEN_REGION': {
      if (isTrue && placementEntries.length > 0) {
        const [key, token] = placementEntries[Math.floor(rand() * placementEntries.length)];
        const pos = parsePosKey(key);
        const region = getRegionName(pos.row, pos.col);
        return {
          text: `Sources suggest the AI's token ${token.value} is heading toward ${region}`,
          isTrue: true,
          reliabilityLabel,
          type: 'TOKEN_REGION',
          actualData: { tokenValue: token.value, region }
        };
      } else {
        let fakeToken = Math.floor(rand() * 5) + 1;
        let fakeRegion = REGIONS[Math.floor(rand() * REGIONS.length)];
        let loops = 0;
        while (
          loops < 20 &&
          placementEntries.some(([key, token]) => {
            const p = parsePosKey(key);
            return getRegionName(p.row, p.col) === fakeRegion && token.value === fakeToken;
          })
        ) {
          fakeToken = Math.floor(rand() * 5) + 1;
          fakeRegion = REGIONS[Math.floor(rand() * REGIONS.length)];
          loops++;
        }

        return {
          text: `Sources suggest the AI's token ${fakeToken} is heading toward ${fakeRegion}`,
          isTrue: false,
          reliabilityLabel,
          type: 'TOKEN_REGION',
          actualData: { tokenValue: fakeToken, region: fakeRegion }
        };
      }
    }

    case 'STRATEGY': {
      if (isTrue) {
        const strategyLabel = aiPlan.strategy.replace(/_/g, ' ').toLowerCase();
        return {
          text: `The AI appears focused on ${strategyLabel} this round`,
          isTrue: true,
          reliabilityLabel,
          type: 'STRATEGY',
          actualData: { strategy: aiPlan.strategy }
        };
      } else {
        const allStrategies = [
          'AGGRESSIVE_EXPANSION',
          'TERRITORY_FORTIFICATION',
          'STEALING_CELLS',
          'SECURING_HIGH_VALUE',
          'SPREADING_THIN',
          'BUILDING_CLUSTER'
        ].filter(s => s !== aiPlan.strategy);
        const fakeStrategy = allStrategies[Math.floor(rand() * allStrategies.length)];
        const strategyLabel = fakeStrategy.replace(/_/g, ' ').toLowerCase();
        return {
          text: `The AI appears focused on ${strategyLabel} this round`,
          isTrue: false,
          reliabilityLabel,
          type: 'STRATEGY',
          actualData: { strategy: fakeStrategy }
        };
      }
    }

    case 'CELL_VALUE': {
      if (isTrue && placementEntries.length > 0) {
        const [key] = placementEntries[Math.floor(rand() * placementEntries.length)];
        const pos = parsePosKey(key);
        const cellVal = board.cells[pos.row][pos.col].value;
        const region = getRegionName(pos.row, pos.col);
        return {
          text: `Intel indicates the AI is eyeing ${region}, where a ${cellVal}-point cell sits vulnerable`,
          isTrue: true,
          reliabilityLabel,
          type: 'CELL_VALUE',
          actualData: { region, cellValue: cellVal }
        };
      } else {
        let fakeRegion = REGIONS[Math.floor(rand() * REGIONS.length)];
        let fakeVal = Math.floor(rand() * 3) + 1;
        let loops = 0;
        while (
          loops < 20 &&
          placementEntries.some(([key]) => {
            const p = parsePosKey(key);
            return getRegionName(p.row, p.col) === fakeRegion && board.cells[p.row][p.col].value === fakeVal;
          })
        ) {
          fakeRegion = REGIONS[Math.floor(rand() * REGIONS.length)];
          fakeVal = Math.floor(rand() * 3) + 1;
          loops++;
        }

        return {
          text: `Intel indicates the AI is eyeing ${fakeRegion}, where a ${fakeVal}-point cell sits vulnerable`,
          isTrue: false,
          reliabilityLabel,
          type: 'CELL_VALUE',
          actualData: { region: fakeRegion, cellValue: fakeVal }
        };
      }
    }
  }
}
