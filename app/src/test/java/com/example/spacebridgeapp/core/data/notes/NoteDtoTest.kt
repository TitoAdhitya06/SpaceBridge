package com.example.spacebridgeapp.core.data.notes

import com.example.spacebridgeapp.core.model.ChecklistItem
import com.example.spacebridgeapp.core.model.Note
import com.example.spacebridgeapp.core.model.NoteStatus
import com.example.spacebridgeapp.core.model.NoteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class NoteDtoTest {
    @Test
    fun `test Note text type to NoteDto and back to Note`() {
        val now = Instant.ofEpochSecond(1700000000, 123000000)
        val note =
            Note(
                id = "note-123",
                type = NoteType.TEXT,
                title = "Judul Catatan",
                body = "Isi teks catatan penting",
                items = emptyList(),
                pinned = true,
                labelIds = listOf("label-1", "label-2"),
                attachmentIds = listOf("file-1"),
                status = NoteStatus.ACTIVE,
                trashedAt = null,
                createdAt = now,
                updatedAt = now,
                hasPendingWrites = false,
            )

        val dto = NoteDto.fromDomain(note)
        assertEquals("text", dto.type)
        assertEquals("Judul Catatan", dto.title)
        assertEquals("Isi teks catatan penting", dto.body)
        assertTrue(dto.items.isEmpty())
        assertTrue(dto.pinned)
        assertEquals(listOf("label-1", "label-2"), dto.labelIds)
        assertEquals(listOf("file-1"), dto.attachmentIds)
        assertEquals("active", dto.status)
        assertNull(dto.trashedAt)
        assertEquals(1700000000L, dto.createdAt?.seconds)
        assertEquals(1, dto.schemaVersion)

        val restoredNote = dto.toDomain(id = "note-123", hasPendingWrites = true)
        assertEquals("note-123", restoredNote.id)
        assertEquals(NoteType.TEXT, restoredNote.type)
        assertEquals("Judul Catatan", restoredNote.title)
        assertEquals("Isi teks catatan penting", restoredNote.body)
        assertTrue(restoredNote.items.isEmpty())
        assertTrue(restoredNote.pinned)
        assertEquals(listOf("label-1", "label-2"), restoredNote.labelIds)
        assertEquals(listOf("file-1"), restoredNote.attachmentIds)
        assertEquals(NoteStatus.ACTIVE, restoredNote.status)
        assertNull(restoredNote.trashedAt)
        assertEquals(now, restoredNote.createdAt)
        assertEquals(now, restoredNote.updatedAt)
        assertTrue(restoredNote.hasPendingWrites)
    }

    @Test
    fun `test Note checklist type to NoteDto and back to Note`() {
        val items =
            listOf(
                ChecklistItem(id = "item-1", text = "Beli kopi", checked = false),
                ChecklistItem(id = "item-2", text = "Beli susu", checked = true),
            )
        val note =
            Note(
                id = "check-1",
                type = NoteType.CHECKLIST,
                title = "Daftar Belanja",
                body = "",
                items = items,
                pinned = false,
                labelIds = emptyList(),
                attachmentIds = emptyList(),
                status = NoteStatus.ARCHIVED,
                trashedAt = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        val dto = NoteDto.fromDomain(note)
        assertEquals("checklist", dto.type)
        assertEquals("", dto.body)
        assertEquals(2, dto.items.size)
        assertEquals("item-1", dto.items[0].id)
        assertEquals("Beli kopi", dto.items[0].text)
        assertEquals(false, dto.items[0].checked)
        assertEquals("item-2", dto.items[1].id)
        assertEquals(true, dto.items[1].checked)

        val restored = dto.toDomain(id = "check-1")
        assertEquals(NoteType.CHECKLIST, restored.type)
        assertEquals(NoteStatus.ARCHIVED, restored.status)
        assertEquals(2, restored.items.size)
        assertEquals("Beli kopi", restored.items[0].text)
        assertEquals(false, restored.items[0].checked)
        assertEquals("Beli susu", restored.items[1].text)
        assertEquals(true, restored.items[1].checked)
    }

    @Test
    fun `test toFirestoreMap contains exact 12 keys required by Firestore rules`() {
        val note =
            Note(
                id = "note-map-1",
                type = NoteType.TEXT,
                title = "Uji Peta",
                body = "Isi peta",
                items = emptyList(),
                pinned = false,
                labelIds = emptyList(),
                attachmentIds = emptyList(),
                status = NoteStatus.ACTIVE,
                trashedAt = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        val map = note.toFirestoreMap()
        val expectedKeys =
            setOf(
                "type",
                "title",
                "body",
                "items",
                "pinned",
                "labelIds",
                "attachmentIds",
                "status",
                "trashedAt",
                "createdAt",
                "updatedAt",
                "schemaVersion",
            )

        assertEquals(expectedKeys, map.keys)
        assertEquals("text", map["type"])
        assertEquals("Uji Peta", map["title"])
        assertEquals("Isi peta", map["body"])
        assertEquals(emptyList<Map<String, Any>>(), map["items"])
        assertEquals(false, map["pinned"])
        assertEquals(emptyList<String>(), map["labelIds"])
        assertEquals(emptyList<String>(), map["attachmentIds"])
        assertEquals("active", map["status"])
        assertNull(map["trashedAt"])
        assertNotNull(map["createdAt"])
        assertNotNull(map["updatedAt"])
        assertEquals(1, map["schemaVersion"])
    }

    @Test
    fun `test toFirestoreMap truncates fields exceeding schema limits`() {
        val longTitle = "A".repeat(300)
        val longBody = "B".repeat(25000)
        val manyLabels = (1..15).map { "label-$it" }
        val manyAttachments = (1..25).map { "file-$it" }
        val manyItems = (1..600).map { ChecklistItem("id-$it", "text-$it", false) }

        val note =
            Note(
                id = "limit-test",
                type = NoteType.CHECKLIST,
                title = longTitle,
                body = longBody,
                items = manyItems,
                pinned = false,
                labelIds = manyLabels,
                attachmentIds = manyAttachments,
                status = NoteStatus.TRASHED,
                trashedAt = Instant.now(),
            )

        val map = note.toFirestoreMap()
        assertEquals(200, (map["title"] as String).length)
        // Checklist body is always empty
        assertEquals("", map["body"])
        assertEquals(500, (map["items"] as List<*>).size)
        assertEquals(10, (map["labelIds"] as List<*>).size)
        assertEquals(20, (map["attachmentIds"] as List<*>).size)
        assertEquals("trashed", map["status"])
        assertNotNull(map["trashedAt"])
    }
}
