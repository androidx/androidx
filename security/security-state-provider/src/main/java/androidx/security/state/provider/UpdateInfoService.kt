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

import android.app.AppOpsManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.RemoteException
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.security.state.IUpdateInfoService
import androidx.security.state.IUpdateInfoSession
import androidx.security.state.UpdateCheckResult
import androidx.security.state.UpdateInfo
import java.io.FileDescriptor
import java.io.PrintWriter
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
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
 * * **Session Management:** Implements a Factory pattern to create secure, stateful sessions
 *   (`IUpdateInfoSession`) for each connecting client.
 * * **Identity Verification:** Validates client-provided package names against their Kernel-level
 *   UIDs to prevent Intent spoofing and guarantee secure attribution. To successfully perform this
 *   validation, the host application must have broad package visibility (e.g., via the
 *   `QUERY_ALL_PACKAGES` permission or by running as the system user).
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
 * * [onClientConnected] / [onClientDisconnected]: Track authenticated session lifecycles and
 *   adoption.
 * * [onFetchFailed]: Report backend errors and exceptions.
 * * [dump]: Debug internal state via `adb shell dumpsys`.
 *
 * ### Host Implementation
 * Host applications (such as the System Updater, Google Play Store, or OEM-specific updaters)
 * written in Kotlin should extend this class and implement [fetchUpdates] to provide the actual
 * network logic. Java consumers should instead extend [ListenableFutureUpdateInfoService].
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

    /**
     * The Factory implementation for establishing secure update info sessions.
     *
     * This binder is returned to the Android system when a client initially binds to the service
     * via [onBind]. It acts as a gateway, forcing the client to explicitly request a session and
     * prove its identity before it can query any update data.
     *
     * By using this Session Pattern, we guarantee that the calling UID is captured synchronously on
     * the Binder thread, avoiding the security risks associated with losing IPC context during
     * coroutine suspension.
     */
    private val factoryBinder =
        object : IUpdateInfoService.Stub() {
            /**
             * Creates a unique, stateful session for the calling client.
             *
             * @param packageName The self-reported package name of the client application.
             * @param clientToken An anonymous Binder token provided by the client, used to register
             *   a death recipient to monitor for process crashes.
             * @return A new [UpdateInfoSession] dedicated to this client.
             * @throws SecurityException If the provided [packageName] does not belong to the
             *   Kernel-verified calling UID, or if the host lacks visibility to the client package.
             */
            override fun openSession(
                packageName: String,
                clientToken: IBinder,
            ): IUpdateInfoSession {
                // 1. Capture Identity SECURELY at the entry point.
                // This runs synchronously on the Binder thread before any coroutine context
                // switching, guaranteeing that we capture the actual IPC caller's UID.
                // (Uses the getCallerUid() wrapper to support Service Broker overrides).
                val callerUid = getCallerUid()

                // 2. Validate the package name to prevent spoofing.
                // We do not trust the string passed by the client blindly. We cross-reference
                // it with the OS to ensure the package name is actually owned by its calling uid.
                // If validation fails, this will throw a SecurityException.
                enforceValidPackageForUid(packageName, callerUid)

                // 3. Notify the host application (Lifecycle Hook).
                // The host receives the fully verified package name and UID, which is safe
                // to use for adoption metrics or access control logging.
                onClientConnected(packageName, callerUid)

                // 4. Return a dedicated, stateful Session object.
                // This session permanently binds the verified UID, package name, and death
                // token to all subsequent requests, ensuring safe telemetry attribution
                // and guaranteed resource cleanup.
                return UpdateInfoSession(packageName, callerUid, clientToken)
            }
        }

    /**
     * A stateful, dedicated session for a specific client to query update information.
     *
     * This class implements the [IUpdateInfoSession] AIDL interface and is instantiated by the
     * [factoryBinder] when a client successfully calls `openSession`.
     *
     * By maintaining the verified identity ([sessionUid] and [sessionPackageName]) as immutable
     * properties, this session guarantees that all subsequent requests are correctly attributed for
     * telemetry and rate limiting, circumventing the risks of losing the IPC context during
     * coroutine suspension.
     *
     * It also implements [IBinder.DeathRecipient] to automatically detect if the client application
     * crashes or is force-stopped, ensuring that the host's disconnection telemetry
     * ([onClientDisconnected]) is always triggered and memory leaks are prevented.
     *
     * @property sessionPackageName The explicitly validated package name of the client.
     * @property sessionUid The Kernel-verified UID of the client.
     * @property clientToken An anonymous Binder token provided by the client, used to monitor the
     *   remote process's lifecycle.
     */
    private inner class UpdateInfoSession(
        val sessionPackageName: String,
        val sessionUid: Int,
        val clientToken: IBinder,
    ) : IUpdateInfoSession.Stub(), IBinder.DeathRecipient {

        private val isClosed = AtomicBoolean(false)

        init {
            try {
                // Register to be notified if the remote client process dies unexpectedly.
                // We link to the clientToken (a proxy Binder) rather than `this` (a local Binder)
                // because local binders cannot die from the perspective of their own process.
                clientToken.linkToDeath(this, 0)
            } catch (e: RemoteException) {
                // The client died before we could even link to it. Clean up immediately.
                binderDied()
            }
        }

        /**
         * Retrieves available updates on behalf of the client.
         *
         * @throws IllegalStateException If the client attempts to call this method after the
         *   session has been closed.
         */
        override fun listAvailableUpdates(): UpdateCheckResult {
            if (isClosed.get()) {
                throw IllegalStateException("Cannot list updates: The UpdateInfoSession is closed.")
            }

            // Bridge the AIDL synchronous call into our Coroutine-based business logic.
            // We pass the securely stored UID so the logic doesn't have to rely on
            // Binder.getCallingUid(), which is unsafe in suspending functions.
            return runBlocking { handleListUpdates(sessionUid) }
        }

        /**
         * Closes the session, unregisters death listeners, and triggers the disconnection hook.
         *
         * This method is idempotent; calling it multiple times will only trigger the cleanup and
         * telemetry hooks once.
         */
        override fun close() {
            if (isClosed.compareAndSet(false, true)) {
                try {
                    // Stop monitoring the client process to prevent kernel resource leaks.
                    clientToken.unlinkToDeath(this, 0)
                } catch (e: NoSuchElementException) {
                    // Ignored: The death recipient was already unlinked or not registered.
                }
                // Notify the host application that the session has ended cleanly,
                // providing the verified identity of the client that disconnected.
                onClientDisconnected(sessionPackageName, sessionUid)
            }
        }

        /** Invoked by the Android OS when the process hosting the client binder has died. */
        override fun binderDied() {
            // The client crashed or was force-stopped.
            // Delegate to close() to perform standard cleanup and trigger telemetry.
            close()
        }
    }

    /**
     * Internal orchestrator for handling the update check request.
     *
     * This method implements the core logic for fulfilling an update check, ensuring thread safety,
     * enforcing rate limits, isolating IPC identity, and recording highly granular performance
     * telemetry.
     *
     * ### Concurrency: Double-Checked Locking
     * To prevent a "thundering herd" scenario where multiple clients trigger simultaneous network
     * requests, this method uses a `Mutex`:
     * 1. **Fast Path:** Checks if the cache is fresh without acquiring the lock.
     * 2. **Acquire Lock:** If stale, suspends until the network lock is available.
     * 3. **Double-Check:** Re-verifies cache freshness inside the lock, as another thread might
     *    have completed the fetch while this thread was waiting.
     *
     * ### Security: Identity Isolation
     * Before invoking the host's [fetchUpdates] implementation, the thread's IPC identity is
     * temporarily cleared. This ensures that any network or disk operations execute with the
     * Service's own privileges, preventing `SecurityException`s if the original client application
     * was unprivileged.
     *
     * @param callerUid The Kernel-verified UID of the client. This is captured securely during
     *   session creation ([IUpdateInfoService.openSession]) and passed explicitly to guarantee
     *   accurate attribution, even if coroutine context switching occurs.
     * @return An [UpdateCheckResult] containing the latest data and metadata.
     */
    private suspend fun handleListUpdates(callerUid: Int): UpdateCheckResult {
        // Use monotonic time for duration measurements to protect against clock changes
        val startTimeMillis = SystemClock.elapsedRealtime()

        activeRequestCount.incrementAndGet()

        var lockWaitDurationMillis = 0L
        var executionStartTimeMillis = 0L
        var fetchDurationMillis = 0L
        var resultType = UpdateFetchOutcome.FAILED

        try {
            // 1. Fast Path: Check policy without acquiring lock
            if (!shouldFetchUpdates()) {
                resultType = UpdateFetchOutcome.CACHE_HIT
                executionStartTimeMillis =
                    SystemClock.elapsedRealtime() // Virtually 0 execution time
                return getCachedResult()
            }

            // 2. Slow Path: Acquire lock to prevent parallel network requests
            val lockStart = SystemClock.elapsedRealtime()
            networkLock.withLock {
                lockWaitDurationMillis = SystemClock.elapsedRealtime() - lockStart
                executionStartTimeMillis = SystemClock.elapsedRealtime()

                // 3. Double-Check: Re-verify policy inside the lock
                // Another thread might have updated the cache while we were waiting.
                if (!shouldFetchUpdates()) {
                    resultType = UpdateFetchOutcome.COALESCED
                    return getCachedResult()
                }

                // 4. Rate Limit Check: Protect the backend
                if (shouldThrottle()) {
                    resultType = UpdateFetchOutcome.THROTTLED
                    Log.i(TAG, "Update check skipped due to rate limiting.")
                    return getCachedResult()
                }

                // 5. Identity Management (Critical for Service Logic)
                // Elevate permissions to the Service's identity to prevent "Permission Denied"
                // errors if the caller is unprivileged.
                val token = Binder.clearCallingIdentity()

                // Initialize start time outside try block to correctly handle exceptions
                var fetchStartMillis = 0L

                try {
                    // 6. Record Attempt & Fetch
                    // We record the attempt *before* the network call to ensure we count it
                    // against the rate limit, even if the fetch times out or crashes.
                    checkRateLimiter.noteAttempt()

                    // BLOCKING: Call the host's implementation to perform the network request.
                    // We measure strictly the duration of the network call for telemetry.
                    fetchStartMillis = SystemClock.elapsedRealtime()
                    val newUpdates = fetchUpdates()

                    // Success: Update cache and timestamp
                    // Note: Persistence time is part of processingDuration but excluded from
                    // fetchDuration
                    newUpdates.forEach { update -> updateInfoManager.registerUpdate(update) }
                    // Use Wall Clock time for persistence (needs to be comparable across reboots)
                    updateInfoManager.setLastCheckTimeMillis(System.currentTimeMillis())

                    resultType = UpdateFetchOutcome.FETCHED
                } finally {
                    if (fetchStartMillis > 0) {
                        fetchDurationMillis = SystemClock.elapsedRealtime() - fetchStartMillis
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
            val endTimeMillis = SystemClock.elapsedRealtime()

            // Calculate granular breakdown (using monotonic time)
            val totalDurationMillis = endTimeMillis - startTimeMillis
            val processingDurationMillis =
                if (executionStartTimeMillis > 0) endTimeMillis - executionStartTimeMillis
                else totalDurationMillis

            // Report granular metrics
            val telemetry =
                UpdateCheckTelemetry(
                    outcome = resultType,
                    totalDurationMillis = totalDurationMillis,
                    lockWaitDurationMillis = lockWaitDurationMillis,
                    processingDurationMillis = processingDurationMillis,
                    fetchDurationMillis = fetchDurationMillis,
                    callerUid = callerUid,
                )
            safeOnRequestCompleted(telemetry)
        }
    }

    /**
     * Called by the system when a client binds to the service.
     *
     * This method verifies that the Intent action matches the expected contract ([ACTION_BIND]). If
     * the action is missing or incorrect, the binding is rejected to ensure the service is not
     * exposed unintentionally.
     *
     * Upon successful verification, this method returns a factory Binder ([IUpdateInfoService])
     * that allows the client to establish a secure, authenticated session.
     *
     * @param intent The Intent that was used to bind to this service.
     * @return The [IUpdateInfoService] factory interface, or `null` if the Intent action is
     *   invalid.
     */
    final override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action == ACTION_BIND) {
            // Note: Connection tracking (onClientConnected) is NOT performed here.
            // It is deferred to openSession() where the client's identity is verified.
            return factoryBinder
        }
        Log.w(TAG, "Rejected binding with unexpected action: ${intent?.action}")
        return null
    }

    /**
     * Called by the system when all clients have disconnected from the service's factory interface.
     *
     * This method is marked `final` to enforce the library's lifecycle contract. It returns
     * `false`, which forces the Android system to call [onBind] for any future connections. This
     * simplifies the service's lifecycle management by ensuring we never need to handle the
     * [android.app.Service.onRebind] edge case.
     *
     * **Lifecycle Note:** Individual client session disconnections and telemetry are tracked when
     * the client calls [IUpdateInfoSession.close] (which triggers [onClientDisconnected]), not
     * within this system-level callback.
     *
     * @param intent The Intent that was used to bind to this service.
     * @return `false` to ensure `onRebind` is not called.
     */
    final override fun onUnbind(intent: Intent?): Boolean {
        return false
    }

    /**
     * Called when a client successfully establishes a session with the update provider.
     *
     * This method is invoked when a client calls [IUpdateInfoService.openSession] and passes the
     * identity validation checks.
     *
     * **Usage:** Override this method to log connection metrics (e.g., "Session Started") or track
     * adoption trends based on the client package name.
     *
     * **Client Identity & Security:** The [packageName] provided to this hook is strictly validated
     * against the Kernel-verified [callerUid] by the Android system. This guarantees that the
     * identity is authentic and cannot be spoofed.
     *
     * **Package Visibility:** The host application must have package visibility (e.g., via the
     * `QUERY_ALL_PACKAGES` permission) to verify the client's package name. If the host cannot
     * verify the package name, the connection will be rejected with a [SecurityException] before
     * this hook is ever called.
     *
     * @param packageName The explicitly verified package name of the client application.
     * @param callerUid The Kernel-verified Linux UID of the client process.
     */
    protected open fun onClientConnected(packageName: String, callerUid: Int) {}

    /**
     * Called when a client explicitly closes its session or its process terminates unexpectedly.
     *
     * This method is invoked when the client calls [IUpdateInfoSession.close] or when the Android
     * system detects that the process hosting the client has died (via
     * [android.os.IBinder.DeathRecipient]).
     *
     * **Usage:** Override this method to log session ends (e.g., "Session Ended") to calculate
     * session duration when paired with [onClientConnected]. You can also use this to perform
     * per-client resource cleanup.
     *
     * @param packageName The explicitly verified package name of the client application.
     * @param callerUid The Kernel-verified Linux UID of the client process.
     */
    protected open fun onClientDisconnected(packageName: String, callerUid: Int) {}

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
     * **Template Method:** This method is implemented by the host application (e.g., the System
     * Updater) and invoked by the [UpdateInfoService] base class on a background thread. The base
     * class handles concurrency, caching, and rate-limiting.
     *
     * **Preconditions:** This method is only called if [shouldFetchUpdates] returns `true` and
     * [shouldThrottle] returns `false` (the rate limiter allows the request).
     *
     * **Error Handling:** If this method throws an exception (e.g., due to a network timeout or
     * server error), the base class will catch it, invoke [onFetchFailed] for telemetry logging,
     * and gracefully return the currently cached data to the client.
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
     * Checks if the update check operation should be throttled.
     *
     * **Default Behavior:** The default implementation delegates to an internal rate limiter backed
     * by `SharedPreferences`. This enforces a standard minimum interval (e.g., 1 hour) between
     * network requests to protect backend infrastructure. The rate limiting state persists across
     * application restarts.
     *
     * **Host Customization:** Hosts can override this method to inject custom rate-limiting logic,
     * such as blocking background checks when the device is on a metered network or has low
     * battery.
     *
     * **Graceful Degradation:** If this method returns `true`, the base class skips calling
     * [fetchUpdates] and gracefully returns the currently cached data to the client. Because the
     * client receives the cached state along with its original `lastCheckTimeMillis`, the client is
     * not expected to manage complex backoff or retry loops.
     *
     * @return `true` if the network request should be blocked (throttled).
     */
    protected open fun shouldThrottle(): Boolean {
        return checkRateLimiter.shouldThrottle()
    }

    /**
     * Callback for handling exceptions that occur during the update check process.
     *
     * **Template Method:** The [UpdateInfoService] base class executes the update check logic and
     * catches all unhandled exceptions to prevent the service from crashing. When an exception is
     * caught, the base class gracefully falls back to returning the currently cached data to the
     * client, and invokes this method.
     *
     * This method will be triggered if an exception occurs during any step of the lifecycle,
     * including:
     * * Determining cache freshness (e.g., inside [shouldFetchUpdates]).
     * * Evaluating rate limits (e.g., inside [shouldThrottle]).
     * * Fetching data from the backend (e.g., inside [fetchUpdates]).
     * * Persisting the fetched results or metadata to local storage.
     *
     * The default implementation logs the error to Logcat. Hosts can override this method to report
     * failures to their own telemetry or crash reporting systems.
     *
     * @param e The exception caught by the base class during the operation.
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
     * Enforces that a given package name officially belongs to the calling process's UID.
     *
     * This method provides a critical security boundary against Intent spoofing by validating that
     * the client application is truthfully reporting its [packageName].
     *
     * **Multi-User / Work Profile Support:** We utilize the deprecated
     * [android.app.AppOpsManager.checkPackage] because it natively handles cross-profile UID
     * resolution within the system server. Modern alternatives like
     * [android.content.pm.PackageManager.getPackageUid] via `createContextAsUser` require the host
     * application to hold the highly privileged `INTERACT_ACROSS_USERS` permission, which violates
     * the principle of least privilege for OEM updaters.
     *
     * **Package Visibility:** The host application must have package visibility (e.g., via the
     * `QUERY_ALL_PACKAGES` permission or by running as the system user) to verify the client's
     * package name.
     *
     * This method is `internal open` to allow unit tests to bypass the system-level AppOpsManager
     * checks, which are otherwise difficult to mock comprehensively.
     *
     * @param packageName The self-reported package name provided by the client application.
     * @param uid The Kernel-verified Linux UID of the calling process.
     * @throws SecurityException If the package name does not belong to the UID, or if the host
     *   lacks visibility to the client package.
     */
    @VisibleForTesting
    @Suppress("DEPRECATION")
    internal open fun enforceValidPackageForUid(packageName: String, uid: Int) {
        val appOps = getSystemService(AppOpsManager::class.java)
        // checkPackage throws SecurityException if the uid/package don't match
        appOps?.checkPackage(uid, packageName)
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
