package com.example.spacebridgeapp.core.model

import java.time.Instant

enum class NoteType {
    TEXT,
    CHECKLIST,
}

enum class NoteStatus {
    ACTIVE,
    ARCHIVED,
    TRASHED,
}

data class ChecklistItem(
    val id: String,
    val text: String,
    val checked: Boolean = false,
)

data class Note(
    val id: String,
    val type: NoteType,
    val title: String = "",
    val body: String = "",
    val items: List<ChecklistItem> = emptyList(),
    val pinned: Boolean = false,
    val labelIds: List<String> = emptyList(),
    val attachmentIds: List<String> = emptyList(),
    val status: NoteStatus = NoteStatus.ACTIVE,
    val trashedAt: Instant? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val hasPendingWrites: Boolean = false,
)

data class Label(
    val id: String,
    val name: String,
)
