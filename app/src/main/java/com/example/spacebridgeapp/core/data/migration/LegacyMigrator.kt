package com.example.spacebridgeapp.core.data.migration

import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

data class MigrationReport(
    val notesMigrated: Int = 0,
    val alreadyMigrated: Boolean = false,
)

class LegacyMigrator(
    private val firestoreProvider: () -> FirebaseFirestore = { Firebase.firestore },
) {
    suspend fun isMigrationNeeded(uid: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val firestore = firestoreProvider()
                val userDoc =
                    firestore
                        .collection("users")
                        .document(uid)
                        .get()
                        .await()
                if (!userDoc.exists()) return@withContext true
                userDoc.getTimestamp("migratedV3At") == null
            } catch (_: Exception) {
                false
            }
        }

    suspend fun migrateIfNeeded(uid: String): Result<MigrationReport> =
        withContext(Dispatchers.IO) {
            try {
                val firestore = firestoreProvider()
                val userDocRef = firestore.collection("users").document(uid)
                val userSnapshot = userDocRef.get().await()

                if (userSnapshot.exists() && userSnapshot.getTimestamp("migratedV3At") != null) {
                    return@withContext Result.success(MigrationReport(notesMigrated = 0, alreadyMigrated = true))
                }

                val legacyCol = firestore.collection("users").document(uid).collection("catatan")
                val legacySnapshot = legacyCol.get().await()

                if (legacySnapshot.isEmpty) {
                    markMigrationCompleted(userDocRef, userSnapshot.exists())
                    return@withContext Result.success(MigrationReport(notesMigrated = 0, alreadyMigrated = false))
                }

                val notesCol = firestore.collection("users").document(uid).collection("notes")
                val documents = legacySnapshot.documents
                var totalMigrated = 0

                val chunkSize = 200
                val chunks = documents.chunked(chunkSize)

                for (chunk in chunks) {
                    val batch = firestore.batch()
                    for (legacyDoc in chunk) {
                        val v3Map = convertLegacyDocToV3Map(legacyDoc)
                        val targetDocRef = notesCol.document(legacyDoc.id)
                        batch.set(targetDocRef, v3Map)
                        batch.delete(legacyDoc.reference)
                    }
                    batch.commit().await()
                    totalMigrated += chunk.size
                }

                markMigrationCompleted(userDocRef, userSnapshot.exists())
                Result.success(MigrationReport(notesMigrated = totalMigrated, alreadyMigrated = false))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private suspend fun markMigrationCompleted(
        userDocRef: DocumentReference,
        exists: Boolean,
    ) {
        if (exists) {
            userDocRef.update("migratedV3At", FieldValue.serverTimestamp()).await()
        } else {
            userDocRef.set(mapOf("migratedV3At" to FieldValue.serverTimestamp()), SetOptions.merge()).await()
        }
    }

    companion object {
        fun convertLegacyDocToV3Map(legacyDoc: DocumentSnapshot): Map<String, Any?> {
            val data = legacyDoc.data ?: emptyMap<String, Any?>()
            return convertLegacyDataToV3Map(data)
        }

        fun convertLegacyDataToV3Map(legacyData: Map<String, Any?>): Map<String, Any?> {
            val judul = legacyData["judul"] as? String ?: ""
            val isi = legacyData["isi"] as? String ?: ""
            val tipe = legacyData["tipe"] as? String ?: "teks"
            val pinned = legacyData["pinned"] as? Boolean ?: false
            val waktu = legacyData["waktu"] as? Timestamp
            val isChecklist = tipe.equals("list", ignoreCase = true) || tipe.equals("checklist", ignoreCase = true)

            val v3Type = if (isChecklist) "checklist" else "text"
            val v3Body = if (isChecklist) "" else isi.take(20000)

            val rawItems = legacyData["items"] as? List<*> ?: emptyList<Any?>()
            val v3Items =
                if (isChecklist) {
                    rawItems.take(500).mapIndexed { index, raw ->
                        val itemMap = raw as? Map<*, *>
                        val text =
                            (
                                itemMap?.get("teks") as? String
                                    ?: itemMap?.get("text") as? String
                                    ?: ""
                            ).take(1000)
                        val checked = itemMap?.get("checked") as? Boolean ?: false
                        val id =
                            (itemMap?.get("id") as? String)?.takeIf { it.isNotBlank() }
                                ?: "item-${index + 1}-${UUID.randomUUID().toString().take(6)}"

                        mapOf(
                            "id" to id,
                            "text" to text,
                            "checked" to checked,
                        )
                    }
                } else {
                    emptyList<Map<String, Any>>()
                }

            return mapOf(
                "type" to v3Type,
                "title" to judul.take(200),
                "body" to v3Body,
                "items" to v3Items,
                "pinned" to pinned,
                "labelIds" to emptyList<String>(),
                "attachmentIds" to emptyList<String>(),
                "status" to "active",
                "trashedAt" to null,
                "createdAt" to (waktu ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp(),
                "schemaVersion" to 1,
            )
        }
    }
}
