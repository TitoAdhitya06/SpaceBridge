package com.example.spacebridgeapp.ui.navigation

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.spacebridgeapp.appContainer
import com.example.spacebridgeapp.feature.auth.AuthViewModel
import com.example.spacebridgeapp.feature.auth.DriveOnboardingScreen
import com.example.spacebridgeapp.feature.auth.LoginScreen
import com.example.spacebridgeapp.ui.components.BottomBarTab
import com.example.spacebridgeapp.ui.components.FloatingBottomBar
import com.example.spacebridgeapp.ui.theme.SpaceBridgeTheme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.CompositionLocalProvider

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpaceBridgeNavHost(
    navController: NavHostController = rememberNavController(),
    context: Context = LocalContext.current,
    startDestination: Any =
        if (context.appContainer.auth.currentUser != null) {
            NotesRoute
        } else {
            LoginRoute
        },
    onFabClick: () -> Unit = { navController.navigate(EditorRoute(noteId = null)) },
    modifier: Modifier = Modifier,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isNotes = currentDestination?.hasRoute<NotesRoute>() == true
    val isFiles = currentDestination?.hasRoute<FilesRoute>() == true
    val showBottomBar = isNotes || isFiles

    val currentTab =
        when {
            isFiles -> BottomBarTab.FILES
            else -> BottomBarTab.NOTES
        }

    val container = context.appContainer

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SpaceBridgeTheme.colors.background,
        bottomBar = {
            if (showBottomBar) {
                FloatingBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        val targetRoute =
                            when (tab) {
                                BottomBarTab.NOTES -> NotesRoute
                                BottomBarTab.FILES -> FilesRoute
                            }
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onFabClick = onFabClick,
                )
            }
        },
    ) { innerPadding ->
        @Suppress("DEPRECATION")
        CompositionLocalProvider(
            LocalOverscrollFactory provides null,
            LocalOverscrollConfiguration provides null,
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
            ) {
            composable<LoginRoute> {
                val authViewModel: AuthViewModel =
                    viewModel(
                        factory =
                            AuthViewModel.provideFactory(
                                authRepository = container.authRepository,
                                preferencesRepository = container.preferencesRepository,
                                driveAuthManager = container.driveAuthManager,
                            ),
                    )
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToHome = {
                        navController.navigate(NotesRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    },
                    onNavigateToDriveOnboarding = {
                        navController.navigate(DriveOnboardingRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    },
                )
            }

            composable<DriveOnboardingRoute> {
                val authViewModel: AuthViewModel =
                    viewModel(
                        factory =
                            AuthViewModel.provideFactory(
                                authRepository = container.authRepository,
                                preferencesRepository = container.preferencesRepository,
                                driveAuthManager = container.driveAuthManager,
                            ),
                    )
                DriveOnboardingScreen(
                    viewModel = authViewModel,
                    onNavigateToHome = {
                        navController.navigate(NotesRoute) {
                            popUpTo(DriveOnboardingRoute) { inclusive = true }
                        }
                    },
                )
            }

            composable<NotesRoute> { backStackEntry ->
                val notesViewModel: com.example.spacebridgeapp.feature.notes.NotesViewModel =
                    viewModel(
                        factory =
                            com.example.spacebridgeapp.feature.notes.NotesViewModel.provideFactory(
                                notesRepository = container.notesRepository,
                                networkObserver = container.networkObserver,
                                authRepository = container.authRepository,
                                legacyMigrator = container.legacyMigrator,
                            ),
                    )

                val trashedNoteId = backStackEntry.savedStateHandle.get<String>("trashed_note_id")
                androidx.compose.runtime.LaunchedEffect(trashedNoteId) {
                    if (trashedNoteId != null) {
                        notesViewModel.onNoteTrashedNotice(trashedNoteId)
                        backStackEntry.savedStateHandle.remove<String>("trashed_note_id")
                    }
                }

                com.example.spacebridgeapp.feature.notes.NotesScreen(
                    viewModel = notesViewModel,
                    onNoteClick = { noteId ->
                        navController.navigate(EditorRoute(noteId = noteId))
                    },
                    onNavigateToSettings = {
                        navController.navigate(SettingsRoute)
                    },
                    onNavigateToTrash = {
                        navController.navigate(TrashRoute)
                    },
                )
            }

            composable<FilesRoute> {
                val filesViewModel: com.example.spacebridgeapp.feature.files.FilesViewModel =
                    viewModel(
                        factory =
                            com.example.spacebridgeapp.feature.files.FilesViewModel.provideFactory(
                                filesRepository = container.filesRepository,
                                driveAuthManager = container.driveAuthManager,
                                authRepository = container.authRepository,
                                networkObserver = container.networkObserver,
                            ),
                    )
                com.example.spacebridgeapp.feature.files.FilesScreen(
                    viewModel = filesViewModel,
                    onNavigateToSettings = {
                        navController.navigate(SettingsRoute)
                    },
                    onNavigateToTrash = {
                        navController.navigate(TrashRoute)
                    },
                )
            }

            composable<EditorRoute> { backStackEntry ->
                val editorRoute = backStackEntry.toRoute<EditorRoute>()
                val editorViewModel: com.example.spacebridgeapp.feature.editor.EditorViewModel =
                    viewModel(
                        key = "editor_${editorRoute.noteId ?: "new"}",
                        factory =
                            com.example.spacebridgeapp.feature.editor.EditorViewModel.provideFactory(
                                notesRepository = container.notesRepository,
                                networkObserver = container.networkObserver,
                                noteId = editorRoute.noteId,
                                initialType = editorRoute.initialType,
                            ),
                    )
                com.example.spacebridgeapp.feature.editor.EditorScreen(
                    viewModel = editorViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNoteTrashed = { trashedId ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("trashed_note_id", trashedId)
                        navController.popBackStack()
                    },
                )
            }

            composable<TrashRoute> {
                val trashViewModel: com.example.spacebridgeapp.feature.trash.TrashViewModel =
                    viewModel(
                        factory =
                            com.example.spacebridgeapp.feature.trash.TrashViewModel.provideFactory(
                                notesRepository = container.notesRepository,
                                filesRepository = container.filesRepository,
                                authRepository = container.authRepository,
                            ),
                    )
                com.example.spacebridgeapp.feature.trash.TrashScreen(
                    viewModel = trashViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                )
            }

            composable<SettingsRoute> {
                val settingsViewModel: com.example.spacebridgeapp.feature.settings.SettingsViewModel =
                    viewModel(
                        factory =
                            com.example.spacebridgeapp.feature.settings.SettingsViewModel.provideFactory(
                                authRepository = container.authRepository,
                                preferencesRepository = container.preferencesRepository,
                                fileCacheManager = container.fileCacheManager,
                                networkObserver = container.networkObserver,
                                driveAuthManager = container.driveAuthManager,
                            ),
                    )
                com.example.spacebridgeapp.feature.settings.SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onSignedOut = {
                        navController.navigate(LoginRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}
}

