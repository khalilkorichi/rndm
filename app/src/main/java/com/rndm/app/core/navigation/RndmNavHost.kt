package com.rndm.app.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.rndm.app.domain.model.DrawType
import com.rndm.app.presentation.draw.duel.ClubDuelDrawScreen
import com.rndm.app.presentation.draw.fixtures.MatchFixturesScreen
import com.rndm.app.presentation.draw.flipcards.FlipCardDrawScreen
import com.rndm.app.presentation.draw.result.DrawResultScreen
import com.rndm.app.presentation.draw.setup.DrawSetupScreen
import com.rndm.app.presentation.draw.spinlist.SpinListDrawScreen
import com.rndm.app.presentation.draw.wheel.WheelDrawScreen
import com.rndm.app.presentation.home.HomeScreen
import com.rndm.app.presentation.profile.detail.ProfileDetailScreen
import com.rndm.app.presentation.profile.edit.CreateEditProfileScreen
import com.rndm.app.presentation.profile.list.ProfileListScreen
import com.rndm.app.presentation.profile.player.PlayerProfileScreen
import com.rndm.app.presentation.profile.player.leaderboard.PlayersLeaderboardScreen
import com.rndm.app.presentation.settings.SettingsScreen
import com.rndm.app.presentation.tournament.archive.TournamentArchiveScreen
import com.rndm.app.presentation.tournament.bracket.TournamentBracketScreen
import com.rndm.app.presentation.tournament.create.CreateTournamentScreen
import com.rndm.app.presentation.tournament.detail.TournamentDetailScreen
import com.rndm.app.presentation.tournament.list.TournamentListScreen
import com.rndm.app.presentation.tournament.promotion.PromotionCandidateScreen
import com.rndm.app.presentation.update.UpdateBottomBar
import com.rndm.app.presentation.update.UpdateUiState
import com.rndm.app.presentation.update.UpdatesViewModel

// Professional motion curves (Material 3 Emphasized Motion Spec)
private val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

private fun isTopLevel(destination: NavDestination?): Boolean {
    if (destination == null) return false
    return BottomNavItem.items.any { item ->
        destination.hierarchy.any { it.hasRoute(item.destination::class) }
    }
}

@Composable
fun RndmNavHost(
    navController: NavHostController = rememberNavController(),
    updatesViewModel: UpdatesViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isTopLevelDestination = isTopLevel(currentDestination)
    val isOnSettingsScreen = currentDestination?.hasRoute(Destination.Settings::class) == true

    val updateUiState by updatesViewModel.uiState.collectAsStateWithLifecycle()
    val dismissedVersion by updatesViewModel.dismissedVersion.collectAsStateWithLifecycle()

    // Automatic throttled check on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updatesViewModel.checkForUpdatesThrottled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val showUpdateBar = remember(updateUiState, dismissedVersion, isOnSettingsScreen) {
        if (isOnSettingsScreen) return@remember false
        when (val state = updateUiState) {
            is UpdateUiState.UpdateAvailable -> state.info.versionName != dismissedVersion
            is UpdateUiState.Downloading,
            is UpdateUiState.Paused,
            is UpdateUiState.ReadyToInstall,
            is UpdateUiState.DownloadFailed -> true
            else -> false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(
            navController = navController,
            startDestination = Destination.Home,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                if (isTopLevel(initialState.destination) && isTopLevel(targetState.destination)) {
                    fadeIn(animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing)) +
                    scaleIn(initialScale = 0.98f, animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing))
                } else {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(durationMillis = 340, easing = EmphasizedDecelerateEasing),
                        initialOffset = { it }
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 260, easing = LinearOutSlowInEasing)
                    ) + scaleIn(
                        initialScale = 0.96f,
                        animationSpec = tween(durationMillis = 340, easing = EmphasizedDecelerateEasing)
                    )
                }
            },
            exitTransition = {
                if (isTopLevel(initialState.destination) && isTopLevel(targetState.destination)) {
                    fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing)) +
                    scaleOut(targetScale = 1.01f, animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing))
                } else {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(durationMillis = 300, easing = EmphasizedAccelerateEasing),
                        targetOffset = { -it / 4 }
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 240, easing = FastOutLinearInEasing)
                    ) + scaleOut(
                        targetScale = 0.94f,
                        animationSpec = tween(durationMillis = 300, easing = EmphasizedAccelerateEasing)
                    )
                }
            },
            popEnterTransition = {
                if (isTopLevel(initialState.destination) && isTopLevel(targetState.destination)) {
                    fadeIn(animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing)) +
                    scaleIn(initialScale = 0.98f, animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing))
                } else {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(durationMillis = 340, easing = EmphasizedDecelerateEasing),
                        initialOffset = { -it / 4 }
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 260, easing = LinearOutSlowInEasing)
                    ) + scaleIn(
                        initialScale = 0.94f,
                        animationSpec = tween(durationMillis = 340, easing = EmphasizedDecelerateEasing)
                    )
                }
            },
            popExitTransition = {
                if (isTopLevel(initialState.destination) && isTopLevel(targetState.destination)) {
                    fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing)) +
                    scaleOut(targetScale = 1.01f, animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing))
                } else {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(durationMillis = 300, easing = EmphasizedAccelerateEasing),
                        targetOffset = { it }
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 220, easing = FastOutLinearInEasing)
                    ) + scaleOut(
                        targetScale = 0.96f,
                        animationSpec = tween(durationMillis = 300, easing = EmphasizedAccelerateEasing)
                    )
                }
            }
        ) {
                composable<Destination.Home> {
                    HomeScreen(
                        onNavigateToDrawSetup = { profileId ->
                            navController.navigate(Destination.DrawSetup(profileId))
                        },
                        onNavigateToDrawMode = { profileId, drawType ->
                            navController.navigate(Destination.Draw(profileId, drawType))
                        },
                        onNavigateToClubDuelDraw = {
                            navController.navigate(Destination.ClubDuelDraw())
                        },
                        onNavigateToCreateProfile = {
                            navController.navigate(Destination.CreateEditProfile(profileId = 0L))
                        },
                        onNavigateToProfiles = {
                            navController.navigate(Destination.ProfileList)
                        },
                        onNavigateToTournaments = {
                            navController.navigate(Destination.TournamentList)
                        },
                        onNavigateToCreateTournament = {
                            navController.navigate(Destination.CreateTournament)
                        },
                        onNavigateToArchive = {
                            navController.navigate(Destination.TournamentArchive)
                        },
                        onNavigateToTournamentDetail = { tournamentId ->
                            navController.navigate(Destination.TournamentDetail(tournamentId))
                        }
                    )
                }

                composable<Destination.ProfileList> {
                    ProfileListScreen(
                        onNavigateToCreateProfileWithType = { type ->
                            navController.navigate(Destination.CreateEditProfile(profileId = 0L, typeName = type.name))
                        },
                        onNavigateToEditProfile = { profileId ->
                            navController.navigate(Destination.CreateEditProfile(profileId = profileId))
                        },
                        onNavigateToProfileDetail = { profileId ->
                            navController.navigate(Destination.ProfileDetail(profileId))
                        },
                        onNavigateToLeaderboard = {
                            navController.navigate(Destination.PlayersLeaderboard)
                        }
                    )
                }

                composable<Destination.ProfileDetail> { backStackEntry ->
                    val route: Destination.ProfileDetail = backStackEntry.toRoute()
                    ProfileDetailScreen(
                        profileId = route.profileId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToEdit = { id ->
                            navController.navigate(Destination.CreateEditProfile(profileId = id))
                        },
                        onNavigateToDraw = { id ->
                            navController.navigate(Destination.DrawSetup(profileId = id))
                        },
                        onNavigateToPlayerProfile = { playerName ->
                            navController.navigate(Destination.PlayerProfile(playerName))
                        },
                        onNavigateToLeaderboard = {
                            navController.navigate(Destination.PlayersLeaderboard)
                        }
                    )
                }

                composable<Destination.CreateEditProfile> { backStackEntry ->
                    val route: Destination.CreateEditProfile = backStackEntry.toRoute()
                    CreateEditProfileScreen(
                        profileId = route.profileId,
                        typeName = route.typeName,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable<Destination.DrawSetup> { backStackEntry ->
                    val route: Destination.DrawSetup = backStackEntry.toRoute()
                    DrawSetupScreen(
                        profileId = route.profileId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToCreateProfile = {
                            navController.navigate(Destination.CreateEditProfile(profileId = 0L))
                        },
                        onStartDraw = { profileId, drawType ->
                            navController.navigate(Destination.Draw(profileId, drawType))
                        }
                    )
                }

                composable<Destination.Draw> { backStackEntry ->
                    val route: Destination.Draw = backStackEntry.toRoute()
                    when (route.drawType) {
                        DrawType.WHEEL -> {
                            WheelDrawScreen(
                                profileId = route.profileId,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToResult = {
                                    navController.navigate(Destination.DrawResult) {
                                        popUpTo<Destination.Draw> { inclusive = true }
                                    }
                                },
                                onNavigateToFixtures = {
                                    navController.navigate(Destination.MatchFixtures)
                                },
                                onNavigateToEditProfile = { profileId ->
                                    navController.navigate(Destination.CreateEditProfile(profileId = profileId))
                                }
                            )
                        }
                        DrawType.FLIP_CARDS -> {
                            FlipCardDrawScreen(
                                profileId = route.profileId,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToResult = {
                                    navController.navigate(Destination.DrawResult) {
                                        popUpTo<Destination.Draw> { inclusive = true }
                                    }
                                }
                            )
                        }
                        DrawType.SPIN_LIST -> {
                            SpinListDrawScreen(
                                profileId = route.profileId,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToResult = {
                                    navController.navigate(Destination.DrawResult) {
                                        popUpTo<Destination.Draw> { inclusive = true }
                                    }
                                }
                            )
                        }
                        DrawType.ROUND_ROBIN -> {
                            DrawResultScreen(
                                profileId = route.profileId,
                                drawType = DrawType.ROUND_ROBIN,
                                onNavigateBack = { navController.popBackStack() },
                                onRedoDraw = { profileId, type ->
                                    navController.navigate(Destination.Draw(profileId, type)) {
                                        popUpTo<Destination.DrawResult> { inclusive = true }
                                    }
                                },
                                onNavigateToHome = {
                                    navController.navigate(Destination.Home) {
                                        popUpTo<Destination.Home> { inclusive = false }
                                    }
                                }
                            )
                        }
                    }
                }

                composable<Destination.ClubDuelDraw> {
                    ClubDuelDrawScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable<Destination.DrawResult> {
                    DrawResultScreen(
                        profileId = 0L,
                        drawType = DrawType.WHEEL,
                        onNavigateBack = { navController.popBackStack() },
                        onRedoDraw = { profileId, type ->
                            navController.navigate(Destination.Draw(profileId, type)) {
                                popUpTo<Destination.DrawResult> { inclusive = true }
                            }
                        },
                        onNavigateToHome = {
                            navController.navigate(Destination.Home) {
                                popUpTo<Destination.Home> { inclusive = false }
                            }
                        }
                    )
                }

                composable<Destination.MatchFixtures> {
                    MatchFixturesScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToDraw = { navController.popBackStack() }
                    )
                }

                composable<Destination.TournamentList> {
                    TournamentListScreen(
                        onNavigateToCreateTournament = {
                            navController.navigate(Destination.CreateTournament)
                        },
                        onNavigateToDrawSetup = {
                            navController.navigate(Destination.DrawSetup())
                        },
                        onNavigateToTournamentDetail = { tournamentId ->
                            navController.navigate(Destination.TournamentDetail(tournamentId))
                        },
                        onNavigateToEditTournament = { tournamentId ->
                            navController.navigate(Destination.TournamentDetail(tournamentId))
                        },
                        onNavigateToArchive = {
                            navController.navigate(Destination.TournamentArchive)
                        },
                        onNavigateToJoinTournament = {
                            navController.navigate(Destination.JoinTournament)
                        }
                    )
                }

                composable<Destination.JoinTournament> {
                    com.rndm.app.presentation.tournament.join.JoinTournamentScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToTournament = { tournamentId ->
                            navController.navigate(Destination.TournamentDetail(tournamentId)) {
                                popUpTo<Destination.JoinTournament> { inclusive = true }
                            }
                        }
                    )
                }

                composable<Destination.CreateTournament> {
                    CreateTournamentScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onTournamentCreated = { tournamentId ->
                            navController.navigate(Destination.TournamentDetail(tournamentId)) {
                                popUpTo<Destination.CreateTournament> { inclusive = true }
                            }
                        },
                        onNavigateToCreateProfile = {
                            navController.navigate(Destination.CreateEditProfile(profileId = 0L))
                        }
                    )
                }

                composable<Destination.TournamentDetail> { backStackEntry ->
                    val route: Destination.TournamentDetail = backStackEntry.toRoute()
                    TournamentDetailScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPromotion = { tournamentId ->
                            navController.navigate(Destination.PromotionCandidate(tournamentId))
                        },
                        onNavigateToBracket = { tournamentId ->
                            navController.navigate(Destination.TournamentBracket(tournamentId))
                        },
                        onNavigateToDraw = { profileId ->
                            navController.navigate(Destination.Draw(profileId = profileId, drawType = com.rndm.app.domain.model.DrawType.WHEEL))
                        },
                        onNavigateToPlayer = { playerName ->
                            navController.navigate(Destination.PlayerProfile(playerName))
                        }
                    )
                }

                composable<Destination.PromotionCandidate> { backStackEntry ->
                    val route: Destination.PromotionCandidate = backStackEntry.toRoute()
                    PromotionCandidateScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToBracket = { tournamentId ->
                            navController.navigate(Destination.TournamentBracket(tournamentId)) {
                                popUpTo<Destination.PromotionCandidate> { inclusive = true }
                            }
                        },
                        onNavigateToPlayer = { playerName ->
                            navController.navigate(Destination.PlayerProfile(playerName))
                        }
                    )
                }

                composable<Destination.TournamentBracket> {
                    TournamentBracketScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPlayer = { playerName ->
                            navController.navigate(Destination.PlayerProfile(playerName))
                        }
                    )
                }

                composable<Destination.TournamentArchive> {
                    TournamentArchiveScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToTournamentDetail = { tournamentId ->
                            navController.navigate(Destination.TournamentDetail(tournamentId))
                        }
                    )
                }

                composable<Destination.PlayerProfile> { backStackEntry ->
                    val route: Destination.PlayerProfile = backStackEntry.toRoute()
                    PlayerProfileScreen(
                        playerName = route.playerName,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToTournament = { tournamentId ->
                            navController.navigate(Destination.TournamentDetail(tournamentId))
                        },
                        onNavigateToPlayer = { opponentName ->
                            navController.navigate(Destination.PlayerProfile(opponentName))
                        }
                    )
                }

                composable<Destination.PlayersLeaderboard> {
                    PlayersLeaderboardScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPlayer = { playerName ->
                            navController.navigate(Destination.PlayerProfile(playerName))
                        }
                    )
                }

                composable<Destination.Settings> {
                    SettingsScreen()
                }
            }

            // Global Floating Update Bottom Bar
            val context = LocalContext.current
            AnimatedVisibility(
                visible = showUpdateBar,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(animationSpec = tween(durationMillis = 280)),
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = spring(
                        dampingRatio = 0.9f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeOut(animationSpec = tween(durationMillis = 200)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = if (isTopLevelDestination) 76.dp else 12.dp)
            ) {
                UpdateBottomBar(
                    uiState = updateUiState,
                    onUpdateClick = { updatesViewModel.downloadUpdate(it) },
                    onPauseClick = { updatesViewModel.pauseDownload(it) },
                    onResumeClick = { updatesViewModel.resumeDownload(it) },
                    onInstallClick = { info, file -> updatesViewModel.installUpdate(context, info, file) },
                    onDismiss = {
                        val version = (updateUiState as? UpdateUiState.UpdateAvailable)?.info?.versionName
                        if (version != null) {
                            updatesViewModel.dismissUpdate(version)
                        }
                    },
                    onNavigateToUpdates = {
                        navController.navigate(Destination.Settings) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            // Floating Bottom Bar layered directly on top with no background obstruction
            AnimatedVisibility(
                visible = isTopLevelDestination,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = spring(
                        dampingRatio = 0.9f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                )
            ) {
                RndmBottomBar(
                    navController = navController
                )
            }
        }
}
