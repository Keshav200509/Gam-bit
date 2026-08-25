package com.example.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.presentation.screens.game.GameScreen
import com.example.presentation.screens.game.Arena3GameScreen
import com.example.presentation.screens.intro.IntroScreen
import com.example.presentation.screens.settings.SettingsScreen
import com.example.presentation.screens.stats.StatsScreen
import com.example.presentation.screens.splash.SplashScreen
import com.example.presentation.screens.intro.IntroViewModel
import com.example.presentation.screens.arena.ArenaMapScreen
import com.example.domain.model.Arena
import com.example.domain.model.GameLevel

@Composable
fun GambitApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("splash") {
            val introViewModel: IntroViewModel = hiltViewModel()
            val onboardingCompleted by introViewModel.onboardingCompleted.collectAsState()

            SplashScreen(
                onSplashFinished = {
                    if (onboardingCompleted) {
                        navController.navigate("home") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("intro") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("intro") {
            val introViewModel: IntroViewModel = hiltViewModel()
            IntroScreen(
                onBeginClicked = {
                    introViewModel.completeOnboarding()
                    navController.navigate("home") {
                        popUpTo("intro") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            val homeViewModel = hiltViewModel<com.example.presentation.screens.home.HomeViewModel>()
            com.example.presentation.screens.home.HomeScreen(
                viewModel = homeViewModel,
                onNavigateToQuickPlay = {
                    navController.navigate("game?arenaName=ASCENDENCY&levelName=LEVEL_1")
                },
                onNavigateToArena = {
                    navController.navigate("mode_selection")
                },
                onNavigateToPvP = {
                    navController.navigate("friends")
                },
                onNavigateToDailyChallenge = {
                    navController.navigate("daily_challenge")
                },
                onNavigateToFriends = {
                    navController.navigate("friends")
                },
                onNavigateToProfile = {
                    navController.navigate("profile")
                },
                onNavigateToStats = {
                    navController.navigate("stats")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }

        composable("mode_selection") {
            val modeSelectionViewModel = hiltViewModel<com.example.presentation.screens.mode.ModeSelectionViewModel>()
            com.example.presentation.screens.mode.ModeSelectionScreen(
                viewModel = modeSelectionViewModel,
                onBack = {
                    navController.popBackStack()
                },
                onPlay = { arena, level ->
                    navController.navigate("game?arenaName=${arena.name}&levelName=${level.name}")
                }
            )
        }

        composable("arena_map") {
            ArenaMapScreen(
                onNavigateToGame = { arena, level ->
                    navController.navigate("game?arenaName=${arena.name}&levelName=${level.name}")
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToFriends = {
                    navController.navigate("friends")
                },
                onNavigateToProfile = {
                    navController.navigate("profile")
                }
            )
        }

        composable("friends") {
            val friendsViewModel = hiltViewModel<com.example.presentation.screens.friends.FriendsViewModel>()
            val state by friendsViewModel.uiState.collectAsState()
            com.example.presentation.screens.friends.FriendsScreen(
                viewModel = friendsViewModel,
                currentUserProfile = state.currentUserProfile,
                onNavigateToPvPMatch = { matchId ->
                    navController.navigate("pvp_game?matchId=$matchId") {
                        popUpTo("friends") { saveState = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("profile") {
            val profileViewModel = hiltViewModel<com.example.presentation.screens.profile.ProfileViewModel>()
            com.example.presentation.screens.profile.ProfileScreen(
                viewModel = profileViewModel,
                onBack = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate("splash") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "pvp_game?matchId={matchId}",
            arguments = listOf(
                navArgument("matchId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
            val pvpViewModel = hiltViewModel<com.example.presentation.screens.game.PvPGameViewModel>()
            com.example.presentation.screens.game.PvPGameScreen(
                viewModel = pvpViewModel,
                matchId = matchId,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "game?arenaName={arenaName}&levelName={levelName}&isDaily={isDaily}",
            arguments = listOf(
                navArgument("arenaName") { nullable = true; defaultValue = null },
                navArgument("levelName") { nullable = true; defaultValue = null },
                navArgument("isDaily") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val arenaName = backStackEntry.arguments?.getString("arenaName")
            if (arenaName == Arena.OBLIVION.name) {
                Arena3GameScreen(
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    },
                    onNavigateToStats = {
                        navController.navigate("stats")
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            } else {
                GameScreen(
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    },
                    onNavigateToStats = {
                        navController.navigate("stats")
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        composable("settings") {
            SettingsScreen(
                onBackClicked = {
                    navController.popBackStack()
                }
            )
        }

        composable("stats") {
            StatsScreen(
                onBackClicked = {
                    navController.popBackStack()
                }
            )
        }

        composable("daily_challenge") {
            val dailyChallengeViewModel = hiltViewModel<com.example.presentation.screens.daily.DailyChallengeViewModel>()
            com.example.presentation.screens.daily.DailyChallengeScreen(
                viewModel = dailyChallengeViewModel,
                onBack = { navController.popBackStack() },
                onStartChallenge = { arena, level ->
                    navController.navigate("game?arenaName=${arena.name}&levelName=${level.name}&isDaily=true")
                },
                onNavigateToLeaderboard = {
                    navController.navigate("daily_leaderboard")
                }
            )
        }

        composable("daily_leaderboard") {
            val dailyChallengeViewModel = hiltViewModel<com.example.presentation.screens.daily.DailyChallengeViewModel>()
            com.example.presentation.screens.daily.LeaderboardScreen(
                viewModel = dailyChallengeViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
