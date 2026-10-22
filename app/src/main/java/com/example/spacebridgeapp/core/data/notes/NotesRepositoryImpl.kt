package com.example.spacebridgeapp.core.data.notes

import com.example.spacebridgeapp.core.model.Note
import com.example.spacebridgeapp.core.model.NoteStatus
import com.example.spacebridgeapp.core.model.NoteType
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class NotesRepositoryImpl(
    private val auth: FirebaseAuth = Firebase.auth,
    private val firestoreProvider: () -> FirebaseFirestore = { Firebase.firestore },
) : NotesRepository {
    constructor(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
    ) : this(
        auth = auth,
        firestoreProvider = { firestore },
    )

    private val currentUid: String?
        get() = auth.currentUser?.uid

    private fun currentUidFlow(): Flow<String?> =
        callbackFlow {
            val listener =
                FirebaseAuth.AuthStateListener { fbAuth ->
                    trySend(fbAuth.currentUser?.uid)
                }
            auth.addAuthStateListener(listener)
            trySend(auth.currentUser?.uid)
            awaitClose {
                auth.removeAuthStateListener(listener)
            }
        }.distinctUntilChanged()

    override fun observeNotes(status: NoteStatus): Flow<List<Note>> =
        currentUidFlow().flatMapLatest { uid ->
            if (uid == null) {
                flowOf(emptyList())
            } else {
                callbackFlow {
                    val statusStr =
                        when (status) {
                            NoteStatus.ACTIVE -> "active"
                            NoteStatus.ARCHIVED -> "archived"
                            NoteStatus.TRASHED -> "trashed"
                        }

                    val query =
                        firestoreProvider()
                            .collection("users")
                            .document(uid)
                            .collection("notes")
                            .whereEqualTo("status", statusStr)
                            .orderBy("updatedAt", Query.Direction.DESCENDING)

                    val registration =
                        query.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                            if (error != null) {
                                android.util.Log.e("NotesRepository", "Firestore snapshot error: ${error.message}", error)
                                trySend(emptyList())
                                return@addSnapshotListener
                            }
                            if (snapshot != null) {
                                val notes = snapshot.documents.mapNotNull { it.toNote() }
                                trySend(notes)
                            }
                        }

                    awaitClose {
                        registration.remove()
                    }
                }
            }
        }

    override fun observeNote(id: String): Flow<Note?> =
        currentUidFlow().flatMapLatest { uid ->
            if (uid == null) {
                flowOf(null)
            } else {
                callbackFlow {
                    val docRef =
                        firestoreProvider()
                            .collection("users")
                            .document(uid)
                            .collection("notes")
                            .document(id)

                    val registration =
                        docRef.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                            if (error != null) {
                                android.util.Log.e("NotesRepository", "Firestore doc snapshot error: ${error.message}", error)
                                trySend(null)
                                return@addSnapshotListener
                            }
                            if (snapshot != null && snapshot.exists()) {
                                trySend(snapshot.toNote())
                            } else {
                                trySend(null)
                            }
                        }

                    awaitClose {
                        registration.remove()
                    }
                }
            }
        }

    override fun newId(): String {
        val uid = currentUid ?: "default"
        return firestoreProvider()
            .collection("users")
            .document(uid)
            .collection("notes")
            .document()
            .id
    }

    override fun create(note: Note) {
        val uid = currentUid ?: return
        val docRef =
            firestoreProvider()
                .collection("users")
                .document(uid)
                .collection("notes")
                .document(note.id)

        val noteMap = note.toFirestoreMap()
        docRef.set(noteMap)
    }

    override fun update(
        id: String,
        changes: NoteChanges,
    ) {
        val uid = currentUid ?: return
        val docRef =
            firestoreProvider()
                .collection("users")
                .document(uid)
                .collection("notes")
                .document(id)

        val updates = mutableMapOf<String, Any?>()

        changes.type?.let { type ->
            updates["type"] =
                when (type) {
                    NoteType.TEXT -> "text"
                    NoteType.CHECKLIST -> "checklist"
                }
        }
        changes.title?.let { updates["title"] = it.take(200) }
        changes.body?.let { updates["body"] = it.take(20000) }
        changes.items?.let { items ->
            updates["items"] =
                items.take(500).map { item ->
                    mapOf(
                        "id" to item.id,
                        "text" to item.text.take(1000),
                        "checked" to item.checked,
                    )
                }
        }
        changes.pinned?.let { updates["pinned"] = it }
        changes.labelIds?.let { updates["labelIds"] = it.take(10) }
        changes.attachmentIds?.let { updates["attachmentIds"] = it.take(20) }
        changes.status?.let { status ->
            updates["status"] =
                when (status) {
                    NoteStatus.ACTIVE -> "active"
                    NoteStatus.ARCHIVED -> "archived"
                    NoteStatus.TRASHED -> "trashed"
                }
            if (status != NoteStatus.TRASHED && changes.trashedAt == null) {
                updates["trashedAt"] = null
            }
        }
        if (changes.trashedAt != null) {
            updates["trashedAt"] = Timestamp(changes.trashedAt.epochSecond, changes.trashedAt.nano)
        }

        updates["updatedAt"] = FieldValue.serverTimestamp()

        docRef.update(updates)
    }

    override fun setPinned(
        ids: List<String>,
        pinned: Boolean,
    ) {
        val uid = currentUid ?: return
        if (ids.isEmpty()) return
        val notesCol =
            firestoreProvider()
                .collection("users")
                .document(uid)
                .collection("notes")
        val batch = firestoreProvider().batch()
        ids.forEach { id ->
            val docRef = notesCol.document(id)
            batch.update(
                docRef,
                mapOf(
                    "pinned" to pinned,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
        }
        batch.commit()
    }

    override fun setStatus(
        ids: List<String>,
        status: NoteStatus,
    ) {
        val uid = currentUid ?: return
        if (ids.isEmpty()) return
        val notesCol =
            firestoreProvider()
                .collection("users")
                .document(uid)
                .collection("notes")
        val batch = firestoreProvider().batch()
        val statusStr =
            when (status) {
                NoteStatus.ACTIVE -> "active"
                NoteStatus.ARCHIVED -> "archived"
                NoteStatus.TRASHED -> "trashed"
            }
        val trashedAtVal = if (status == NoteStatus.TRASHED) FieldValue.serverTimestamp() else null

        ids.forEach { id ->
            val docRef = notesCol.document(id)
            batch.update(
                docRef,
                mapOf(
                    "status" to statusStr,
                    "trashedAt" to trashedAtVal,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
        }
        batch.commit()
    }

    override fun deletePermanently(ids: List<String>) {
        val uid = currentUid ?: return
        if (ids.isEmpty()) return
        val notesCol =
            firestoreProvider()
                .collection("users")
                .document(uid)
                .collection("notes")
        val batch = firestoreProvider().batch()
        ids.forEach { id ->
            batch.delete(notesCol.document(id))
        }
        batch.commit()
    }

    override fun addLabel(
        ids: List<String>,
        labelId: String,
    ) {
        val uid = currentUid ?: return
        if (ids.isEmpty()) return
        val notesCol =
            firestoreProvider()
                .collection("users")
                .document(uid)
                .collection("notes")
        val batch = firestoreProvider().batch()
        ids.forEach { id ->
            val docRef = notesCol.document(id)
            batch.update(
                docRef,
                mapOf(
                    "labelIds" to FieldValue.arrayUnion(labelId),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
        }
        batch.commit()
    }

    override suspend fun purgeTrashOlderThan(cutoff: Instant) =
        withContext(Dispatchers.IO) {
            val uid = currentUid ?: return@withContext
            val cutoffTs = Timestamp(cutoff.epochSecond, cutoff.nano)
            val notesCol =
                firestoreProvider()
                    .collection("users")
                    .document(uid)
                    .collection("notes")
            try {
                val snapshot =
                    notesCol
                        .whereEqualTo("status", "trashed")
                        .whereLessThanOrEqualTo("trashedAt", cutoffTs)
                        .get()
                        .await()

                if (!snapshot.isEmpty) {
                    val batch = firestoreProvider().batch()
                    for (doc in snapshot.documents) {
                        batch.delete(doc.reference)
                    }
                    batch.commit().await()
                }
            } catch (_: Exception) {
            }
        }

    override suspend fun removeAttachmentEverywhere(fileId: String) =
        withContext(Dispatchers.IO) {
            val uid = currentUid ?: return@withContext
            val notesCol =
                firestoreProvider()
                    .collection("users")
                    .document(uid)
                    .collection("notes")
            try {
                val snapshot =
                    notesCol
                        .whereArrayContains("attachmentIds", fileId)
                        .get()
                        .await()

                if (!snapshot.isEmpty) {
                    val batch = firestoreProvider().batch()
                    for (doc in snapshot.documents) {
                        batch.update(
                            doc.reference,
                            mapOf(
                                "attachmentIds" to FieldValue.arrayRemove(fileId),
                                "updatedAt" to FieldValue.serverTimestamp(),
                            ),
                        )
                    }
                    batch.commit().await()
                }
            } catch (_: Exception) {
            }
        }

    override suspend fun removeLabelEverywhere(labelId: String) =
        withContext(Dispatchers.IO) {
            val uid = currentUid ?: return@withContext
            val notesCol =
                firestoreProvider()
                    .collection("users")
                    .document(uid)
                    .collection("notes")
            try {
                val snapshot =
                    notesCol
                        .whereArrayContains("labelIds", labelId)
                        .get()
                        .await()

                if (!snapshot.isEmpty) {
                    val batch = firestoreProvider().batch()
                    for (doc in snapshot.documents) {
                        batch.update(
                            doc.reference,
                            mapOf(
                                "labelIds" to FieldValue.arrayRemove(labelId),
                                "updatedAt" to FieldValue.serverTimestamp(),
                            ),
                        )
                    }
                    batch.commit().await()
                }
            } catch (_: Exception) {
            }
        }
}
