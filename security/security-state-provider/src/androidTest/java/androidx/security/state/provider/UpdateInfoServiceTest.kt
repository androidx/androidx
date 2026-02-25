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
import androidx.security.state.IUpdateInfoService
import androidx.security.state.UpdateInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
        service = TestUpdateInfoService()
        service.attach(context)
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
    fun listAvailableUpdates_returnsCachedDataFromManager() {
        // 1. Setup: Seed the SharedPreferences with data
        val updateInfo =
            UpdateInfo.Builder()
                .setComponent("SYSTEM")
                .setSecurityPatchLevel("2025-01-01")
                .setPublishedDateMillis(1L)
                .setLastCheckTimeMillis(1000L)
                .build()

        // Use the Manager directly to seed data
        val manager = UpdateInfoManager(context)
        manager.registerUpdate(updateInfo)
        manager.setLastCheckTimeMillis(1000L)

        // 2. Action: Call the Service method via the Binder interface
        val binder =
            service.onBind(Intent("androidx.security.state.provider.UPDATE_INFO_SERVICE"))
                as IUpdateInfoService

        val result = binder.listAvailableUpdates()

        // 3. Verify
        assertEquals("Should return 1 update", 1, result.updates.size)
        assertEquals("SYSTEM", result.updates[0].component)
        assertEquals("2025-01-01", result.updates[0].securityPatchLevel)
        assertEquals(1L, result.updates[0].publishedDateMillis)
        assertEquals(1000L, result.updates[0].lastCheckTimeMillis)
        assertEquals(1000L, result.lastCheckTimeMillis)
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
                .setSecurityPatchLevel(futureDate)
                .setPublishedDateMillis(1L)
                .build()

        val u2 =
            UpdateInfo.Builder()
                .setComponent("SYSTEM_MODULES")
                .setSecurityPatchLevel(futureDate)
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
}
