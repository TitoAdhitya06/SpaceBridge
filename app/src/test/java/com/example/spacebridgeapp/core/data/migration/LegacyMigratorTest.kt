package com.example.spacebridgeapp.core.data.migration

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyMigratorTest {
    @Test
    fun `test convertLegacyDataToV3Map with text note`() {
        val waktu = Timestamp(1690000000, 500000000)
        val legacy =
            mapOf(
                "judul" to "Catatan v2 Lama",
                "isi" to "Ini isi catatan v2 yang lama",
                "tipe" to "teks",
                "pinned" to true,
                "waktu" to waktu,
            )

        val v3Map = LegacyMigrator.convertLegacyDataToV3Map(legacy)
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

        assertEquals(expectedKeys, v3Map.keys)
        assertEquals("text", v3Map["type"])
        assertEquals("Catatan v2 Lama", v3Map["title"])
        assertEquals("Ini isi catatan v2 yang lama", v3Map["body"])
        assertEquals(emptyList<Map<String, Any>>(), v3Map["items"])
        assertEquals(true, v3Map["pinned"])
        assertEquals(emptyList<String>(), v3Map["labelIds"])
        assertEquals(emptyList<String>(), v3Map["attachmentIds"])
        assertEquals("active", v3Map["status"])
        assertNull(v3Map["trashedAt"])
        assertEquals(waktu, v3Map["createdAt"])
        assertNotNull(v3Map["updatedAt"])
        assertEquals(1, v3Map["schemaVersion"])
    }

    @Test
    fun `test convertLegacyDataToV3Map with checklist note`() {
        val legacyItems =
            listOf(
                mapOf("teks" to "Beli telur", "checked" to false),
                mapOf("teks" to "Beli susu", "checked" to true),
            )
        val legacy =
            mapOf(
                "judul" to "Daftar Belanjaan",
                "isi" to "teks yang harus dikosongkan",
                "tipe" to "list",
                "pinned" to false,
                "items" to legacyItems,
            )

        val v3Map = LegacyMigrator.convertLegacyDataToV3Map(legacy)
        assertEquals("checklist", v3Map["type"])
        assertEquals("Daftar Belanjaan", v3Map["title"])
        // PRD 7.3: body wajib selalu "" untuk checklist
        assertEquals("", v3Map["body"])

        @Suppress("UNCHECKED_CAST")
        val items = v3Map["items"] as List<Map<String, Any>>
        assertEquals(2, items.size)
        assertEquals("Beli telur", items[0]["text"])
        assertEquals(false, items[0]["checked"])
        assertTrue((items[0]["id"] as String).isNotBlank())

        assertEquals("Beli susu", items[1]["text"])
        assertEquals(true, items[1]["checked"])
        assertTrue((items[1]["id"] as String).isNotBlank())
    }

    @Test
    fun `test convertLegacyDataToV3Map truncates limits`() {
        val longJudul = "J".repeat(300)
        val longIsi = "I".repeat(30000)
        val longItemText = "T".repeat(1500)
        val manyItems = (1..600).map { mapOf("teks" to "$longItemText-$it", "checked" to false) }

        val legacy =
            mapOf(
                "judul" to longJudul,
                "isi" to longIsi,
                "tipe" to "checklist",
                "pinned" to false,
                "items" to manyItems,
            )

        val v3Map = LegacyMigrator.convertLegacyDataToV3Map(legacy)
        assertEquals(200, (v3Map["title"] as String).length)
        assertEquals("", v3Map["body"])

        @Suppress("UNCHECKED_CAST")
        val items = v3Map["items"] as List<Map<String, Any>>
        assertEquals(500, items.size)
        assertEquals(1000, (items[0]["text"] as String).length)
    }
}
