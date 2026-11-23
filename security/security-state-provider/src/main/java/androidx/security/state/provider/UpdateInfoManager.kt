/*
 * Copyright 2024 The Android Open Source Project
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
import android.os.Process
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.security.state.SecurityPatchState
import androidx.security.state.SerializableUpdateInfo
import androidx.security.state.UpdateInfo
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json

/**
 * Manages the persistent storage of security update information.
 *
 * This class acts as the local database for the [UpdateInfoService]. It is responsible for:
 * 1. Storing the list of available [UpdateInfo] objects (persisted in SharedPreferences).
 * 2. Storing metadata about the update checks (e.g., [getLastCheckTimeMillis]).
 * 3. Cleaning up outdated updates by comparing them against the device's current state.
 *
 * Typical usage involves an update client (like GOTA (Google Over-The-Air) or Play Store)
 * registering new updates via [registerUpdate] when they are discovered, and the
 * [UpdateInfoService] querying [getAllUpdates] to return them to consumers. An update client that
 * checks more than one component also records the time of each check with
 * `setLastCheckTimeMillis(component, timestampMillis)`, so that consumers can tell which components
 * were checked.
 */
public class UpdateInfoManager
@VisibleForTesting
internal constructor(
    context: Context,
    customSecurityState: SecurityPatchState?,
    private val backgroundExecutor: Executor,
) {

    public constructor(
        context: Context,
        customSecurityState: SecurityPatchState? = null,
    ) : this(context, customSecurityState, DEFAULT_CLEANUP_EXECUTOR)

    private val appContext = context.applicationContext ?: context
    private val securityState: SecurityPatchState =
        customSecurityState ?: SecurityPatchState(appContext)

    private companion object {
        private val writeLock = Any()

        private const val UPDATE_INFO_PREFS = "UPDATE_INFO_PREFS"
        private const val METADATA_PREFS = "UPDATE_INFO_METADATA_PREFS"
        private const val KEY_LAST_CHECK_TIME = "last_check_time_millis"
        private const val KEY_COMPONENT_LAST_CHECK_TIME_PREFIX = "last_check_time_millis_component_"
        // Use a dynamic ThreadPoolExecutor with corePoolSize = 0 and keepAliveTime to
        // allow the thread to terminate when idle. This prevents classloader leaks
        // when this class is loaded dynamically (e.g., inside dynamic feature modules
        // or container-based applications).
        private val DEFAULT_CLEANUP_EXECUTOR: Executor =
            ThreadPoolExecutor(
                /* corePoolSize= */ 0,
                /* maximumPoolSize= */ 1,
                /* keepAliveTime= */ 60L,
                TimeUnit.SECONDS,
                LinkedBlockingQueue<Runnable>(),
                ThreadFactory { runnable ->
                    Thread(
                        {
                            try {
                                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                            } catch (e: SecurityException) {
                                Log.w(
                                    "UpdateInfoManager",
                                    "Failed to set background thread priority",
                                    e,
                                )
                            }
                            runnable.run()
                        },
                        "UpdateInfoManagerCompactor",
                    )
                },
            )
    }

    /**
     * Registers information about an available update for the specified component.
     *
     * @param updateInfo Update information structure.
     */
    public fun registerUpdate(updateInfo: UpdateInfo) {
        val sharedPreferences =
            appContext.getSharedPreferences(UPDATE_INFO_PREFS, Context.MODE_PRIVATE)
        val json =
            Json.encodeToString(
                SerializableUpdateInfo.serializer(),
                updateInfo.toSerializableUpdateInfo(),
            )
        synchronized(writeLock) {
            val editor = sharedPreferences.edit()
            editor.putString(updateInfo.component, json)
            editor.apply()
        }

        try {
            backgroundExecutor.execute { cleanupUpdateInfo() }
        } catch (e: RejectedExecutionException) {
            // Ignore rejection
        }
    }

    /**
     * Unregisters information about an available update for the specified component.
     *
     * @param updateInfo Update information structure.
     */
    public fun unregisterUpdate(updateInfo: UpdateInfo) {
        val sharedPreferences =
            appContext.getSharedPreferences(UPDATE_INFO_PREFS, Context.MODE_PRIVATE)
        synchronized(writeLock) {
            val editor = sharedPreferences.edit()
            editor.remove(updateInfo.component)
            editor.apply()
        }

        try {
            backgroundExecutor.execute { cleanupUpdateInfo() }
        } catch (e: RejectedExecutionException) {
            // Ignore rejection
        }
    }

    /**
     * Retrieves the timestamp of the last successful update check.
     *
     * This metadata is stored separately from the update list. The value represents "Wall Clock
     * Time" to ensure it remains meaningful across device reboots.
     *
     * The result is the latest of the provider-wide check time and any check times recorded for
     * individual components with `setLastCheckTimeMillis(component, timestampMillis)`. If no
     * component check time has been recorded, it is the provider-wide check time.
     *
     * @return The time of the last check in milliseconds since the epoch
     *   ([System.currentTimeMillis]), or 0 if no check has ever occurred.
     */
    public fun getLastCheckTimeMillis(): Long {
        val prefs = appContext.getSharedPreferences(METADATA_PREFS, Context.MODE_PRIVATE)
        val globalCheckTime = prefs.getLong(KEY_LAST_CHECK_TIME, 0L).coerceAtLeast(0L)
        val maxComponentCheckTime = getComponentLastCheckTimesMillis().values.maxOrNull() ?: 0L
        return maxOf(globalCheckTime, maxComponentCheckTime)
    }

    /**
     * Returns the last successful check time recorded for [component], in milliseconds since the
     * epoch.
     *
     * Only times recorded with `setLastCheckTimeMillis(component, timestampMillis)` are returned.
     * The provider-wide check time and the check times of registered updates are not used.
     *
     * @param component component identifier, such as [SecurityPatchState.COMPONENT_SYSTEM] or
     *   [SecurityPatchState.COMPONENT_SYSTEM_MODULES]
     * @return last check time recorded for [component] in milliseconds since the epoch
     *   ([System.currentTimeMillis]), or 0 if none has been recorded
     */
    public fun getLastCheckTimeMillis(@SecurityPatchState.Component component: String): Long {
        if (component.isEmpty()) return 0L
        val prefs = appContext.getSharedPreferences(METADATA_PREFS, Context.MODE_PRIVATE)
        return prefs
            .getLong("$KEY_COMPONENT_LAST_CHECK_TIME_PREFIX$component", 0L)
            .coerceAtLeast(0L)
    }

    /**
     * Returns the last successful check time recorded for each component, in milliseconds since the
     * epoch, keyed by component identifier.
     *
     * A component is omitted if no positive time has been recorded for it with
     * `setLastCheckTimeMillis(component, timestampMillis)`.
     */
    internal fun getComponentLastCheckTimesMillis(): Map<String, Long> {
        val prefs = appContext.getSharedPreferences(METADATA_PREFS, Context.MODE_PRIVATE)
        val allEntries = prefs.all ?: return emptyMap()
        return buildMap {
            for ((key, value) in allEntries) {
                if (
                    key.startsWith(KEY_COMPONENT_LAST_CHECK_TIME_PREFIX) &&
                        key.length > KEY_COMPONENT_LAST_CHECK_TIME_PREFIX.length &&
                        value is Long &&
                        value > 0L
                ) {
                    put(key.removePrefix(KEY_COMPONENT_LAST_CHECK_TIME_PREFIX), value)
                }
            }
        }
    }

    /**
     * Updates the provider-wide timestamp of the last successful update check.
     *
     * **Usage Note for Hosts:** The [UpdateInfoService] base class automatically calls this method
     * after a successful network fetch triggered by a client request. Host applications should only
     * call this method manually if they are performing out-of-band synchronizations (e.g., via a
     * background `JobService` or `WorkManager`).
     *
     * A host that checks more than one component should instead record each check with
     * `setLastCheckTimeMillis(component, timestampMillis)`, so that clients can tell which
     * component was checked. `getLastCheckTimeMillis()` takes those per-component times into
     * account.
     *
     * @param timestampMillis The current time in milliseconds ([System.currentTimeMillis]).
     */
    public fun setLastCheckTimeMillis(timestampMillis: Long) {
        val sharedPreferences =
            appContext.getSharedPreferences(METADATA_PREFS, Context.MODE_PRIVATE)
        synchronized(writeLock) {
            val editor = sharedPreferences.edit()
            editor.putLong(KEY_LAST_CHECK_TIME, timestampMillis)
            editor.apply()
        }
    }

    /**
     * Updates or clears the time of the last successful update check for [component].
     *
     * A host that checks more than one component should call this after every successful check of
     * [component], including checks that find no update, so that a check of one component is not
     * mistaken for a check of another. [registerUpdate] and [unregisterUpdate] do not record a
     * check time for the component.
     *
     * Passing 0 removes the recorded check time for [component], so that it is omitted from
     * [androidx.security.state.UpdateCheckResult.componentLastCheckTimesMillis].
     *
     * @param component component identifier, such as [SecurityPatchState.COMPONENT_SYSTEM] or
     *   [SecurityPatchState.COMPONENT_SYSTEM_MODULES]
     * @param timestampMillis wall-clock time of the check in milliseconds
     *   ([System.currentTimeMillis]), or 0 to clear the recorded time for [component]
     * @throws IllegalArgumentException if [component] is empty or [timestampMillis] is negative
     */
    public fun setLastCheckTimeMillis(
        @SecurityPatchState.Component component: String,
        timestampMillis: Long,
    ) {
        require(component.isNotEmpty()) { "Component must not be empty." }
        require(timestampMillis >= 0L) { "Timestamp must be non-negative: $timestampMillis" }
        val sharedPreferences =
            appContext.getSharedPreferences(METADATA_PREFS, Context.MODE_PRIVATE)
        val key = "$KEY_COMPONENT_LAST_CHECK_TIME_PREFIX$component"
        synchronized(writeLock) {
            val editor = sharedPreferences.edit()
            if (timestampMillis == 0L) {
                editor.remove(key)
            } else {
                editor.putLong(key, timestampMillis)
            }
            editor.apply()
        }
    }

    // internal for testing
    @VisibleForTesting
    internal fun cleanupUpdateInfo() {
        val sharedPreferences =
            appContext.getSharedPreferences(UPDATE_INFO_PREFS, Context.MODE_PRIVATE)
        val allEntries = sharedPreferences.all ?: return
        val targetsToRemove = mutableMapOf<String, String>()

        allEntries.forEach { (component, value) ->
            val rawJson = value as? String ?: return@forEach

            val updateInfo: UpdateInfo
            try {
                updateInfo = Json.decodeFromString<SerializableUpdateInfo>(rawJson).toUpdateInfo()
            } catch (e: Exception) {
                targetsToRemove[component] = rawJson
                return@forEach
            }

            val currentSpl: SecurityPatchState.SecurityPatchLevel
            try {
                currentSpl = securityState.getDeviceSecurityPatchLevel(component)
            } catch (e: Exception) {
                // Ignore unknown components or errors retrieving SPL.
                return@forEach
            }

            try {
                if (updateInfo.securityPatchLevel <= currentSpl) {
                    targetsToRemove[component] = rawJson
                }
            } catch (e: IllegalArgumentException) {
                // Incompatible types or generic string fallback -> remove
                targetsToRemove[component] = rawJson
            }
        }

        if (targetsToRemove.isNotEmpty()) {
            // Acquire the writeLock before modifying SharedPreferences to prevent TOCTOU
            // (Time-of-Check to Time-of-Use) race conditions. This ensures that the background
            // compaction thread does not delete a brand-new valid update written by the main
            // thread concurrently.
            synchronized(writeLock) {
                val editor = sharedPreferences.edit()
                targetsToRemove.forEach { (component, expectedRawJson) ->
                    val currentJson = sharedPreferences.getString(component, null)
                    if (currentJson == expectedRawJson) {
                        editor.remove(component)
                    }
                }
                editor.apply()
            }
        }
    }

    /**
     * Retrieves a list of all updates currently registered in the system's shared preferences. This
     * method is primarily used for managing and tracking updates that have been registered but not
     * yet applied or acknowledged by the system.
     *
     * @return A list of [UpdateInfo] objects, each representing a registered update.
     */
    internal fun getAllUpdates(): List<UpdateInfo> {
        val allUpdates = mutableListOf<UpdateInfo>()
        for (json in getAllUpdatesAsJson()) {
            val serializableUpdateInfo: SerializableUpdateInfo = Json.decodeFromString(json)
            val updateInfo: UpdateInfo = serializableUpdateInfo.toUpdateInfo()
            allUpdates.add(updateInfo)
        }
        return allUpdates
    }

    /**
     * Retrieves all registered updates in JSON format from the system's shared preferences.
     *
     * @return A list of strings, each representing an update in JSON format.
     */
    private fun getAllUpdatesAsJson(): List<String> {
        val allUpdates = mutableListOf<String>()
        val sharedPreferences =
            appContext.getSharedPreferences(UPDATE_INFO_PREFS, Context.MODE_PRIVATE)
        val allEntries = sharedPreferences.all ?: return emptyList()
        for ((_, value) in allEntries) {
            val json = value as? String
            if (json != null) {
                allUpdates.add(json)
            }
        }
        return allUpdates
    }
}
