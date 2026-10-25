import React, { useState, useEffect } from 'react';
import { ArenaId, GameLevelId } from './types/game';
import { getStoredOnboardingCompleted, setStoredOnboardingCompleted, getStoredProfile, saveStoredProfile, getStoredSave } from './services/storage';
import { SplashScreen } from './screens/SplashScreen';
import { IntroScreen } from './screens/IntroScreen';
import { HomeScreen } from './screens/HomeScreen';
import { ModeSelectionScreen } from './screens/ModeSelectionScreen';
import { GameScreen } from './screens/GameScreen';
import { DailyChallengeScreen } from './screens/DailyChallengeScreen';
import { LeaderboardScreen } from './screens/LeaderboardScreen';
import { StatsScreen } from './screens/StatsScreen';
import { SettingsScreen } from './screens/SettingsScreen';
import { ProfileScreen } from './screens/ProfileScreen';
import { FriendsScreen } from './screens/FriendsScreen';
import { PvPGameScreen } from './screens/PvPGameScreen';

export type Screen =
  | 'splash'
  | 'intro'
  | 'home'
  | 'mode_selection'
  | 'game'
  | 'daily_challenge'
  | 'daily_leaderboard'
  | 'stats'
  | 'settings'
  | 'profile'
  | 'friends'
  | 'pvp_game';

export const App: React.FC = () => {
  const [currentScreen, setCurrentScreen] = useState<Screen>('splash');
  const [userProfile, setUserProfile] = useState(getStoredProfile());

  // Game arguments
  const [currentGameArena, setCurrentGameArena] = useState<ArenaId>('ASCENDENCY');
  const [currentGameLevel, setCurrentGameLevel] = useState<GameLevelId>('LEVEL_1');
  const [isDailyChallenge, setIsDailyChallenge] = useState<boolean>(false);
  const [dailySeed, setDailySeed] = useState<number>(0);
  const [pvpOpponentName, setPvpOpponentName] = useState<string>('Opponent');

  const hasSavedGame = !!getStoredSave();

  // Navigation handlers
  const handleSplashFinished = () => {
    const onboardingDone = getStoredOnboardingCompleted();
    if (onboardingDone) {
      setCurrentScreen('home');
    } else {
      setCurrentScreen('intro');
    }
  };

  const handleIntroComplete = (displayName: string, avatar: string) => {
    const updated = { ...userProfile, displayName, photoUrl: avatar };
    setUserProfile(updated);
    saveStoredProfile(updated);
    setStoredOnboardingCompleted(true);
    setCurrentScreen('home');
  };

  const handleStartGame = (arena: ArenaId, level: GameLevelId, isDaily: boolean = false, seed: number = 0) => {
    setCurrentGameArena(arena);
    setCurrentGameLevel(level);
    setIsDailyChallenge(isDaily);
    setDailySeed(seed);
    setCurrentScreen('game');
  };

  const handleResumeSavedGame = () => {
    const saved = getStoredSave();
    if (saved) {
      setCurrentGameArena(saved.arena);
      setCurrentGameLevel(saved.level);
      setIsDailyChallenge(saved.isDailyChallenge);
      setDailySeed(saved.dailyChallengeSeed);
    }
    setCurrentScreen('game');
  };

  const handleStartPvPDuel = (opponent: string) => {
    setPvpOpponentName(opponent);
    setCurrentScreen('pvp_game');
  };

  return (
    <div className="w-full min-h-screen bg-[#0A0B10] text-[#C8D0E0]">
      {currentScreen === 'splash' && (
        <SplashScreen onFinished={handleSplashFinished} />
      )}

      {currentScreen === 'intro' && (
        <IntroScreen onComplete={handleIntroComplete} />
      )}

      {currentScreen === 'home' && (
        <HomeScreen
          userProfile={userProfile}
          hasSavedGame={hasSavedGame}
          onQuickPlay={() => handleStartGame('ASCENDENCY', 'LEVEL_1')}
          onChooseArena={() => setCurrentScreen('mode_selection')}
          onDailyChallenge={() => setCurrentScreen('daily_challenge')}
          onPvP={() => setCurrentScreen('friends')}
          onProfile={() => setCurrentScreen('profile')}
          onStats={() => setCurrentScreen('stats')}
          onSettings={() => setCurrentScreen('settings')}
          onResumeGame={handleResumeSavedGame}
        />
      )}

      {currentScreen === 'mode_selection' && (
        <ModeSelectionScreen
          onBack={() => setCurrentScreen('home')}
          onPlay={(arena, level) => handleStartGame(arena, level)}
        />
      )}

      {currentScreen === 'game' && (
        <GameScreen
          arena={currentGameArena}
          level={currentGameLevel}
          isDailyChallenge={isDailyChallenge}
          dailyChallengeSeed={dailySeed}
          onExit={() => setCurrentScreen('home')}
          onOpenSettings={() => setCurrentScreen('settings')}
          onNavigateToStats={() => setCurrentScreen('stats')}
        />
      )}

      {currentScreen === 'daily_challenge' && (
        <DailyChallengeScreen
          onBack={() => setCurrentScreen('home')}
          onStartChallenge={(arena, level, seed) => handleStartGame(arena, level, true, seed)}
          onViewLeaderboard={() => setCurrentScreen('daily_leaderboard')}
        />
      )}

      {currentScreen === 'daily_leaderboard' && (
        <LeaderboardScreen onBack={() => setCurrentScreen('daily_challenge')} />
      )}

      {currentScreen === 'stats' && (
        <StatsScreen onBack={() => setCurrentScreen('home')} />
      )}

      {currentScreen === 'settings' && (
        <SettingsScreen onBack={() => setCurrentScreen('home')} />
      )}

      {currentScreen === 'profile' && (
        <ProfileScreen onBack={() => setCurrentScreen('home')} />
      )}

      {currentScreen === 'friends' && (
        <FriendsScreen
          onBack={() => setCurrentScreen('home')}
          onStartPvPMatch={handleStartPvPDuel}
        />
      )}

      {currentScreen === 'pvp_game' && (
        <PvPGameScreen
          opponentName={pvpOpponentName}
          onBack={() => setCurrentScreen('friends')}
        />
      )}
    </div>
  );
};

export default App;
