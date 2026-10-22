package com.example.spacebridgeapp.core.data.notes

import com.example.spacebridgeapp.core.model.ChecklistItem
import com.example.spacebridgeapp.core.model.Note
import com.example.spacebridgeapp.core.model.NoteStatus
import com.example.spacebridgeapp.core.model.NoteType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant

@IgnoreExtraProperties
data class ChecklistItemDto(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("text") @set:PropertyName("text") var text: String = "",
    @get:PropertyName("checked") @set:PropertyName("checked") var checked: Boolean = false,
) {
    fun toDomain(): ChecklistItem =
        ChecklistItem(
            id = id,
            text = text,
            checked = checked,
        )

    companion object {
        fun fromDomain(item: ChecklistItem): ChecklistItemDto =
            ChecklistItemDto(
                id = item.id,
                text = item.text,
                checked = item.checked,
            )
    }
}

@IgnoreExtraProperties
data class NoteDto(
    @get:PropertyName("type") @set:PropertyName("type") var type: String = "text",
    @get:PropertyName("title") @set:PropertyName("title") var title: String = "",
    @get:PropertyName("body") @set:PropertyName("body") var body: String = "",
    @get:PropertyName("items") @set:PropertyName("items") var items: List<ChecklistItemDto> = emptyList(),
    @get:PropertyName("pinned") @set:PropertyName("pinned") var pinned: Boolean = false,
    @get:PropertyName("labelIds") @set:PropertyName("labelIds") var labelIds: List<String> = emptyList(),
    @get:PropertyName("attachmentIds") @set:PropertyName("attachmentIds") var attachmentIds: List<String> = emptyList(),
    @get:PropertyName("status") @set:PropertyName("status") var status: String = "active",
    @get:PropertyName("trashedAt") @set:PropertyName("trashedAt") var trashedAt: Timestamp? = null,
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") @ServerTimestamp var createdAt: Timestamp? = null,
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt") @ServerTimestamp var updatedAt: Timestamp? = null,
    @get:PropertyName("schemaVersion") @set:PropertyName("schemaVersion") var schemaVersion: Int = 1,
) {
    fun toDomain(
        id: String,
        hasPendingWrites: Boolean = false,
    ): Note {
        val domainType =
            when (type.lowercase()) {
                "checklist" -> NoteType.CHECKLIST
                else -> NoteType.TEXT
            }
        val domainStatus =
            when (status.lowercase()) {
                "archived" -> NoteStatus.ARCHIVED
                "trashed" -> NoteStatus.TRASHED
                else -> NoteStatus.ACTIVE
            }
        return Note(
            id = id,
            type = domainType,
            title = title,
            body = body,
            items = items.map { it.toDomain() },
            pinned = pinned,
            labelIds = labelIds,
            attachmentIds = attachmentIds,
            status = domainStatus,
            trashedAt = trashedAt?.toInstant(),
            createdAt = createdAt?.toInstant(),
            updatedAt = updatedAt?.toInstant(),
            hasPendingWrites = hasPendingWrites,
        )
    }

    companion object {
        fun fromDomain(note: Note): NoteDto =
            NoteDto(
                type =
                    when (note.type) {
                        NoteType.CHECKLIST -> "checklist"
                        NoteType.TEXT -> "text"
                    },
                title = note.title,
                body = if (note.type == NoteType.CHECKLIST) "" else note.body,
                items = if (note.type == NoteType.TEXT) emptyList() else note.items.map { ChecklistItemDto.fromDomain(it) },
                pinned = note.pinned,
                labelIds = note.labelIds,
                attachmentIds = note.attachmentIds,
                status =
                    when (note.status) {
                        NoteStatus.ACTIVE -> "active"
                        NoteStatus.ARCHIVED -> "archived"
                        NoteStatus.TRASHED -> "trashed"
                    },
                trashedAt = note.trashedAt?.toTimestamp(),
                createdAt = note.createdAt?.toTimestamp(),
                updatedAt = note.updatedAt?.toTimestamp(),
                schemaVersion = 1,
            )
    }
}

fun Note.toFirestoreMap(): Map<String, Any?> =
    mapOf(
        "type" to
            when (type) {
                NoteType.CHECKLIST -> "checklist"
                NoteType.TEXT -> "text"
            },
        "title" to title.take(200),
        "body" to if (type == NoteType.CHECKLIST) "" else body.take(20000),
        "items" to
            if (type == NoteType.TEXT) {
                emptyList<Map<String, Any>>()
            } else {
                items.take(500).map { item ->
                    mapOf(
                        "id" to item.id,
                        "text" to item.text.take(1000),
                        "checked" to item.checked,
                    )
                }
            },
        "pinned" to pinned,
        "labelIds" to labelIds.take(10),
        "attachmentIds" to attachmentIds.take(20),
        "status" to
            when (status) {
                NoteStatus.ACTIVE -> "active"
                NoteStatus.ARCHIVED -> "archived"
                NoteStatus.TRASHED -> "trashed"
            },
        "trashedAt" to trashedAt?.toTimestamp(),
        "createdAt" to (createdAt?.toTimestamp() ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp(),
        "schemaVersion" to 1,
    )

fun DocumentSnapshot.toNote(): Note? {
    if (!exists()) return null
    return try {
        val dto = toObject(NoteDto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        dto?.toDomain(id = id, hasPendingWrites = metadata.hasPendingWrites()) ?: toNoteFallback(this)
    } catch (_: Exception) {
        toNoteFallback(this)
    }
}

private fun toNoteFallback(snapshot: DocumentSnapshot): Note {
    val typeStr = snapshot.getString("type") ?: "text"
    val type =
        if (typeStr.equals("checklist", ignoreCase = true)) {
            NoteType.CHECKLIST
        } else {
            NoteType.TEXT
        }
    val title = snapshot.getString("title") ?: ""
    val body = snapshot.getString("body") ?: ""
    val rawItems = snapshot.get("items") as? List<*> ?: emptyList<Any>()
    val items =
        rawItems.mapNotNull { raw ->
            when (raw) {
                is Map<*, *> -> {
                    val itemId = raw["id"] as? String ?: ""
                    val itemText = raw["text"] as? String ?: ""
                    val itemChecked = raw["checked"] as? Boolean ?: false
                    ChecklistItem(id = itemId, text = itemText, checked = itemChecked)
                }
                else -> null
            }
        }
    val pinned = snapshot.getBoolean("pinned") ?: false
    val labelIds = (snapshot.get("labelIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    val attachmentIds = (snapshot.get("attachmentIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    val statusStr = snapshot.getString("status") ?: "active"
    val status =
        when (statusStr.lowercase()) {
            "archived" -> NoteStatus.ARCHIVED
            "trashed" -> NoteStatus.TRASHED
            else -> NoteStatus.ACTIVE
        }
    val trashedAt = snapshot.getTimestamp("trashedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toInstant()
    val createdAt = snapshot.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toInstant()
    val updatedAt = snapshot.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toInstant()

    return Note(
        id = snapshot.id,
        type = type,
        title = title,
        body = body,
        items = items,
        pinned = pinned,
        labelIds = labelIds,
        attachmentIds = attachmentIds,
        status = status,
        trashedAt = trashedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        hasPendingWrites = snapshot.metadata.hasPendingWrites(),
    )
}

fun Instant.toTimestamp(): Timestamp = Timestamp(epochSecond, nano)
