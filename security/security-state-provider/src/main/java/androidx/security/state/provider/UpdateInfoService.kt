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

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.security.state.IUpdateInfoService
import androidx.security.state.UpdateCheckResult
import androidx.security.state.UpdateInfo
import java.io.FileDescriptor
import java.io.PrintWriter
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Base class for implementing the AndroidX Security State Update Provider service.
 *
 * This abstract class provides the foundational implementation for the `IUpdateInfoService` AIDL
 * interface. It manages the complexity of serving security update information to client
 * applications, enforcing a consistent contract for data freshness, resource usage, and
 * observability.
 *
 * ### Responsibilities
 * * **IPC Handling:** Manages the AIDL binding process and verifies Intent actions.
 * * **Concurrency Control:** Implements Double-Checked Locking to prevent "thundering herd"
 *   scenarios where multiple clients trigger parallel network requests.
 * * **Rate Limiting:** Enforces a default throttling policy (e.g., maximum one check per hour) to
 *   protect backend infrastructure.
 * * **Caching:** Automatically persists fetched results and freshness metadata to local storage.
 *
 * ### Monitoring & Telemetry
 * To support high-scale hosts that require strict monitoring, this class provides a comprehensive
 * set of observability hooks. Hosts can override these methods to integrate with their own logging
 * infrastructure:
 * * [onRequestCompleted]: Report granular performance metrics (latency, lock contention) and usage.
 * * [onClientConnected] / [onClientDisconnected]: Track service session lifecycles and adoption.
 * * [onFetchFailed]: Report backend errors and exceptions.
 * * [dump]: Debug internal state via `adb shell`.
 *
 * ### Host Implementation
 * Host applications (such as the System Updater, Google Play Store, or OEM-specific updaters) must
 * extend this class and implement the [fetchUpdates] method to provide the actual network logic.
 *
 * ### Manifest Declaration
 * To expose this service, you must declare it in your `AndroidManifest.xml` with the
 * `exported="true"` attribute and an intent-filter for the `UPDATE_INFO_SERVICE` action:
 * ```xml
 * <service
 *     android:name=".MyUpdateInfoService"
 *     android:exported="true">
 *     <intent-filter>
 *         <action android:name="androidx.security.state.provider.UPDATE_INFO_SERVICE" />
 *     </intent-filter>
 * </service>
 * ```
 */
public abstract class UpdateInfoService : Service() {

    private companion object {
        private const val TAG = "UpdateInfoService"
        private const val ACTION_BIND = "androidx.security.state.provider.UPDATE_INFO_SERVICE"
    }

    /** Lazy-initialized manager for accessing persistent update information. */
    private val updateInfoManager by lazy { UpdateInfoManager(this) }

    /**
     * Rate limiter to prevent abuse of the backend. Defaults to
     * [SharedPreferencesUpdateCheckRateLimiter].
     */
    private val checkRateLimiter: UpdateCheckRateLimiter by lazy {
        SharedPreferencesUpdateCheckRateLimiter(this)
    }

    /**
     * Mutex to serialize network requests across concurrent binder threads. This ensures that if
     * multiple clients bind and request updates simultaneously, only one network request is
     * executed.
     */
    private val networkLock = Mutex()

    /**
     * Gauge for currently active requests (concurrency level). Used for debugging stuck threads via
     * [dump].
     */
    private val activeRequestCount = AtomicInteger(0)

    /** The implementation of the AIDL interface. */
    private val binder: IUpdateInfoService.Stub =
        object : IUpdateInfoService.Stub() {
            override fun listAvailableUpdates(): UpdateCheckResult {
                // AIDL calls are blocking by default. We use runBlocking to bridge
                // to our suspendable internal logic, allowing us to use Coroutines
                // for the network fetch and locking.
                return runBlocking { handleListUpdates() }
            }
        }

    /**
     * Internal orchestrator for handling the update check request.
     *
     * This implements the "Double-Checked Locking" pattern:
     * 1. Check if cache is fresh (Fast path, no lock).
     * 2. Acquire lock.
     * 3. Check if cache is fresh again (Double-check).
     * 4. Check rate limits.
     * 5. Fetch from network (with Identity Isolation).
     */
    private suspend fun handleListUpdates(): UpdateCheckResult {
        // Use monotonic time for duration measurements
        val startTime = SystemClock.elapsedRealtime()

        // 1. Capture Caller Identity (Critical for Telemetry)
        // Must be captured before clearing identity or suspending.
        val callerUid = getCallerUid()

        activeRequestCount.incrementAndGet()

        var lockWaitDuration = 0L
        var executionStartTime = 0L
        var fetchDuration = 0L
        var resultType = UpdateFetchOutcome.FAILED

        try {
            // 2. Fast Path: Check policy without acquiring lock
            if (!shouldFetchUpdates()) {
                resultType = UpdateFetchOutcome.CACHE_HIT
                executionStartTime = SystemClock.elapsedRealtime() // Virtually 0 execution time
                return getCachedResult()
            }

            // 3. Slow Path: Acquire lock to prevent parallel network requests
            val lockStart = SystemClock.elapsedRealtime()
            networkLock.withLock {
                lockWaitDuration = SystemClock.elapsedRealtime() - lockStart
                executionStartTime = SystemClock.elapsedRealtime()

                // 4. Double-Check: Re-verify policy inside the lock
                // Another thread might have updated the cache while we were waiting.
                if (!shouldFetchUpdates()) {
                    resultType = UpdateFetchOutcome.COALESCED
                    return getCachedResult()
                }

                // 5. Rate Limit Check: Protect the backend
                if (shouldThrottle()) {
                    resultType = UpdateFetchOutcome.THROTTLED
                    Log.i(TAG, "Update check skipped due to rate limiting.")
                    return getCachedResult()
                }

                // 6. Identity Management (Critical for Service Logic)
                // Elevate permissions to the Service's identity to prevent "Permission Denied"
                // errors if the caller is unprivileged.
                val token = Binder.clearCallingIdentity()

                // Initialize start time outside try block to correctly handle exceptions
                var fetchStart = 0L

                try {
                    // 7. Record Attempt & Fetch
                    // We record the attempt *before* the network call to ensure we count it
                    // against the rate limit, even if the fetch times out or crashes.
                    checkRateLimiter.noteAttempt()

                    // BLOCKING: Call the host's implementation to perform the network request.
                    // We measure strictly the duration of the network call for telemetry.
                    fetchStart = SystemClock.elapsedRealtime()
                    val newUpdates = fetchUpdates()

                    // Success: Update cache and timestamp
                    // Note: Persistence time is part of processingDuration but excluded from
                    // fetchDuration
                    newUpdates.forEach { update -> updateInfoManager.registerUpdate(update) }
                    // Use Wall Clock time for persistence (needs to be comparable across reboots)
                    updateInfoManager.setLastCheckTimeMillis(System.currentTimeMillis())

                    resultType = UpdateFetchOutcome.FETCHED
                } finally {
                    if (fetchStart > 0) {
                        fetchDuration = SystemClock.elapsedRealtime() - fetchStart
                    }
                    Binder.restoreCallingIdentity(token)
                }

                return getCachedResult()
            }
        } catch (e: Exception) {
            resultType = UpdateFetchOutcome.FAILED
            safeOnFetchFailed(e)
            return getCachedResult()
        } finally {
            activeRequestCount.decrementAndGet()
            val endTime = SystemClock.elapsedRealtime()

            // Calculate granular breakdown (using monotonic time)
            val totalDuration = endTime - startTime
            val processingDuration =
                if (executionStartTime > 0) endTime - executionStartTime else totalDuration

            // Report granular metrics
            val telemetry =
                UpdateCheckTelemetry(
                    outcome = resultType,
                    totalDurationMillis = totalDuration,
                    lockWaitDurationMillis = lockWaitDuration,
                    processingDurationMillis = processingDuration,
                    fetchDurationMillis = fetchDuration,
                    callerUid = callerUid,
                )
            safeOnRequestCompleted(telemetry)
        }
    }

    /**
     * Called by the system when a client binds to the particular interface defined by the [intent].
     *
     * This method verifies that the Intent action matches the expected contract ([ACTION_BIND]). If
     * the action is missing or incorrect, the binding is rejected to ensure the service is not
     * exposed unintentionally.
     *
     * **Lifecycle Note:** Because the client library attaches unique data (the package name) to the
     * [intent], this method is called for **every** new client session, allowing the
     * [onClientConnected] hook to reliably track individual connections.
     *
     * @param intent The Intent that was used to bind to this service.
     * @return The IBinder interface for clients to interact with the service, or `null` if the
     *   Intent action is invalid.
     */
    final override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action == ACTION_BIND) {
            onClientConnected(intent)
            return binder
        }
        Log.w(TAG, "Rejected binding with unexpected action: ${intent?.action}")
        return null
    }

    /**
     * Called by the system when all clients have disconnected from the particular interface defined
     * by the [intent].
     *
     * This method is marked `final` to enforce the library's lifecycle contract. It performs two
     * critical actions:
     * 1. Triggers the [onClientDisconnected] hook, allowing the host to log the end of the session.
     * 2. Returns `false`, which forces the Android system to call [onBind] (and thus
     *    [onClientConnected]) for all future connections. This simplifies telemetry by ensuring a
     *    consistent "Connect -> Disconnect" cycle without needing to handle the `onRebind` edge
     *    case.
     *
     * @param intent The Intent that was used to bind to this service.
     * @return `false` to ensure [onRebind] is not called.
     */
    final override fun onUnbind(intent: Intent?): Boolean {
        onClientDisconnected(intent)
        return false
    }

    /**
     * Called when a client binds to the particular interface defined by the [intent].
     *
     * This method is invoked during [onBind] after the Intent action has been verified.
     *
     * **Usage:** Override this method to log connection metrics (e.g., "Session Started") or track
     * adoption trends.
     *
     * **Client Identity:** The `intent` passed to this method typically contains the client's
     * package name in the data URI (e.g., `package:com.example.client`). This allows you to
     * identify which app is starting a session. Note that this value is self-reported by the client
     * library and is useful for aggregate trends but less secure than the
     * [UpdateCheckTelemetry.callerUid] captured in [onRequestCompleted].
     *
     * **Lifecycle:** Because the client library attaches unique data to the bind Intent, this
     * method is called for **every** new client session, enabling accurate session counting.
     *
     * @param intent The Intent that was used to bind to this service.
     */
    protected open fun onClientConnected(intent: Intent) {}

    /**
     * Called when all clients have disconnected from the particular interface defined by the
     * [intent].
     *
     * This method is invoked during [onUnbind].
     *
     * **Usage:** Override this method to log session ends (e.g., "Session Ended") to calculate
     * session duration when paired with [onClientConnected]. You can also use this to perform
     * per-client resource cleanup.
     *
     * @param intent The Intent that was used to bind to this service. It contains the same
     *   identification data as passed to [onClientConnected].
     */
    protected open fun onClientDisconnected(intent: Intent?) {}

    /**
     * Called when a request completes. Override this to log telemetry.
     *
     * This hook is called for **every** request to `listAvailableUpdates`, regardless of whether it
     * succeeded, failed, was throttled, or was served from the cache.
     *
     * **Usage:** Use this hook to log granular performance metrics (latency histograms) and
     * resource usage.
     *
     * **Attribution:** The [telemetry] object contains the [UpdateCheckTelemetry.callerUid], which
     * is the Kernel-verified UID of the calling process. This is the authoritative source for
     * attributing load (CPU/Network/Lock Contention) to specific client applications.
     *
     * @param telemetry The [UpdateCheckTelemetry] containing metrics and outcomes.
     */
    protected open fun onRequestCompleted(telemetry: UpdateCheckTelemetry) {}

    /**
     * Performs the actual network request to fetch fresh updates from the backend.
     *
     * Implementations of this method should:
     * * Block until the network request completes.
     * * Return the list of updates found.
     * * Throw an exception if the network request fails (this will be caught and logged).
     *
     * **Note:** This method is only called if [shouldFetchUpdates] returns `true` and the rate
     * limiter allows the request.
     *
     * @return A list of [UpdateInfo] objects currently available for the device.
     */
    protected abstract suspend fun fetchUpdates(): List<UpdateInfo>

    /**
     * Determines if the local cache is stale and a refresh should be attempted.
     *
     * The default implementation returns `true` if the data is older than 1 hour. Hosts can
     * override this to implement custom caching policies (e.g., 4 hours, 24 hours).
     *
     * @return `true` if a network fetch should be attempted.
     */
    protected open fun shouldFetchUpdates(): Boolean {
        val lastCheckTimeMillis = updateInfoManager.getLastCheckTimeMillis()
        val dataAge = System.currentTimeMillis() - lastCheckTimeMillis
        return dataAge > TimeUnit.HOURS.toMillis(1)
    }

    /**
     * Checks if the operation should be throttled based on the rate limiter.
     *
     * The default implementation delegates to an internal rate limiter that enforces a cooling-off
     * period (e.g., one check per hour). Hosts can override this to inject custom rate limiting
     * logic (e.g., battery checks).
     *
     * @return `true` if the request should be blocked.
     */
    protected open fun shouldThrottle(): Boolean {
        return checkRateLimiter.shouldThrottle()
    }

    /**
     * Callback for handling exceptions that occur during the update check process.
     *
     * This method is invoked if any step of the update logic fails, including:
     * * Determining cache freshness ([shouldFetchUpdates]).
     * * Recording rate-limit attempts.
     * * Fetching data from the backend ([fetchUpdates]).
     * * Persisting the results to local storage.
     *
     * The default implementation logs the error to Logcat. Hosts can override this to report errors
     * to their own telemetry or crash reporting systems.
     *
     * @param e The exception thrown during the operation.
     */
    protected open fun onFetchFailed(e: Exception) {
        Log.w(TAG, "Failed to fetch updates", e)
    }

    /**
     * Retrieves the UID of the calling process.
     *
     * The default implementation delegates to [android.os.Binder.getCallingUid]. Host
     * implementations using a proxy or broker architecture should override this to return the
     * logical client UID instead of the broker's UID, ensuring correct attribution.
     */
    protected open fun getCallerUid(): Int {
        return Binder.getCallingUid()
    }

    /**
     * Internal wrapper to invoke [onRequestCompleted] safely.
     *
     * This acts as a **firewall** between the library's core logic and the host's telemetry
     * implementation. If the host code throws an exception (e.g., due to a bug in their analytics
     * logger), this wrapper catches it to ensure the service remains stable and returns the result
     * to the client.
     */
    private fun safeOnRequestCompleted(telemetry: UpdateCheckTelemetry) {
        try {
            onRequestCompleted(telemetry)
        } catch (e: Exception) {
            Log.e(TAG, "Telemetry error", e)
        }
    }

    /**
     * Internal wrapper to invoke [onFetchFailed] safely.
     *
     * This acts as a **firewall** between the library's core logic and the host's error reporting
     * implementation. If the host's crash reporter fails (e.g., throws an exception during
     * initialization), this wrapper catches it to ensure the original error is still logged to
     * Logcat and the service continues to degrade gracefully (returning cached data).
     */
    private fun safeOnFetchFailed(e: Exception) {
        try {
            onFetchFailed(e)
        } catch (loggingEx: Exception) {
            Log.e(TAG, "Error in onFetchFailed hook", loggingEx)
        }
    }

    /** Helper to construct the result from the current persistence layer. */
    private fun getCachedResult(): UpdateCheckResult {
        return UpdateCheckResult(
            providerPackageName = packageName,
            updates = updateInfoManager.getAllUpdates(),
            lastCheckTimeMillis = updateInfoManager.getLastCheckTimeMillis(),
        )
    }

    /**
     * Dumps the state of the service for debugging (e.g. `adb shell dumpsys activity service`).
     *
     * This implementation provides a snapshot of the internal state, including:
     * * **Active Request Count:** To detect stuck threads or high concurrency.
     * * **Global Last Check Time:** The timestamp of the last successful service-wide sync (Service
     *   Health).
     * * **Throttling Status:** Whether the rate limiter is currently blocking network requests.
     * * **Cached Updates:** A complete list of stored updates with detailed metadata (Component,
     *   SPL, Provider, Published Date, Last Checked Time).
     */
    public override fun dump(fd: FileDescriptor?, writer: PrintWriter?, args: Array<out String>?) {
        val pw = writer ?: return

        pw.println("UpdateInfoService State:")
        pw.println("  Active Requests: ${activeRequestCount.get()}")
        pw.println("  Global Last Check: ${Date(updateInfoManager.getLastCheckTimeMillis())}")
        pw.println("  Should Throttle: ${shouldThrottle()}")

        val updates = updateInfoManager.getAllUpdates()
        pw.println("  Cached Updates (${updates.size}):")

        if (updates.isEmpty()) {
            pw.println("    (None)")
        } else {
            updates.forEach { update ->
                pw.println("    - Component: ${update.component}")
                pw.println("      SPL: ${update.securityPatchLevel}")
                pw.println("      Published: ${Date(update.publishedDateMillis)}")
                pw.println("      Last Checked: ${Date(update.lastCheckTimeMillis)}")
            }
        }
    }
}
