package com.example.spacebridgeapp.core.data.drive

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.spacebridgeapp.core.data.prefs.PreferencesRepository
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import sun.misc.Unsafe
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class DriveAuthManagerTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakePreferencesRepository: FakePreferencesRepository
    private lateinit var dummyContext: Context
    private var testEmail: String? = "tester@gmail.com"

    private var authorizeTask: Task<AuthorizationResult> =
        Tasks.forResult(
            AuthorizationResult(null, "fake-token", null, listOf(DriveAuthManagerImpl.DRIVE_FILE_SCOPE), null, null),
        )
    private var resultFromIntent: AuthorizationResult? = null
    private var intentException: Exception? = null
    private var lastCapturedRequest: AuthorizationRequest? = null

    private lateinit var fakeClient: AuthorizationClient

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePreferencesRepository = FakePreferencesRepository()
        dummyContext = createDummyContext()
        testEmail = "tester@gmail.com"
        resultFromIntent = null
        intentException = null
        lastCapturedRequest = null

        fakeClient =
            Proxy.newProxyInstance(
                AuthorizationClient::class.java.classLoader,
                arrayOf(AuthorizationClient::class.java),
            ) { _, method, args ->
                when (method.name) {
                    "authorize" -> {
                        lastCapturedRequest = args?.get(0) as? AuthorizationRequest
                        authorizeTask
                    }
                    "getAuthorizationResultFromIntent" -> {
                        intentException?.let { throw it }
                        resultFromIntent ?: AuthorizationResult(
                            null,
                            "intent-token",
                            null,
                            listOf(DriveAuthManagerImpl.DRIVE_FILE_SCOPE),
                            null,
                            null,
                        )
                    }
                    "equals" -> false
                    "hashCode" -> 0
                    "toString" -> "FakeAuthorizationClient"
                    else -> throw UnsupportedOperationException("Method not mocked: ${method.name}")
                }
            } as AuthorizationClient
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createDummyContext(): Context {
        val field = Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = field.get(null) as Unsafe
        return unsafe.allocateInstance(android.content.ContextWrapper::class.java) as Context
    }

    private fun createDummyPendingIntent(): PendingIntent {
        val field = Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = field.get(null) as Unsafe
        return unsafe.allocateInstance(PendingIntent::class.java) as PendingIntent
    }

    private fun createDummyIntent(): Intent {
        val field = Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = field.get(null) as Unsafe
        return unsafe.allocateInstance(Intent::class.java) as Intent
    }

    private fun createManager(emailProvider: () -> String? = { testEmail }): DriveAuthManagerImpl =
        DriveAuthManagerImpl(
            context = dummyContext,
            preferencesRepository = fakePreferencesRepository,
            clientProvider = { fakeClient },
            coroutineScope = testScope,
            userEmailProvider = emailProvider,
        )

    @Test
    fun `test authorize when granted returns Granted and updates isAuthorized`() =
        runTest(testDispatcher) {
            val expectedToken = "valid-token-123"
            authorizeTask =
                Tasks.forResult(
                    AuthorizationResult(null, expectedToken, null, listOf(DriveAuthManagerImpl.DRIVE_FILE_SCOPE), null, null),
                )

            val manager = createManager()
            advanceUntilIdle()

            val result = manager.authorize()

            assertTrue(result is DriveAuthResult.Granted)
            assertEquals(expectedToken, (result as DriveAuthResult.Granted).accessToken)
            assertTrue(manager.isAuthorized.value)
            assertTrue(fakePreferencesRepository.isDriveAuthorized.first())

            val capturedRequest = lastCapturedRequest
            assertTrue(capturedRequest?.account != null)
            assertEquals(DriveAuthManagerImpl.DRIVE_FILE_SCOPE, capturedRequest?.requestedScopes?.firstOrNull()?.scopeUri)
        }

    @Test
    fun `test authorize when resolution needed returns NeedsConsent and sets isAuthorized false`() =
        runTest(testDispatcher) {
            val pendingIntent = createDummyPendingIntent()
            authorizeTask =
                Tasks.forResult(
                    AuthorizationResult(null, null, null, emptyList(), null, pendingIntent),
                )

            val manager = createManager()
            advanceUntilIdle()

            val result = manager.authorize()

            assertTrue(result is DriveAuthResult.NeedsConsent)
            org.junit.Assert.assertSame(pendingIntent, (result as DriveAuthResult.NeedsConsent).pendingIntent)
            assertFalse(manager.isAuthorized.value)
            assertFalse(fakePreferencesRepository.isDriveAuthorized.first())
        }

    @Test
    fun `test authorize when user not logged in returns Failed`() =
        runTest(testDispatcher) {
            val manager = createManager(emailProvider = { null })
            advanceUntilIdle()

            val result = manager.authorize()

            assertTrue(result is DriveAuthResult.Failed)
            assertFalse(manager.isAuthorized.value)
        }

    @Test
    fun `test authorize when token is empty returns Failed`() =
        runTest(testDispatcher) {
            authorizeTask =
                Tasks.forResult(
                    AuthorizationResult(null, "", null, emptyList(), null, null),
                )

            val manager = createManager()
            advanceUntilIdle()

            val result = manager.authorize()

            assertTrue(result is DriveAuthResult.Failed)
            assertFalse(manager.isAuthorized.value)
        }

    @Test
    fun `test authorize when ApiException with resolution thrown returns NeedsConsent`() =
        runTest(testDispatcher) {
            val pendingIntent = createDummyPendingIntent()
            val statusWithResolution = Status(CommonStatusCodes.SIGN_IN_REQUIRED, "Sign in required", pendingIntent)
            authorizeTask = Tasks.forException(ApiException(statusWithResolution))

            val manager = createManager()
            advanceUntilIdle()

            val result = manager.authorize()

            assertTrue(result is DriveAuthResult.NeedsConsent)
            org.junit.Assert.assertSame(pendingIntent, (result as DriveAuthResult.NeedsConsent).pendingIntent)
            assertFalse(manager.isAuthorized.value)
        }

    @Test
    fun `test authorize when ApiException without resolution thrown returns Failed`() =
        runTest(testDispatcher) {
            val statusNoResolution = Status(CommonStatusCodes.INTERNAL_ERROR, "Internal error")
            authorizeTask = Tasks.forException(ApiException(statusNoResolution))

            val manager = createManager()
            advanceUntilIdle()

            val result = manager.authorize()

            assertTrue(result is DriveAuthResult.Failed)
            assertFalse(manager.isAuthorized.value)
        }

    @Test
    fun `test handleConsentResult with null data returns Failed`() =
        runTest(testDispatcher) {
            val manager = createManager()
            advanceUntilIdle()

            val result = manager.handleConsentResult(null)

            assertTrue(result is DriveAuthResult.Failed)
            assertFalse(manager.isAuthorized.value)
        }

    @Test
    fun `test handleConsentResult with valid data returns Granted`() =
        runTest(testDispatcher) {
            val expectedToken = "consent-token-456"
            resultFromIntent =
                AuthorizationResult(
                    null,
                    expectedToken,
                    null,
                    listOf(DriveAuthManagerImpl.DRIVE_FILE_SCOPE),
                    null,
                    null,
                )

            val manager = createManager()
            advanceUntilIdle()

            val dummyIntent = createDummyIntent()
            val result = manager.handleConsentResult(dummyIntent)

            assertTrue(result is DriveAuthResult.Granted)
            assertEquals(expectedToken, (result as DriveAuthResult.Granted).accessToken)
            assertTrue(manager.isAuthorized.value)
            assertTrue(fakePreferencesRepository.isDriveAuthorized.first())
        }

    @Test
    fun `test handleConsentResult with empty token returns Failed`() =
        runTest(testDispatcher) {
            resultFromIntent = AuthorizationResult(null, "", null, emptyList(), null, null)

            val manager = createManager()
            advanceUntilIdle()

            val dummyIntent = createDummyIntent()
            val result = manager.handleConsentResult(dummyIntent)

            assertTrue(result is DriveAuthResult.Failed)
            assertFalse(manager.isAuthorized.value)
        }

    @Test
    fun `test handleConsentResult when ApiException thrown returns Failed`() =
        runTest(testDispatcher) {
            intentException = ApiException(Status(CommonStatusCodes.ERROR, "Failed"))

            val manager = createManager()
            advanceUntilIdle()

            val dummyIntent = createDummyIntent()
            val result = manager.handleConsentResult(dummyIntent)

            assertTrue(result is DriveAuthResult.Failed)
            assertFalse(manager.isAuthorized.value)
        }

    @Test
    fun `test getTokenSilently returns token when authorized`() =
        runTest(testDispatcher) {
            val expectedToken = "silent-token-789"
            authorizeTask =
                Tasks.forResult(
                    AuthorizationResult(null, expectedToken, null, listOf(DriveAuthManagerImpl.DRIVE_FILE_SCOPE), null, null),
                )

            val manager = createManager()
            advanceUntilIdle()

            val token = manager.getTokenSilently()

            assertEquals(expectedToken, token)
            assertTrue(manager.isAuthorized.value)
            assertTrue(fakePreferencesRepository.isDriveAuthorized.first())
        }

    @Test
    fun `test getTokenSilently returns null when consent required`() =
        runTest(testDispatcher) {
            val pendingIntent = createDummyPendingIntent()
            authorizeTask =
                Tasks.forResult(
                    AuthorizationResult(null, null, null, emptyList(), null, pendingIntent),
                )

            val manager = createManager()
            advanceUntilIdle()

            val token = manager.getTokenSilently()

            assertNull(token)
            assertFalse(manager.isAuthorized.value)
        }

    @Test
    fun `test getTokenSilently returns null when user not logged in`() =
        runTest(testDispatcher) {
            val manager = createManager(emailProvider = { null })
            advanceUntilIdle()

            val token = manager.getTokenSilently()

            assertNull(token)
            assertFalse(manager.isAuthorized.value)
        }

    @Test
    fun `test getTokenSilently returns null when exception occurs`() =
        runTest(testDispatcher) {
            authorizeTask = Tasks.forException(ApiException(Status.RESULT_INTERNAL_ERROR))

            val manager = createManager()
            advanceUntilIdle()

            val token = manager.getTokenSilently()

            assertNull(token)
            assertFalse(manager.isAuthorized.value)
        }

    @Test
    fun `test revoke resets isAuthorized and updates preferences`() =
        runTest(testDispatcher) {
            fakePreferencesRepository.setDriveAuthorized(true)

            val manager = createManager()
            advanceUntilIdle()

            manager.revoke()
            advanceUntilIdle()

            assertFalse(manager.isAuthorized.value)
            assertFalse(fakePreferencesRepository.isDriveAuthorized.first())
        }

    @Test
    fun `test invalidateToken does not throw and clears token`() =
        runTest(testDispatcher) {
            val manager = createManager()
            advanceUntilIdle()

            manager.invalidateToken("any-token")
        }

    @Test
    fun `test initial state reflects preferencesRepository`() =
        runTest(testDispatcher) {
            fakePreferencesRepository.setDriveAuthorized(true)

            val manager = createManager()
            advanceUntilIdle()

            assertTrue(manager.isAuthorized.value)
        }
}

class FakePreferencesRepository : PreferencesRepository {
    private val _isDriveAuthorized = MutableStateFlow(false)
    override val isDriveAuthorized: Flow<Boolean> = _isDriveAuthorized

    private val _themeMode = MutableStateFlow<String?>(null)
    override val themeMode: Flow<String?> = _themeMode

    private val _isWifiOnlyUpload = MutableStateFlow(true)
    override val isWifiOnlyUpload: Flow<Boolean> = _isWifiOnlyUpload

    private val _isGridLayout = MutableStateFlow(true)
    override val isGridLayout: Flow<Boolean> = _isGridLayout

    private val _hasSeenDriveOnboarding = MutableStateFlow(false)
    override val hasSeenDriveOnboarding: Flow<Boolean> = _hasSeenDriveOnboarding

    override suspend fun setThemeMode(mode: String?) {
        _themeMode.value = mode
    }

    override suspend fun setWifiOnlyUpload(wifiOnly: Boolean) {
        _isWifiOnlyUpload.value = wifiOnly
    }

    override suspend fun setGridLayout(isGrid: Boolean) {
        _isGridLayout.value = isGrid
    }

    override suspend fun setSeenDriveOnboarding(seen: Boolean) {
        _hasSeenDriveOnboarding.value = seen
    }

    override suspend fun setDriveAuthorized(authorized: Boolean) {
        _isDriveAuthorized.value = authorized
    }

    override suspend fun clearUserPreferences() {
        _hasSeenDriveOnboarding.value = false
        _isDriveAuthorized.value = false
    }
}
