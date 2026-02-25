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
import androidx.security.state.SecurityPatchState
import androidx.security.state.SecurityPatchState.Companion.getComponentSecurityPatchLevel
import androidx.security.state.SerializableUpdateInfo
import androidx.security.state.UpdateInfo
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
 * [UpdateInfoService] querying [getAllUpdates] to return them to consumers.
 */
public class UpdateInfoManager(
    private val context: Context,
    customSecurityState: SecurityPatchState? = null,
) {

    private val updateInfoPrefs: String = "UPDATE_INFO_PREFS"
    private val metadataPrefs: String = "UPDATE_INFO_METADATA_PREFS"
    private var securityState: SecurityPatchState =
        customSecurityState ?: SecurityPatchState(context)

    private companion object {
        private const val KEY_LAST_CHECK_TIME = "last_check_time_millis"
    }

    /**
     * Registers information about an available update for the specified component.
     *
     * @param updateInfo Update information structure.
     */
    public fun registerUpdate(updateInfo: UpdateInfo) {
        cleanupUpdateInfo()

        val sharedPreferences = context.getSharedPreferences(updateInfoPrefs, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json =
            Json.encodeToString(
                SerializableUpdateInfo.serializer(),
                updateInfo.toSerializableUpdateInfo(),
            )
        editor.putString(updateInfo.component, json)
        editor.apply()
    }

    /**
     * Unregisters information about an available update for the specified component.
     *
     * @param updateInfo Update information structure.
     */
    public fun unregisterUpdate(updateInfo: UpdateInfo) {
        cleanupUpdateInfo()

        val sharedPreferences = context.getSharedPreferences(updateInfoPrefs, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove(updateInfo.component)
        editor.apply()
    }

    /**
     * Retrieves the timestamp of the last successful update check.
     *
     * @return The epoch timestamp in milliseconds, or 0L if no check has occurred.
     */
    public fun getLastCheckTimeMillis(): Long {
        val sharedPreferences = context.getSharedPreferences(metadataPrefs, Context.MODE_PRIVATE)
        return sharedPreferences.getLong(KEY_LAST_CHECK_TIME, 0L)
    }

    /**
     * Updates the timestamp of the last successful update check.
     *
     * This should be called by the [UpdateInfoService] immediately after a successful
     * synchronization with the backend.
     *
     * @param timestampMillis The current epoch timestamp in milliseconds.
     */
    public fun setLastCheckTimeMillis(timestampMillis: Long) {
        val sharedPreferences = context.getSharedPreferences(metadataPrefs, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putLong(KEY_LAST_CHECK_TIME, timestampMillis)
        editor.apply()
    }

    /**
     * Cleans up outdated or applied updates from the shared preferences. This method checks each
     * registered update against the current device security patch levels and removes any updates
     * that are no longer relevant (i.e., the update's patch level is less than or equal to the
     * current device patch level).
     */
    private fun cleanupUpdateInfo() {
        val allUpdates = getAllUpdates()
        val sharedPreferences = context.getSharedPreferences(updateInfoPrefs, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit() ?: return

        allUpdates.forEach { updateInfo ->
            val component = updateInfo.component
            val currentSpl: SecurityPatchState.SecurityPatchLevel
            try {
                currentSpl = securityState.getDeviceSecurityPatchLevel(component)
            } catch (e: IllegalArgumentException) {
                // Ignore unknown components.
                return@forEach
            }
            val updateSpl = getComponentSecurityPatchLevel(component, updateInfo.securityPatchLevel)

            if (updateSpl <= currentSpl) {
                editor.remove(updateInfo.component)
            }
        }

        editor.apply()
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
        val sharedPreferences = context.getSharedPreferences(updateInfoPrefs, Context.MODE_PRIVATE)
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
