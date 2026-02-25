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

package androidx.glance.wear

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.StrictMode
import androidx.annotation.VisibleForTesting
import androidx.glance.wear.ActiveWidgetStore.Companion.ACTIVE_WIDGET_SHARED_PREF_NAME
import androidx.glance.wear.ActiveWidgetStore.Companion.INACTIVE_WIDGET_DURATION
import androidx.glance.wear.ActiveWidgetStore.Companion.getWidgetKey
import androidx.glance.wear.core.ActiveWearWidgetHandle
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.WidgetInstanceId
import java.time.Duration
import java.time.Instant

/**
 * A store for active widgets used for API < 34 where
 * [com.google.wear.services.tiles.TilesManager.getActiveTiles] is not available.
 *
 * This store must only be used on API < 34.
 */
internal class ActiveWidgetStore
/**
 * Creates a new [ActiveWidgetStore].
 *
 * @param context The application context.
 * @param getCurrentTimeInstant The function that returns the current time instant.
 */
@VisibleForTesting
internal constructor(
    private val context: Context,
    private val getCurrentTimeInstant: () -> Instant,
) {

    /**
     * Creates a new [ActiveWidgetStore].
     *
     * @param context The application context.
     */
    constructor(context: Context) : this(context, Instant::now)

    /** Shared preferences for active widgets. */
    private val activeWidgetPref: SharedPreferences =
        context.getSharedPreferences(ACTIVE_WIDGET_SHARED_PREF_NAME, Context.MODE_PRIVATE)

    /**
     * Marks a widget instance as active by adding it to [ACTIVE_WIDGET_SHARED_PREF_NAME]. If the
     * widget is already recorded, its timestamp is updated to the current time.
     */
    fun markWidgetAsActive(componentName: ComponentName, instanceId: Int) {
        val now = getCurrentTimeInstant()
        val key = getWidgetKey(componentName, instanceId)
        allowThreadDiskWrites {
            if (activeWidgetPref.contains(key)) {
                val start = Instant.ofEpochMilli(activeWidgetPref.getLong(key, 0))
                val duration = Duration.between(start, now)
                if (duration < UPDATE_WIDGET_DURATION) {
                    return@allowThreadDiskWrites
                }
            }

            activeWidgetPref
                .edit()
                .putLong(key, now.toEpochMilli())
                .cleanUpActiveWidgets(now)
                .apply()
        }
    }

    /**
     * Marks a widget instance as inactive by removing its entry from
     * [ACTIVE_WIDGET_SHARED_PREF_NAME], if it exists.
     */
    fun markWidgetAsInactive(componentName: ComponentName, instanceId: Int) {
        val key = getWidgetKey(componentName, instanceId)
        allowThreadDiskWrites { activeWidgetPref.edit().remove(key).cleanUpActiveWidgets().apply() }
    }

    /** Returns all active widgets and tiles associated with the calling package. */
    fun getActiveWidgets(): List<ActiveWearWidgetHandle> = allowThreadDiskWrites {
        activeWidgetPref.edit().cleanUpActiveWidgets().apply()
        activeWidgetPref.all.mapNotNull { (key, _) ->
            val (componentName, instanceId) =
                getComponentAndInstanceId(key) ?: return@mapNotNull null
            ActiveWearWidgetHandle(
                provider = componentName,
                instanceId =
                    WidgetInstanceId(WidgetInstanceId.WIDGET_CAROUSEL_NAMESPACE, instanceId),
                containerType = ContainerInfo.CONTAINER_TYPE_FULLSCREEN,
            )
        }
    }

    /**
     * Cleans up [ACTIVE_WIDGET_SHARED_PREF_NAME] by removing widget instances that have been
     * inactive for longer than [INACTIVE_WIDGET_DURATION].
     */
    private fun SharedPreferences.Editor.cleanUpActiveWidgets(
        now: Instant = getCurrentTimeInstant()
    ): SharedPreferences.Editor {
        // There is a potential race condition between reading and removing the value. However,
        // given the 60-day expiration period, it is highly unlikely that the value will be
        // updated concurrently after such a long duration. We accept this risk.
        activeWidgetPref.all.forEach { (key, value) ->
            val timestamp = value as? Long
            if (timestamp == null) {
                remove(key)
                return@forEach
            }
            val duration = Duration.between(Instant.ofEpochMilli(timestamp), now)
            if (duration >= INACTIVE_WIDGET_DURATION) {
                remove(key)
            }
        }
        return this
    }

    private companion object {
        /**
         * Name of the [SharedPreferences] file used to store active widgets on older OS versions
         * where SDK API is not available.
         * * These preferences are shared across all [GlanceWearWidgetService] and `TileService`
         *   implementations within the app.
         * * NOTE: This value must match the internal constant used in
         *   `androidx.wear.tiles.TileService.ACTIVE_TILES_SHARED_PREF_NAME`.
         */
        const val ACTIVE_WIDGET_SHARED_PREF_NAME: String = "active_tiles_shared_preferences"
        /**
         * The refresh period indicating the widget instance stored in
         * [ACTIVE_WIDGET_SHARED_PREF_NAME] is still active.
         */
        val UPDATE_WIDGET_DURATION: Duration = Duration.ofDays(1)
        /**
         * Timeout period for removing inactive widgets from [ACTIVE_WIDGET_SHARED_PREF_NAME] based
         * on the last timestamp update.
         */
        val INACTIVE_WIDGET_DURATION: Duration = Duration.ofDays(60)

        /**
         * Generates the unique key used to identify a widget in [ACTIVE_WIDGET_SHARED_PREF_NAME].
         * * Must remain consistent with the internal implementation in
         *   `androidx.wear.tiles.ActiveTileIdentifier.flattenToString()`.
         */
        fun getWidgetKey(componentName: ComponentName, instanceId: Int): String =
            "$instanceId:${componentName.flattenToString()}"

        /**
         * Extracts the [ComponentName] and the instanceId from the key generated with
         * [getWidgetKey] and used to store the widget in [ACTIVE_WIDGET_SHARED_PREF_NAME].
         */
        fun getComponentAndInstanceId(key: String): Pair<ComponentName, Int>? {
            val (instanceId, componentName) = key.split(":", limit = 2)
            return Pair(
                ComponentName.unflattenFromString(componentName) ?: return null,
                instanceId.toIntOrNull() ?: return null,
            )
        }

        /**
         * Utility function that allows thread disk reads&writes.
         *
         * @param block The block to execute with thread disk reads&writes allowed.
         */
        fun <T> allowThreadDiskWrites(block: () -> T): T {
            val policy = StrictMode.allowThreadDiskWrites()
            return try {
                block()
            } finally {
                StrictMode.setThreadPolicy(policy)
            }
        }
    }
}
