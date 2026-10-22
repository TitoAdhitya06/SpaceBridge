package com.example.spacebridgeapp.feature.settings

import android.app.Activity
import android.content.Context
import com.example.spacebridgeapp.core.data.auth.AuthRepository
import com.example.spacebridgeapp.core.data.cache.FileCacheManager
import com.example.spacebridgeapp.core.data.network.NetworkObserver
import com.example.spacebridgeapp.core.data.prefs.PreferencesRepository
import com.example.spacebridgeapp.core.model.AppUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakePreferencesRepository: FakePreferencesRepository
    private lateinit var fakeFileCacheManager: FakeFileCacheManager
    private lateinit var fakeNetworkObserver: FakeNetworkObserver
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
        fakePreferencesRepository = FakePreferencesRepository()
        fakeFileCacheManager = FakeFileCacheManager(currentSizeBytes = 25 * 1024 * 1024L)
        fakeNetworkObserver = FakeNetworkObserver()

        viewModel =
            SettingsViewModel(
                authRepository = fakeAuthRepository,
                preferencesRepository = fakePreferencesRepository,
                fileCacheManager = fakeFileCacheManager,
                networkObserver = fakeNetworkObserver,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state emits user profile and preferences accurately`() =
        runTest {
            val user =
                AppUser(
                    uid = "test-uid",
                    displayName = "Tito Pamungkas",
                    email = "tito@example.com",
                )
            fakeAuthRepository.setCurrentUser(user)
            fakePreferencesRepository.setDriveAuthorized(true)
            fakePreferencesRepository.setWifiOnlyUpload(true)
            fakePreferencesRepository.setThemeMode("dark")
            fakeNetworkObserver.setConnected(true)

            val collectJob = launch(testDispatcher) { viewModel.uiState.collect() }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(user, state.currentUser)
            assertTrue(state.isDriveAuthorized)
            assertTrue(state.isWifiOnlyUpload)
            assertEquals(ThemeOption.DARK, state.themeOption)
            assertTrue(state.isConnected)
            assertEquals(25 * 1024 * 1024L, state.cacheSizeBytes)
            assertFalse(state.isSignedOut)

            collectJob.cancel()
        }

    @Test
    fun `onToggleWifiOnly toggles preferences value in repository`() =
        runTest {
            val collectJob = launch(testDispatcher) { viewModel.uiState.collect() }
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isWifiOnlyUpload)

            viewModel.onToggleWifiOnly(false)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isWifiOnlyUpload)

            viewModel.onToggleWifiOnly(true)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isWifiOnlyUpload)

            collectJob.cancel()
        }

    @Test
    fun `onSelectTheme updates themeMode in preferences repository`() =
        runTest {
            val collectJob = launch(testDispatcher) { viewModel.uiState.collect() }
            advanceUntilIdle()

            viewModel.onSelectTheme(ThemeOption.LIGHT)
            advanceUntilIdle()
            assertEquals(ThemeOption.LIGHT, viewModel.uiState.value.themeOption)

            viewModel.onSelectTheme(ThemeOption.DARK)
            advanceUntilIdle()
            assertEquals(ThemeOption.DARK, viewModel.uiState.value.themeOption)

            viewModel.onSelectTheme(ThemeOption.SYSTEM)
            advanceUntilIdle()
            assertEquals(ThemeOption.SYSTEM, viewModel.uiState.value.themeOption)

            collectJob.cancel()
        }

    @Test
    fun `onClearCache invokes cacheManager and updates cacheSizeBytes to zero with temporary success feedback`() =
        runTest {
            val collectJob = launch(testDispatcher) { viewModel.uiState.collect() }
            advanceUntilIdle()

            assertEquals(25 * 1024 * 1024L, viewModel.uiState.value.cacheSizeBytes)

            viewModel.onClearCache()
            testDispatcher.scheduler.advanceTimeBy(100L)

            assertEquals(0L, viewModel.uiState.value.cacheSizeBytes)
            assertTrue(viewModel.uiState.value.isCacheClearedSuccess)
            assertFalse(viewModel.uiState.value.isClearingCache)

            testDispatcher.scheduler.advanceTimeBy(2100L)
            assertFalse(viewModel.uiState.value.isCacheClearedSuccess)

            collectJob.cancel()
        }

    @Test
    fun `refreshCacheSize updates cache size from fileCacheManager`() =
        runTest {
            val collectJob = launch(testDispatcher) { viewModel.uiState.collect() }
            advanceUntilIdle()

            assertEquals(25 * 1024 * 1024L, viewModel.uiState.value.cacheSizeBytes)

            fakeFileCacheManager.setCacheSize(50 * 1024 * 1024L)
            viewModel.refreshCacheSize()
            advanceUntilIdle()

            assertEquals(50 * 1024 * 1024L, viewModel.uiState.value.cacheSizeBytes)

            collectJob.cancel()
        }

    @Test
    fun `onSignOut signs out from authRepository and updates isSignedOut to true`() =
        runTest {
            fakeAuthRepository.setCurrentUser(
                AppUser(uid = "user-1", displayName = "User One", email = "one@example.com"),
            )
            val collectJob = launch(testDispatcher) { viewModel.uiState.collect() }
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isSignedOut)

            viewModel.onSignOut()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isSignedOut)
            assertTrue(fakeAuthRepository.isSignOutCalled)

            collectJob.cancel()
        }

    @Test
    fun `onDeleteAccount success deletes account, signs out, and updates state`() =
        runTest {
            fakeAuthRepository.setCurrentUser(
                AppUser(uid = "user-1", displayName = "User One", email = "one@example.com"),
            )
            val collectJob = launch(testDispatcher) { viewModel.uiState.collect() }
            advanceUntilIdle()

            val activity = Activity()
            viewModel.onDeleteAccount(activity = activity, trashDriveFolder = true)
            advanceUntilIdle()

            assertTrue(fakeAuthRepository.isDeleteAccountCalled)
            assertEquals(true, fakeAuthRepository.lastTrashDriveFolder)
            assertTrue(viewModel.uiState.value.isSignedOut)
            assertFalse(viewModel.uiState.value.isDeletingAccount)

            collectJob.cancel()
        }

    @Test
    fun `onDeleteAccount failure sets deleteAccountError and does not sign out`() =
        runTest {
            fakeAuthRepository.setCurrentUser(
                AppUser(uid = "user-1", displayName = "User One", email = "one@example.com"),
            )
            fakeAuthRepository.shouldDeleteAccountFail = true
            val collectJob = launch(testDispatcher) { viewModel.uiState.collect() }
            advanceUntilIdle()

            val activity = Activity()
            viewModel.onDeleteAccount(activity = activity, trashDriveFolder = false)
            advanceUntilIdle()

            assertTrue(fakeAuthRepository.isDeleteAccountCalled)
            assertEquals(false, fakeAuthRepository.lastTrashDriveFolder)
            assertFalse(viewModel.uiState.value.isSignedOut)
            assertFalse(viewModel.uiState.value.isDeletingAccount)
            assertEquals("Gagal menghubungi server otentikasi", viewModel.uiState.value.deleteAccountError)

            viewModel.clearDeleteAccountError()
            advanceUntilIdle()
            assertEquals(null, viewModel.uiState.value.deleteAccountError)

            collectJob.cancel()
        }

    @Test
    fun `factory provides instance of SettingsViewModel correctly`() {
        val factory =
            SettingsViewModel.provideFactory(
                authRepository = fakeAuthRepository,
                preferencesRepository = fakePreferencesRepository,
                fileCacheManager = fakeFileCacheManager,
                networkObserver = fakeNetworkObserver,
            )
        val created = factory.create(SettingsViewModel::class.java)
        assertEquals(SettingsViewModel::class.java, created.javaClass)
    }

    private class FakeAuthRepository : AuthRepository {
        private val _currentUser = MutableStateFlow<AppUser?>(null)
        override val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

        var isSignOutCalled = false
        var isDeleteAccountCalled = false
        var lastTrashDriveFolder: Boolean? = null
        var shouldDeleteAccountFail = false

        fun setCurrentUser(user: AppUser?) {
            _currentUser.value = user
        }

        override suspend fun signIn(activity: Activity): Result<Unit> = Result.success(Unit)

        override suspend fun signIn(context: Context): Result<Unit> = Result.success(Unit)

        override suspend fun signOut() {
            isSignOutCalled = true
            _currentUser.value = null
        }

        override suspend fun deleteAccount(
            activity: Activity,
            trashDriveFolder: Boolean,
        ): Result<Unit> {
            isDeleteAccountCalled = true
            lastTrashDriveFolder = trashDriveFolder
            if (shouldDeleteAccountFail) {
                return Result.failure(IllegalStateException("Gagal menghubungi server otentikasi"))
            }
            signOut()
            return Result.success(Unit)
        }
    }

    private class FakePreferencesRepository : PreferencesRepository {
        private val _themeMode = MutableStateFlow<String?>(null)
        private val _isWifiOnly = MutableStateFlow(true)
        private val _isGrid = MutableStateFlow(true)
        private val _hasSeenOnboarding = MutableStateFlow(false)
        private val _isDriveAuthorized = MutableStateFlow(false)

        override val themeMode: Flow<String?> = _themeMode.asStateFlow()
        override val isWifiOnlyUpload: Flow<Boolean> = _isWifiOnly.asStateFlow()
        override val isGridLayout: Flow<Boolean> = _isGrid.asStateFlow()
        override val hasSeenDriveOnboarding: Flow<Boolean> = _hasSeenOnboarding.asStateFlow()
        override val isDriveAuthorized: Flow<Boolean> = _isDriveAuthorized.asStateFlow()

        override suspend fun setThemeMode(mode: String?) {
            _themeMode.value = mode
        }

        override suspend fun setWifiOnlyUpload(wifiOnly: Boolean) {
            _isWifiOnly.value = wifiOnly
        }

        override suspend fun setGridLayout(isGrid: Boolean) {
            _isGrid.value = isGrid
        }

        override suspend fun setSeenDriveOnboarding(seen: Boolean) {
            _hasSeenOnboarding.value = seen
        }

        override suspend fun setDriveAuthorized(authorized: Boolean) {
            _isDriveAuthorized.value = authorized
        }

        override suspend fun clearUserPreferences() {
            _hasSeenOnboarding.value = false
            _isDriveAuthorized.value = false
        }
    }

    private class FakeFileCacheManager(
        private var currentSizeBytes: Long = 0L,
    ) : FileCacheManager {
        override suspend fun getCacheSizeBytes(): Long = currentSizeBytes

        override suspend fun clearCache(): Boolean {
            currentSizeBytes = 0L
            return true
        }

        fun setCacheSize(size: Long) {
            currentSizeBytes = size
        }
    }

    private class FakeNetworkObserver : NetworkObserver {
        private val _isConnected = MutableStateFlow(true)
        override val isConnected: Flow<Boolean> = _isConnected.asStateFlow()

        fun setConnected(connected: Boolean) {
            _isConnected.value = connected
        }
    }
}
