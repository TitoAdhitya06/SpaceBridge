package com.example.spacebridgeapp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.spacebridgeapp.core.data.auth.AuthRepository
import com.example.spacebridgeapp.core.data.files.FilesRepository
import com.example.spacebridgeapp.core.data.files.FilesRepositoryImpl
import com.example.spacebridgeapp.core.data.network.NetworkConnectivityObserver
import com.example.spacebridgeapp.core.data.network.NetworkObserver
import com.example.spacebridgeapp.core.data.notes.NotesRepository
import com.example.spacebridgeapp.core.data.prefs.DefaultPreferencesRepository
import com.example.spacebridgeapp.core.data.prefs.PreferencesRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "spacebridge_preferences")

interface AppContainer {
    val firestore: FirebaseFirestore
    val auth: FirebaseAuth
    val dataStore: DataStore<Preferences>
    val networkObserver: NetworkObserver
    val preferencesRepository: PreferencesRepository
    val authRepository: AuthRepository
    val driveAuthManager: com.example.spacebridgeapp.core.data.drive.DriveAuthManager
    val driveApi: com.example.spacebridgeapp.core.data.drive.DriveApi
    val driveRepository: com.example.spacebridgeapp.core.data.drive.DriveRepository
    val notesRepository: NotesRepository
    val filesRepository: FilesRepository
    val legacyMigrator: com.example.spacebridgeapp.core.data.migration.LegacyMigrator
    val fileCacheManager: com.example.spacebridgeapp.core.data.cache.FileCacheManager
}

class DefaultAppContainer(
    private val context: Context,
) : AppContainer {
    override val firestore: FirebaseFirestore
        get() = Firebase.firestore

    override val auth: FirebaseAuth by lazy {
        Firebase.auth
    }

    override val dataStore: DataStore<Preferences> by lazy {
        context.dataStore
    }

    override val networkObserver: NetworkObserver by lazy {
        NetworkConnectivityObserver(context.applicationContext)
    }

    override val preferencesRepository: PreferencesRepository by lazy {
        DefaultPreferencesRepository(dataStore)
    }

    override val authRepository: AuthRepository by lazy {
        com.example.spacebridgeapp.core.data.auth.AuthRepositoryImpl(
            auth = auth,
            context = context.applicationContext,
            preferencesRepository = preferencesRepository,
            firestoreProvider = { firestore },
            driveRepositoryProvider = { driveRepository },
        )
    }

    override val driveAuthManager: com.example.spacebridgeapp.core.data.drive.DriveAuthManager by lazy {
        com.example.spacebridgeapp.core.data.drive.DriveAuthManagerImpl(
            context = context.applicationContext,
            auth = auth,
            preferencesRepository = preferencesRepository,
        )
    }

    override val driveApi: com.example.spacebridgeapp.core.data.drive.DriveApi by lazy {
        val json =
            kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                isLenient = true
                coerceInputValues = true
            }
        val contentType = "application/json".toMediaType()
        retrofit2.Retrofit
            .Builder()
            .baseUrl("https://www.googleapis.com/")
            .client(okhttp3.OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(com.example.spacebridgeapp.core.data.drive.DriveApi::class.java)
    }

    override val driveRepository: com.example.spacebridgeapp.core.data.drive.DriveRepository by lazy {
        com.example.spacebridgeapp.core.data.drive.DriveRepositoryImpl(
            driveApi = driveApi,
            driveAuthManager = driveAuthManager,
            auth = auth,
            firestoreProvider = { firestore },
        )
    }

    override val notesRepository: NotesRepository by lazy {
        com.example.spacebridgeapp.core.data.notes.NotesRepositoryImpl(
            auth = auth,
            firestoreProvider = { firestore },
        )
    }

    override val legacyMigrator: com.example.spacebridgeapp.core.data.migration.LegacyMigrator by lazy {
        com.example.spacebridgeapp.core.data.migration.LegacyMigrator(
            firestoreProvider = { firestore },
        )
    }

    private var _filesRepository: FilesRepository? = null
    override var filesRepository: FilesRepository
        get() =
            _filesRepository ?: FilesRepositoryImpl(
                context = context.applicationContext,
                auth = auth,
                firestoreProvider = { firestore },
                preferencesRepository = preferencesRepository,
                driveRepository = driveRepository,
            ).also { _filesRepository = it }
        set(value) {
            _filesRepository = value
        }

    override val fileCacheManager: com.example.spacebridgeapp.core.data.cache.FileCacheManager by lazy {
        com.example.spacebridgeapp.core.data.cache.DefaultFileCacheManager(context.applicationContext)
    }
}

