package com.example.spacebridgeapp.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object LoginRoute

@Serializable
object DriveOnboardingRoute

@Serializable
object NotesRoute

@Serializable
data class EditorRoute(
    val noteId: String? = null,
    val initialType: String? = null,
)

@Serializable
object FilesRoute

@Serializable
object TrashRoute

@Serializable
object SettingsRoute
