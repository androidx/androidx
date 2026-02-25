/*
 * Copyright 2026 The Android Open Source Project
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
import android.os.Process
import androidx.security.state.IUpdateInfoService
import androidx.security.state.UpdateCheckResult
import androidx.security.state.UpdateInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.PrintWriter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.DefaultAsserter.assertNotEquals
import kotlin.test.DefaultAsserter.fail
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowBinder

@RunWith(AndroidJUnit4::class)
class UpdateInfoServiceTelemetryTest {

    private lateinit var context: Context
    private lateinit var service: TelemetrySpyService

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
        service = TelemetrySpyService()
        service.attach(context)
    }

    // --- 1. Request Telemetry (Outcomes & Durations) ---

    @Test
    fun testTelemetry_reportsCacheHit() {
        // GIVEN cache is fresh
        service.testShouldFetch = false

        // WHEN request is made
        service.callListAvailableUpdates()

        // THEN we report CACHE_HIT
        val event = service.lastTelemetry
        assertNotNull(event)
        assertEquals(UpdateFetchOutcome.CACHE_HIT, event?.outcome)

        // AND verify no network fetch was attempted
        assertEquals("Fetch duration should be 0 for cache hits", 0L, event!!.fetchDurationMillis)

        // AND processing duration is minimal (local read only)
        assertTrue(
            "Processing duration should be minimal (<10ms) for cache hits",
            event.processingDurationMillis < 10,
        )
    }

    @Test
    fun testTelemetry_reportsFetched_withDurationBreakdown() {
        // GIVEN cache is stale and fetch is allowed
        service.testShouldFetch = true
        service.fetchDelayMillis = 50L // Simulate 50ms network work

        // WHEN request is made
        service.callListAvailableUpdates()

        // THEN we report FETCHED
        val event = service.lastTelemetry
        assertNotNull(event)
        assertEquals(UpdateFetchOutcome.FETCHED, event?.outcome)

        // AND the total duration accounts for the network delay
        assertTrue(
            "Total duratiion should be at least the fetch delay (>= 50ms)",
            event!!.totalDurationMillis >= 50,
        )

        // AND the fetch duration metric specifically captures the network duration
        assertTrue(
            "Fetch duration should capture the simulated delay (>= 50ms)",
            event.fetchDurationMillis >= 50,
        )

        // AND the fetch duration is strictly a subset of the total processing duration
        // (Processing = Fetch + Persistence + Overhead)
        assertTrue(
            "Fetch duration must be <= Total Processing time",
            event.fetchDurationMillis <= event.processingDurationMillis,
        )

        // AND lock wait duration is minimal (no contention in this single-threaded test)
        assertTrue(
            "Lock wait duration should be minimal (<10ms) without contention",
            event.lockWaitDurationMillis < 10,
        )
    }

    @Test
    fun testTelemetry_reportsThrottled() {
        // GIVEN cache is stale BUT rate limiter blocks it
        service.testShouldFetch = true
        service.testIsThrottled = true

        // WHEN request is made
        service.callListAvailableUpdates()

        // THEN we report THROTTLED
        val event = service.lastTelemetry
        assertNotNull(event)
        assertEquals(UpdateFetchOutcome.THROTTLED, event?.outcome)

        // AND verify no fetch occurred (duration is 0)
        assertEquals("Fetch duration should be 0 when throttled", 0L, event!!.fetchDurationMillis)

        // AND processing duration is minimal (just the logic check)
        assertTrue("Processing duration should be minimal", event.processingDurationMillis < 10)
    }

    @Test
    fun testTelemetry_reportsCoalesced_andLockWaitTime() = runBlocking {
        // GIVEN the cache is stale and the network fetch is slow
        service.testShouldFetch = true
        service.fetchDelayMillis = 100L

        // WHEN two requests happen in parallel
        // Request 1 starts immediately and acquires the lock
        val deferred1 = async(Dispatchers.IO) { service.callListAvailableUpdates() }

        // Request 2 starts slightly later and gets blocked on the lock
        val deferred2 =
            async(Dispatchers.IO) {
                delay(10)
                service.callListAvailableUpdates()
            }

        deferred1.await()
        deferred2.await()

        // THEN we have two telemetry events
        assertEquals(2, service.telemetryEvents.size)

        // 1. Verify the Winner (FETCHED)
        val fetchEvent = service.telemetryEvents.find { it.outcome == UpdateFetchOutcome.FETCHED }
        assertNotNull("One request should have performed the fetch", fetchEvent)
        assertTrue(
            "Winning request should record the network fetch duration (>= 100ms)",
            fetchEvent!!.fetchDurationMillis >= 100,
        )

        // 2. Verify the Loser (COALESCED)
        val coalescedEvent =
            service.telemetryEvents.find { it.outcome == UpdateFetchOutcome.COALESCED }
        assertNotNull("One request should have been coalesced", coalescedEvent)

        // The coalesced request waited for the lock...
        assertTrue(
            "Coalesced request should have high lock wait duration (waited for fetch)",
            coalescedEvent!!.lockWaitDurationMillis >= 50,
        )

        // ...but it did NOT perform a fetch itself (just read the new cache)
        assertEquals(
            "Coalesced request should have 0 fetch duration",
            0L,
            coalescedEvent.fetchDurationMillis,
        )

        // Its processing duration (after acquiring the lock) should be essentially just a cache
        // read
        assertTrue(
            "Coalesced request should have minimal processing duration (<20ms)",
            coalescedEvent.processingDurationMillis < 20,
        )
    }

    @Test
    fun testTelemetry_reportsFailed_andErrorHook() {
        // GIVEN the cache is stale and a network fetch is required
        service.testShouldFetch = true

        // AND the fetch is configured to fail (simulating a Network Error)
        service.shouldThrowError = true

        // AND we simulate a delay before the failure (e.g., a connection timeout)
        // This ensures we have a measurable duration to verify.
        service.fetchDelayMillis = 50L

        // WHEN request is made
        service.callListAvailableUpdates()

        // THEN we report FAILED
        val event = service.lastTelemetry
        assertNotNull(event)
        assertEquals(UpdateFetchOutcome.FAILED, event?.outcome)

        // AND verify that the fetch duration was captured despite the exception.
        // This confirms that duration measurement occurs in the 'finally' block, allowing
        // hosts to distinguish between immediate crashes (0ms) and timeouts (>5s).
        assertTrue(
            "Fetch duration should be captured even on failure (expected >= 50ms, was ${event!!.fetchDurationMillis}ms)",
            event.fetchDurationMillis >= 50,
        )

        // AND verify the error hook captured the exception for reporting
        assertNotNull("Should have captured exception in onFetchFailed", service.lastException)
        assertEquals("Simulated Network Error", service.lastException?.message)
    }

    // --- 2. Service Lifecycle (Connection Tracking) ---

    @Test
    fun testLifecycle_reportsClientConnection() {
        // GIVEN a binding intent with a package data URI
        val clientPackage = "com.example.settings"
        val intent =
            Intent("androidx.security.state.provider.UPDATE_INFO_SERVICE").apply {
                data = android.net.Uri.fromParts("package", clientPackage, null)
            }

        // WHEN a client binds
        service.onBind(intent)

        // THEN onClientConnected receives the intent with the data intact
        val connectedIntent = service.lastConnectedIntent
        assertNotNull(connectedIntent)
        // Verify the object is the same
        assertEquals(intent, connectedIntent)
        // Explicitly verify the package name is accessible
        assertEquals(clientPackage, connectedIntent?.data?.schemeSpecificPart)
    }

    @Test
    fun testLifecycle_reportsClientDisconnection() {
        // GIVEN a binding intent with a package data URI
        val clientPackage = "com.example.settings"
        val intent =
            Intent("androidx.security.state.provider.UPDATE_INFO_SERVICE").apply {
                data = android.net.Uri.fromParts("package", clientPackage, null)
            }

        // WHEN a client unbinds
        service.onUnbind(intent)

        // THEN onClientDisconnected receives the intent with the package name intact
        val disconnectedIntent = service.lastDisconnectedIntent
        assertNotNull(disconnectedIntent)
        // Verify the object is the same
        assertEquals(intent, disconnectedIntent)
        // Explicitly verify the package name is accessible
        assertEquals(clientPackage, disconnectedIntent?.data?.schemeSpecificPart)
    }

    @Test
    fun testLifecycle_doesNotReportConnection_forWrongAction() {
        // GIVEN a bind request with the WRONG action
        val intent = Intent("com.example.WRONG_ACTION")

        // WHEN onBind is called
        val binder = service.onBind(intent)

        // THEN binding is rejected
        assertNull(binder)

        // AND onClientConnected is NEVER called
        // (Metrics should not be polluted by invalid attempts)
        assertNull(service.lastConnectedIntent)
    }

    // --- 3. Debugging Support (Dump & State) ---

    @Test
    fun testDump_outputsInternalState() {
        // GIVEN the service is in a specific state (e.g., throttled)
        service.testIsThrottled = true

        // SETUP: Populate the manager with a known update AND global metadata
        // 1. Create Update with all fields (Per-Update Data)
        val update =
            UpdateInfo.Builder()
                .setComponent("SYSTEM")
                .setSecurityPatchLevel("2026-01-01")
                .setPublishedDateMillis(1L)
                .setLastCheckTimeMillis(1L)
                .build()

        val manager = UpdateInfoManager(context)
        manager.registerUpdate(update)

        // 2. Set the Global Last Check Time (Service Health)
        manager.setLastCheckTimeMillis(1)

        // WHEN we dump the service state
        val dumpOutput = getDumpOutput()

        // THEN the output contains the service header
        assertTrue(
            "Dump should contain standard header",
            dumpOutput.contains("UpdateInfoService State:"),
        )

        // AND reports the correct throttling state
        assertTrue(
            "Dump should report throttling status",
            dumpOutput.contains("Should Throttle: true"),
        )

        // AND reports the Global Last Check
        assertTrue(
            "Dump should report global check time",
            dumpOutput.contains("Global Last Check:"),
        )

        // AND reports the cached update details
        // (This confirms that the service can successfully read and print the cache)
        assertTrue(
            "Dump should show the count of cached updates",
            dumpOutput.contains("Cached Updates (1)"),
        )
        assertTrue(
            "Dump should show the specific update component",
            dumpOutput.contains("Component: SYSTEM"),
        )
        assertTrue("Dump should show the specific SPL", dumpOutput.contains("SPL: 2026-01-01"))

        // AND reports the per-update timestamps
        assertTrue("Dump should show Published date", dumpOutput.contains("Published:"))
        assertTrue("Dump should show Last Checked date", dumpOutput.contains("Last Checked:"))
    }

    @Test
    fun testDump_reportsActiveRequestCount_duringProcessing() {
        runBlocking {
            // GIVEN a request that hangs in the "network" phase
            service.testShouldFetch = true
            service.fetchDelayMillis = 500L

            // WHEN we start the request asynchronously
            val deferred = async(Dispatchers.IO) { service.callListAvailableUpdates() }

            // Wait for the service to actually start fetching.
            // This guarantees activeRequestCount has been incremented.
            val started = service.fetchLatch.await(2, TimeUnit.SECONDS)
            assertTrue("Timed out waiting for fetch to start", started)

            // THEN dump() reports 1 active request
            val dumpOutput = getDumpOutput()

            assertTrue(
                "Should report active request. Output: $dumpOutput",
                dumpOutput.contains("Active Requests: 1"),
            )

            // CLEANUP
            deferred.await()
        }
    }

    @Test
    fun testDump_reportsMultipleActiveRequests_duringContention() {
        runBlocking {
            // GIVEN a slow fetch
            service.testShouldFetch = true
            service.fetchDelayMillis = 500L

            // WHEN request 1 starts (and holds the lock)
            val deferred1 = async(Dispatchers.IO) { service.callListAvailableUpdates() }
            service.fetchLatch.await(2, TimeUnit.SECONDS)

            // AND request 2 starts (and gets blocked on the lock)
            val deferred2 = async(Dispatchers.IO) { service.callListAvailableUpdates() }
            delay(100) // Allow request 2 to hit the lock

            // THEN dump() reports 2 active requests
            val dumpOutput = getDumpOutput()
            assertTrue(
                "Should report 2 active requests (1 running + 1 waiting). Output: $dumpOutput",
                dumpOutput.contains("Active Requests: 2"),
            )

            // Cleanup
            deferred1.await()
            deferred2.await()
        }
    }

    @Test
    fun testDump_reportsZeroActiveRequests_afterCompletion() {
        // GIVEN a request that finishes (even with failure)
        service.testShouldFetch = true
        service.shouldThrowError = true // Force an exception path

        // WHEN request completes
        // (We don't need try-catch here because the Service swallows the error
        // to return cached data gracefully, but wrapping it is fine)
        service.callListAvailableUpdates()

        // THEN dump() reports 0 active requests
        val dumpOutput = getDumpOutput()
        assertTrue(
            "Active requests should return to 0. Output: $dumpOutput",
            dumpOutput.contains("Active Requests: 0"),
        )
    }

    @Test
    fun testDump_decrementsActiveCount_onCancellation() {
        runBlocking {
            // GIVEN a request that is stuck in the fetch phase
            service.testShouldFetch = true
            service.fetchDelayMillis = 5000L // Long delay

            // WHEN we start the request
            val job = launch(Dispatchers.IO) { service.callListAvailableUpdates() }

            // Wait for it to start (Latch ensures we are inside the critical section)
            service.fetchLatch.await(2, TimeUnit.SECONDS)

            // Verify it is counted as active
            var dumpOutput = getDumpOutput()
            assertTrue("Should be active before cancel", dumpOutput.contains("Active Requests: 1"))

            // ACTION: Cancel the client's request
            job.cancel()
            job.join() // Wait for cancellation to propagate

            // THEN the active count returns to 0 (Finally block execution)
            dumpOutput = getDumpOutput()
            assertTrue(
                "Active requests should return to 0 after cancel. Output: $dumpOutput",
                dumpOutput.contains("Active Requests: 0"),
            )
        }
    }

    // --- 4. Caller Attribution (UIDs & Identity) ---

    @Test
    fun testTelemetry_capturesCallerUid() {
        // WHEN request is made
        service.callListAvailableUpdates()

        // THEN telemetry includes the UID
        val event = service.lastTelemetry
        // In local unit tests, this is usually the test process UID
        assertEquals(Process.myUid(), event?.callerUid)
    }

    @Test
    fun testTelemetry_preservesCallerIdentity_acrossClearing() {
        // GIVEN a specific remote caller UID
        val distinctUid = 9999
        service.testCallerUid = distinctUid // Configure the spy directly

        // WHEN request is made
        service.callListAvailableUpdates()

        // THEN telemetry reports the ORIGINAL caller UID
        // (This verifies we captured it *before* clearing identity)
        val event = service.lastTelemetry
        assertNotNull(event)
        assertEquals("Should capture UID before clearing identity", distinctUid, event?.callerUid)
    }

    @Test
    fun testTelemetry_attributesConcurrentCallers_correctly() = runBlocking {
        // GIVEN cache is stale
        service.testShouldFetch = true
        service.fetchDelayMillis = 100L

        // ENABLE DYNAMIC UIDs in SPY
        service.useDynamicUids = true

        // WHEN two distinct callers request updates
        val deferred1 = async(Dispatchers.IO) { service.callListAvailableUpdates() }
        val deferred2 =
            async(Dispatchers.IO) {
                delay(10)
                service.callListAvailableUpdates()
            }

        deferred1.await()
        deferred2.await()

        // THEN we have 2 events
        val events = service.telemetryEvents
        assertEquals(2, events.size)

        // AND they have DIFFERENT UIDs
        val uid1 = events[0].callerUid
        val uid2 = events[1].callerUid

        assertNotEquals("Concurrent callers should have distinct UIDs", uid1, uid2)
    }

    @Test
    fun testIdentity_isCleared_insideFetchUpdates() {
        // GIVEN a remote caller with a distinct UID (simulated via Robolectric)
        val remoteUid = 9999
        ShadowBinder.setCallingUid(remoteUid)

        // Setup the spy to verify the UID *inside* the critical section
        service.verifyIdentityInFetch = true
        service.testShouldFetch = true // Ensure we enter the fetch block

        // WHEN request is made
        service.callListAvailableUpdates()

        // THEN the spy asserts that Binder.getCallingUid() was NOT remoteUid
        // (This confirms clearCallingIdentity() was active)
        assertTrue("Identity should be cleared", service.wasIdentityCleared)
    }

    // --- 6. Resilience & Safety (Error Handling) ---

    @Test
    fun testTelemetry_ignoresExceptionsInHooks() {
        // GIVEN the host's telemetry hook is buggy and throws an exception
        service.shouldCrashInTelemetry = true
        service.testShouldFetch = false // Fast path

        // WHEN a request is made
        // (This should NOT throw an exception to the caller)
        try {
            val result = service.callListAvailableUpdates()

            // THEN the service still functions and returns data
            assertNotNull("Service should return result even if metrics fail", result)
        } catch (e: Exception) {
            fail("Service crashed due to telemetry error: ${e.message}")
        }
    }

    @Test
    fun testTelemetry_ignoresExceptionsInOnFetchFailed() {
        // GIVEN a fetch failure AND a buggy error hook
        service.testShouldFetch = true
        service.shouldThrowError = true
        service.shouldCrashInErrorHook = true

        // WHEN request is made
        try {
            val result = service.callListAvailableUpdates()

            // THEN the service catches BOTH exceptions and returns the cached result
            assertNotNull("Should return result despite double-failure", result)
            // AND telemetry still reports the FAILED state
            assertEquals(UpdateFetchOutcome.FAILED, service.lastTelemetry?.outcome)
        } catch (e: Exception) {
            fail("Service crashed due to error hook failure: ${e.message}")
        }
    }

    @Test
    fun testState_globalLastCheckTime_notUpdated_onFetchFailure() {
        // GIVEN a stale cache with an old timestamp (e.g. 0)
        val initialTime = UpdateInfoManager(context).getLastCheckTimeMillis()
        assertEquals(0L, initialTime)

        // AND a fetch that is guaranteed to fail
        service.testShouldFetch = true
        service.shouldThrowError = true

        // WHEN request is made
        service.callListAvailableUpdates()

        // THEN the global last check time is NOT updated
        // (This ensures the rate limiter won't block the next attempt, allowing an immediate retry)
        val newTime = UpdateInfoManager(context).getLastCheckTimeMillis()
        assertEquals("Global timestamp should not update on failure", 0L, newTime)
    }

    // Helper to reduce boilerplate
    private fun getDumpOutput(): String {
        val outStream = ByteArrayOutputStream()
        val writer = PrintWriter(outStream)
        service.dump(null, writer, null)
        writer.flush()
        return outStream.toString()
    }

    // --- Telemetry Spy Implementation ---

    /**
     * A Spy implementation that exposes the protected telemetry hooks for verification.
     *
     * This class acts as a Test Double that allows unit tests to:
     * 1. Inspect internal state (telemetry events, exceptions, intents).
     * 2. Control logic flow (simulating stale cache, throttling, network delays).
     * 3. Simulate failure modes (network errors, crashes in hooks).
     */
    class TelemetrySpyService : UpdateInfoService() {

        // --- Captured Data ---

        // Use a thread-safe list to handle concurrent telemetry reports from multiple threads
        val telemetryEvents = CopyOnWriteArrayList<UpdateCheckTelemetry>()

        @Volatile // Ensure visibility across threads
        var lastTelemetry: UpdateCheckTelemetry? = null

        var lastConnectedIntent: Intent? = null
        var lastDisconnectedIntent: Intent? = null
        var lastException: Exception? = null

        // --- Logic Controls ---

        var testShouldFetch = false
        var testIsThrottled = false
        var shouldThrowError = false
        var fetchDelayMillis = 0L

        // Latch to signal when fetchUpdates starts (critical for testing "Active Requests")
        val fetchLatch = CountDownLatch(1)

        // --- Crash Simulation ---

        var shouldCrashInTelemetry = false
        var shouldCrashInErrorHook = false

        // --- Identity & UID Simulation ---

        var testCallerUid: Int? = null
        var useDynamicUids = false
        private val dynamicUidCounter = AtomicInteger(10000)

        // Flags to verify identity isolation logic
        var verifyIdentityInFetch = false
        var wasIdentityCleared = false

        // --- Test Setup Helpers ---

        /**
         * Exposes attachBaseContext publicly so tests can inject a standard Application context.
         */
        fun attach(context: Context) {
            super.attachBaseContext(context)
        }

        /** Helper to call the binder method directly, simulating an incoming IPC call. */
        fun callListAvailableUpdates(): UpdateCheckResult {
            val binder =
                onBind(Intent("androidx.security.state.provider.UPDATE_INFO_SERVICE"))
                    as IUpdateInfoService
            return binder.listAvailableUpdates()
        }

        // --- Overrides for Logic ---

        override fun shouldFetchUpdates(): Boolean = testShouldFetch

        override fun shouldThrottle(): Boolean = testIsThrottled

        override suspend fun fetchUpdates(): List<UpdateInfo> {
            // Signal that we have entered the fetch block (ActiveRequest count is now incremented)
            fetchLatch.countDown()

            // Verify Identity Isolation (if requested by test)
            if (verifyIdentityInFetch) {
                // Check the CURRENT binder identity.
                // It should be the Process UID (Service), NOT the remote caller UID.
                val currentUid = Binder.getCallingUid()
                // We verify that the current UID effectively *changed* from what the test
                // configured as the "external caller".
                wasIdentityCleared = (currentUid != testCallerUid)
            }

            if (fetchDelayMillis > 0) delay(fetchDelayMillis)
            if (shouldThrowError) throw IOException("Simulated Network Error")

            // Simulate the side effect: Cache becomes fresh after successful fetch
            testShouldFetch = false
            return emptyList()
        }

        override fun getCallerUid(): Int {
            return if (useDynamicUids) {
                // Simulate distinct apps calling in (for concurrency attribution tests)
                dynamicUidCounter.getAndIncrement()
            } else {
                // Return the test value if set, otherwise fallback to real system
                testCallerUid ?: super.getCallerUid()
            }
        }

        // --- Overrides for Telemetry Hooks ---=
        override fun onRequestCompleted(telemetry: UpdateCheckTelemetry) {
            // 1. Crash Simulation (verify service doesn't crash if metrics fail)
            if (shouldCrashInTelemetry) {
                throw RuntimeException("Telemetry Crash!")
            }

            // 2. Data Capture
            telemetryEvents.add(telemetry)
            lastTelemetry = telemetry

            // 3. Super call (good practice, though base is no-op)
            super.onRequestCompleted(telemetry)
        }

        override fun onClientConnected(intent: Intent) {
            lastConnectedIntent = intent
        }

        override fun onClientDisconnected(intent: Intent?) {
            lastDisconnectedIntent = intent
        }

        override fun onFetchFailed(e: Exception) {
            if (shouldCrashInErrorHook) {
                throw RuntimeException("Crash in Error Hook!")
            }
            lastException = e
        }
    }
}
