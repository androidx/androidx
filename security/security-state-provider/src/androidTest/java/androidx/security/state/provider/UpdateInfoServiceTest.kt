/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.security.state.provider

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Process.myUid
import androidx.security.state.IUpdateInfoService
import androidx.security.state.SecurityPatchState
import androidx.security.state.UpdateInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpdateInfoServiceTest {

    private lateinit var context: Context
    private lateinit var service: TestUpdateInfoService

    // A service instance dedicated to testing the real implementation
    private lateinit var realService: UpdateInfoService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear shared preferences before each test to ensure a clean slate for each run.
        context
            .getSharedPreferences("UPDATE_INFO_PREFS", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context
            .getSharedPreferences("UPDATE_INFO_METADATA_PREFS", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        // Initialize the Test Double service used for logic and concurrency tests
        service = TestUpdateInfoService()
        service.attach(context)

        // Initialize the Real service used strictly for package validation and security tests
        realService =
            object : UpdateInfoService() {
                    override suspend fun fetchUpdates(): List<UpdateInfo> = emptyList()

                    fun attach(c: Context) {
                        super.attachBaseContext(c)
                    }
                }
                .apply { attach(context) }
    }

    @Test
    fun onBind_returnsBinder_forCorrectAction() {
        val intent = Intent("androidx.security.state.provider.UPDATE_INFO_SERVICE")
        val binder = service.onBind(intent)
        assertNotNull("Should return binder for matching action", binder)
    }

    @Test
    fun onBind_returnsNull_forIncorrectAction() {
        val intent = Intent("com.example.WRONG_ACTION")
        val binder = service.onBind(intent)
        assertNull("Should reject incorrect action", binder)
    }

    @Test
    fun openSession_returnsDistinctSessionsForMultipleClients() {
        val intent = Intent("androidx.security.state.provider.UPDATE_INFO_SERVICE")
        val factory = service.onBind(intent) as IUpdateInfoService

        // WHEN multiple clients open sessions
        val session1 = factory.openSession("com.client.one", Binder())
        val session2 = factory.openSession("com.client.two", Binder())

        // THEN they receive distinct binder instances
        Assert.assertNotSame("Factory should return distinct session instances", session1, session2)
    }

    @Test(expected = SecurityException::class)
    fun openSession_throwsSecurityException_forSpoofedPackage() {
        // GIVEN the validation check will fail (simulating a spoofed package)
        service.testIsValidPackage = false

        val intent = Intent("androidx.security.state.provider.UPDATE_INFO_SERVICE")
        val factory = service.onBind(intent) as IUpdateInfoService

        // WHEN a malicious client tries to open a session with a spoofed name
        // THEN it throws a SecurityException
        factory.openSession("com.google.android.settings", Binder())
    }

    @Test
    fun testSession_linksToClientTokenDeath() {
        // GIVEN a fake client token
        val fakeToken = FakeToken()
        val factory =
            service.onBind(Intent("androidx.security.state.provider.UPDATE_INFO_SERVICE"))
                as IUpdateInfoService

        // WHEN the client opens a session
        factory.openSession("com.example.client", fakeToken)

        // THEN the service registers a death recipient on the provided token
        assertNotNull("Service should register a DeathRecipient", fakeToken.deathRecipient)
    }

    @Test
    fun testSession_unlinksFromClientTokenDeath_onClose() {
        // GIVEN an active session with a fake token
        val fakeToken = FakeToken()
        val factory =
            service.onBind(Intent("androidx.security.state.provider.UPDATE_INFO_SERVICE"))
                as IUpdateInfoService
        val session = factory.openSession("com.example.client", fakeToken)

        assertNotNull("DeathRecipient should be registered initially", fakeToken.deathRecipient)

        // WHEN the client closes the session
        session.close()

        // THEN the service unregisters the death recipient to prevent memory/kernel leaks
        assertNull(
            "Service should unregister the DeathRecipient on close",
            fakeToken.deathRecipient,
        )
    }

    @Test(expected = IllegalStateException::class)
    fun listAvailableUpdates_throwsException_ifSessionIsClosed() {
        // GIVEN an active session
        val intent = Intent("androidx.security.state.provider.UPDATE_INFO_SERVICE")
        val factory = service.onBind(intent) as IUpdateInfoService
        val session = factory.openSession("com.example.client", Binder())

        // WHEN the client closes the session
        session.close()

        // THEN further calls on that session binder throw an IllegalStateException
        session.listAvailableUpdates()
    }

    @Test
    fun fetchUpdates_callsFetchUpdatesAsync_whenOverridden() = runBlocking {
        // Create a service that ONLY overrides fetchUpdatesAsync
        val asyncService =
            object : ListenableFutureUpdateInfoService() {
                    override fun fetchUpdatesAsync():
                        com.google.common.util.concurrent.ListenableFuture<
                            @JvmSuppressWildcards
                            List<UpdateInfo>
                        > {
                        return androidx.concurrent.futures.SuspendToFutureAdapter.launchFuture(
                            kotlinx.coroutines.Dispatchers.IO
                        ) {
                            listOf(
                                UpdateInfo.Builder()
                                    .setComponent("SYSTEM")
                                    .setSecurityPatchLevel(
                                        SecurityPatchState.DateBasedSecurityPatchLevel.fromString(
                                            "2025-01-01"
                                        )
                                    )
                                    .build()
                            )
                        }
                    }

                    fun attach(c: Context) {
                        super.attachBaseContext(c)
                    }
                }
                .apply { attach(context) }

        // We can't call fetchUpdates directly because it's protected,
        // so we'll test it via the same path as callListAvailableUpdates
        val intent = Intent("androidx.security.state.provider.UPDATE_INFO_SERVICE")
        val factory = asyncService.onBind(intent) as IUpdateInfoService
        val session = factory.openSession(context.packageName, Binder())

        // Use reflection or just test via the public API that it successfully fetches
        // For simplicity, we just trigger listAvailableUpdates and see if it succeeds.
        val result = session.listAvailableUpdates()
        session.close()

        assertEquals(1, result.updates.size)
        assertEquals("SYSTEM", result.updates[0].component)
        assertEquals("2025-01-01", result.updates[0].securityPatchLevel.toString())
    }

    @Test
    fun listAvailableUpdates_returnsCachedDataFromManager() {
        // 1. Setup: Seed the SharedPreferences with data
        val updateInfo =
            UpdateInfo.Builder()
                .setComponent("SYSTEM")
                .setSecurityPatchLevel(
                    SecurityPatchState.DateBasedSecurityPatchLevel.fromString("2025-01-01")
                )
                .setPublishedDateMillis(1L)
                .setLastCheckTimeMillis(1000L)
                .build()

        // Use the Manager directly to seed data
        val manager = UpdateInfoManager(context)
        manager.registerUpdate(updateInfo)
        manager.setLastCheckTimeMillis(1000L)

        val factory =
            service.onBind(Intent("androidx.security.state.provider.UPDATE_INFO_SERVICE"))
                as IUpdateInfoService

        // Open the session with a verified package name
        val session = factory.openSession("com.example.client", Binder())

        // Retrieve the data from the session
        val result = session.listAvailableUpdates()

        // 3. Verify
        assertEquals("Should return 1 update", 1, result.updates.size)
        assertEquals("SYSTEM", result.updates[0].component)
        assertEquals("2025-01-01", result.updates[0].securityPatchLevel.toString())
        assertEquals(1L, result.updates[0].publishedDateMillis)
        assertEquals(1000L, result.updates[0].lastCheckTimeMillis)

        // Verify the global check time matches
        assertEquals(1000L, result.lastCheckTimeMillis)

        // Cleanup the session
        session.close()
    }

    @Test
    fun listAvailableUpdates_returnsCachedData_whenFresh() {
        // GIVEN cache is "Fresh" (shouldFetch = false)
        service.testShouldFetch = false

        // WHEN we call the API
        val result = service.callListAvailableUpdates()

        // THEN no network fetch occurs, and we get a result
        assertEquals(0, service.fetchCount)
        assertNotNull(result)
    }

    @Test
    fun listAvailableUpdates_fetches_whenStaleAndNotThrottled() {
        // GIVEN cache is "Stale" and we are NOT throttled
        service.testShouldFetch = true
        service.testIsThrottled = false

        // WHEN we call the API
        service.callListAvailableUpdates()

        // THEN fetch is called exactly once
        assertEquals(1, service.fetchCount)
    }

    @Test
    fun listAvailableUpdates_returnsCachedData_whenThrottled() {
        // GIVEN cache is "Stale" BUT we ARE throttled
        service.testShouldFetch = true
        service.testIsThrottled = true

        // WHEN we call the API
        val result = service.callListAvailableUpdates()

        // THEN fetch is skipped (Graceful Degradation)
        assertEquals(0, service.fetchCount)
        assertNotNull(result)
    }

    @Test
    fun listAvailableUpdates_usesDoubleCheckedLocking_forConcurrency() = runBlocking {
        // GIVEN cache is "Stale" and multiple threads want updates
        service.testShouldFetch = true
        service.testIsThrottled = false

        // Latches to synchronize threads
        val threadAEntered = CompletableDeferred<Unit>()
        val allowThreadAToFinish = CompletableDeferred<Unit>()

        // Configure the Stub:
        service.onFetchUpdates = {
            // 1. Notify that we have acquired the lock
            threadAEntered.complete(Unit)
            // 2. Wait for the test to signal us to proceed
            allowThreadAToFinish.await()
        }

        // 1. Launch Thread A (The "First" request)
        val jobA = async(Dispatchers.IO) { service.callListAvailableUpdates() }

        // 2. Wait for Thread A to definitely acquire the lock
        threadAEntered.await()

        // 3. Launch Thread B (The "Second" request)
        //    We KNOW Thread A holds the lock now, so Thread B *must* block.
        val jobB = async(Dispatchers.IO) { service.callListAvailableUpdates() }

        // 4. Release Thread A
        allowThreadAToFinish.complete(Unit)

        // 5. Wait for both to finish
        jobA.await()
        jobB.await()

        // VERIFY: fetchUpdates was called exactly ONCE (requests were coalesced)
        assertEquals("Should coalesce concurrent requests", 1, service.fetchCount)
    }

    @Test
    fun listAvailableUpdates_handlesFetchException_gracefully() {
        // GIVEN cache is stale, but the network fetch will fail
        service.testShouldFetch = true
        service.shouldThrowError = true

        // WHEN we call the API
        val result = service.callListAvailableUpdates()

        // THEN the service catches the exception and returns cached data instead of crashing
        assertEquals(1, service.fetchCount)
        assertNotNull("Should return cached result on failure", result)
        assertTrue("Should invoke error handler", service.wasOnFetchFailedCalled)
    }

    @Test
    fun listAvailableUpdates_persistsMultipleUpdates_viaLoop() = runBlocking {
        // GIVEN cache is stale
        service.testShouldFetch = true
        service.testIsThrottled = false

        // Use Calendar/SimpleDateFormat for API 23 compatibility.
        // We generate a date 1 year in the future to ensure the update is considered
        // "newer" than the device's OS version, bypassing the cleanup logic.
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.YEAR, 1)
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val futureDate = dateFormat.format(calendar.time)

        val u1 =
            UpdateInfo.Builder()
                .setComponent("SYSTEM")
                .setSecurityPatchLevel(
                    SecurityPatchState.DateBasedSecurityPatchLevel.fromString(futureDate)
                )
                .setPublishedDateMillis(1L)
                .build()

        val u2 =
            UpdateInfo.Builder()
                .setComponent("SYSTEM_MODULES")
                .setSecurityPatchLevel(
                    SecurityPatchState.DateBasedSecurityPatchLevel.fromString(futureDate)
                )
                .setPublishedDateMillis(1L)
                .build()

        service.updatesToReturn = listOf(u1, u2)

        // WHEN we call the API
        val result = service.callListAvailableUpdates()

        // THEN we get both updates back
        assertEquals(2, result.updates.size)

        // AND they are persisted to storage (cleanup did NOT delete them)
        val storedUpdates = UpdateInfoManager(context).getAllUpdates()
        assertEquals(2, storedUpdates.size)
    }

    @Test
    fun shouldFetchUpdates_usesDefaultOneHourLogic() {
        // Enable the REAL logic (calling super.shouldFetchUpdates)
        service.useRealFreshnessLogic = true
        val manager = UpdateInfoManager(context)

        // 1. Case: Fresh (Checked 30 mins ago)
        val thirtyMinsAgo =
            System.currentTimeMillis() - java.util.concurrent.TimeUnit.MINUTES.toMillis(30)
        manager.setLastCheckTimeMillis(thirtyMinsAgo)

        // Assert: Should NOT fetch
        org.junit.Assert.assertFalse("Should be fresh (< 1 hour)", service.callShouldFetchUpdates())

        // 2. Case: Stale (Checked 2 hours ago)
        val twoHoursAgo =
            System.currentTimeMillis() - java.util.concurrent.TimeUnit.HOURS.toMillis(2)
        manager.setLastCheckTimeMillis(twoHoursAgo)

        // Assert: SHOULD fetch
        assertTrue("Should be stale (> 1 hour)", service.callShouldFetchUpdates())
    }

    @Test
    fun enforceValidPackageForUid_returnsTrue_forActualApp() {
        // WHEN we enforce validation using the actual test app's package and UID
        // THEN it succeeds silently (no SecurityException is thrown) because the
        // real Android OS confirms we own this process.
        realService.enforceValidPackageForUid(context.packageName, myUid())
    }

    @Test(expected = SecurityException::class)
    fun enforceValidPackageForUid_throwsException_forSpoofedPackage() {
        // WHEN we validate a spoofed/malicious package name against our real UID
        // THEN the real Android OS rejects it, throwing a SecurityException
        realService.enforceValidPackageForUid("com.malicious.spoof.app", myUid())
    }

    @Test(expected = SecurityException::class)
    fun enforceValidPackageForUid_throwsException_forUnknownUid() {
        // WHEN we validate our real package name against a fake, unused UID
        // THEN the OS rejects the mismatch, throwing a SecurityException
        realService.enforceValidPackageForUid(
            context.packageName,
            9999999, // Non-existent UID
        )
    }

    /**
     * A Fake implementation of an Android Binder token.
     *
     * This avoids Mockito limitations on Android VMs and allows us to securely capture and trigger
     * the DeathRecipient registered by the service.
     */
    class FakeToken : Binder() {
        var deathRecipient: IBinder.DeathRecipient? = null

        override fun linkToDeath(recipient: IBinder.DeathRecipient, flags: Int) {
            this.deathRecipient = recipient
        }

        override fun unlinkToDeath(recipient: IBinder.DeathRecipient, flags: Int): Boolean {
            if (this.deathRecipient === recipient) {
                this.deathRecipient = null
                return true
            }
            return false
        }
    }
}
