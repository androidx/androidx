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
import kotlinx.serialization.json.Json

/**
 * This class interfaces with a [SecurityPatchState] to manage update information for system
 * components.
 *
 * Typically, OTA or other update clients utilize this class to expose update information to other
 * applications or components within the system that need access to the latest security updates
 * data. The client calls [registerUpdate] to add [UpdateInfo] to a local store and
 * [unregisterUpdate] to remove it. [UpdateInfoProvider] then serves this data to the applications.
 */
public class UpdateInfoManager(
    private val context: Context,
    customSecurityState: SecurityPatchState? = null
) {

    private val updateInfoPrefs: String = "UPDATE_INFO_PREFS"
    private var securityState: SecurityPatchState =
        customSecurityState ?: SecurityPatchState(context)

    /**
     * Registers information about an available update for the specified component.
     *
     * @param updateInfo Update information structure.
     */
    public fun registerUpdate(updateInfo: UpdateInfo) {
        cleanupUpdateInfo()

        val sharedPreferences = context.getSharedPreferences(updateInfoPrefs, Context.MODE_PRIVATE)
        val editor = sharedPreferences?.edit()
        val key = getKeyForUpdateInfo(updateInfo)
        val json =
            Json.encodeToString(
                SerializableUpdateInfo.serializer(),
                updateInfo.toSerializableUpdateInfo()
            )
        editor?.putString(key, json)
        editor?.apply()
    }

    /**
     * Unregisters information about an available update for the specified component.
     *
     * @param updateInfo Update information structure.
     */
    public fun unregisterUpdate(updateInfo: UpdateInfo) {
        cleanupUpdateInfo()

        val sharedPreferences = context.getSharedPreferences(updateInfoPrefs, Context.MODE_PRIVATE)
        val editor = sharedPreferences?.edit()
        val key = getKeyForUpdateInfo(updateInfo)
        editor?.remove(key)
        editor?.apply()
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
        val editor = sharedPreferences?.edit() ?: return

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
                val key = getKeyForUpdateInfo(updateInfo)
                editor.remove(key)
            }
        }

        editor.apply()
    }

    private fun getKeyForUpdateInfo(updateInfo: UpdateInfo): String {
        // Create a unique key for each update info.
        return "${updateInfo.component}-${updateInfo.uri}"
    }

    /**
     * Retrieves a list of all updates currently registered in the system's shared preferences. This
     * method is primarily used for managing and tracking updates that have been registered but not
     * yet applied or acknowledged by the system.
     *
     * @return A list of [UpdateInfo] objects, each representing a registered update.
     */
    private fun getAllUpdates(): List<UpdateInfo> {
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
    internal fun getAllUpdatesAsJson(): List<String> {
        val allUpdates = mutableListOf<String>()
        val sharedPreferences = context.getSharedPreferences(updateInfoPrefs, Context.MODE_PRIVATE)
        val allEntries = sharedPreferences?.all ?: return emptyList()
        for ((_, value) in allEntries) {
            val json = value as? String
            if (json != null) {
                allUpdates.add(json)
            }
        }
        return allUpdates
    }
}
