package com.example.spacebridgeapp.core.model

data class AppUser(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
)
