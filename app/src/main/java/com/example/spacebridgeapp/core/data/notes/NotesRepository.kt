package com.example.spacebridgeapp.core.data.notes

import com.example.spacebridgeapp.core.model.ChecklistItem
import com.example.spacebridgeapp.core.model.Note
import com.example.spacebridgeapp.core.model.NoteStatus
import com.example.spacebridgeapp.core.model.NoteType
import kotlinx.coroutines.flow.Flow
import java.time.Instant

data class NoteChanges(
    val type: NoteType? = null,
    val title: String? = null,
    val body: String? = null,
    val items: List<ChecklistItem>? = null,
    val pinned: Boolean? = null,
    val labelIds: List<String>? = null,
    val attachmentIds: List<String>? = null,
    val status: NoteStatus? = null,
    val trashedAt: Instant? = null,
)

interface NotesRepository {
    fun observeNotes(status: NoteStatus): Flow<List<Note>>

    fun observeNote(id: String): Flow<Note?>

    fun newId(): String

    fun create(note: Note)

    fun update(
        id: String,
        changes: NoteChanges,
    )

    fun setPinned(
        ids: List<String>,
        pinned: Boolean,
    )

    fun setStatus(
        ids: List<String>,
        status: NoteStatus,
    )

    fun deletePermanently(ids: List<String>)

    fun addLabel(
        ids: List<String>,
        labelId: String,
    )

    suspend fun purgeTrashOlderThan(cutoff: Instant)

    suspend fun removeAttachmentEverywhere(fileId: String)

    suspend fun removeLabelEverywhere(labelId: String)
}
