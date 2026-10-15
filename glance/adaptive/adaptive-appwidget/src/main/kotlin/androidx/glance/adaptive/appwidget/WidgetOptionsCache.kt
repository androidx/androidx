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

package androidx.glance.adaptive.appwidget

import android.appwidget.AppWidgetManager
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import androidx.annotation.VisibleForTesting
import androidx.collection.MutableIntObjectMap
import androidx.collection.mutableIntObjectMapOf
import java.util.UUID

/**
 * Thread-safe in-memory cache of resolved [WidgetOptionsState] per platform `appWidgetId`, avoiding
 * redundant [AppWidgetManager.getAppWidgetOptions] Binder IPC calls across widget lifecycle
 * updates, telemetry writes, and instance repository queries.
 */
internal object WidgetOptionsCache {
    private val lock = Any()
    private val cache: MutableIntObjectMap<WidgetOptionsState> = mutableIntObjectMapOf()

    /**
     * Generates a unique default string identifier for a widget instance when one is not explicitly
     * provided or already configured.
     */
    fun generateWidgetId(): String = UUID.randomUUID().toString()

    fun contains(appWidgetId: Int): Boolean = synchronized(lock) { cache.containsKey(appWidgetId) }

    fun getCachedWidgetId(appWidgetId: Int): String? =
        synchronized(lock) { cache[appWidgetId]?.widgetId?.takeIf { it.isNotBlank() } }

    fun put(appWidgetId: Int, state: WidgetOptionsState): WidgetOptionsState? =
        synchronized(lock) { cache.put(appWidgetId, state) }

    fun remove(appWidgetIds: IntArray) {
        synchronized(lock) {
            for (id in appWidgetIds) {
                cache.remove(id)
            }
        }
    }

    fun restore(oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        val count = minOf(oldWidgetIds.size, newWidgetIds.size)
        synchronized(lock) {
            val states = arrayOfNulls<WidgetOptionsState>(count)
            for (i in 0 until count) {
                states[i] = cache.remove(oldWidgetIds[i])
            }
            for (i in 0 until count) {
                val cachedState = states[i]
                if (cachedState != null) {
                    cache[newWidgetIds[i]] = cachedState
                }
            }
        }
    }

    fun cacheWidgetOptionsIfMissing(appWidgetId: Int, options: Bundle): String {
        // `cache` is a primitive-keyed MutableIntObjectMap (avoiding Int boxing) guarded by `lock`.
        // Check presence under a brief lock, then parse the Bundle outside the lock so parcel
        // unmarshalling and sorting do not hold the monitor.
        val existingWidgetId = getCachedWidgetId(appWidgetId)
        if (existingWidgetId != null) {
            return existingWidgetId
        }
        val parsed = WidgetOptionsState.from(options)
        val resolvedWidgetId = parsed.widgetId?.takeIf { it.isNotBlank() } ?: generateWidgetId()
        val state =
            if (parsed.widgetId == resolvedWidgetId) {
                parsed
            } else {
                parsed.copy(widgetId = resolvedWidgetId)
            }
        return synchronized(lock) {
            cache[appWidgetId]?.widgetId?.takeIf { it.isNotBlank() }
                ?: run {
                    cache[appWidgetId] = state
                    resolvedWidgetId
                }
        }
    }

    @VisibleForTesting
    fun clear() {
        synchronized(lock) { cache.clear() }
    }
}

internal data class WidgetOptionsState(
    val widgetId: String?,
    val minWidth: Int?,
    val minHeight: Int?,
    val maxWidth: Int?,
    val maxHeight: Int?,
    val sizes: List<SizeF>?,
) {
    companion object {
        fun from(options: Bundle): WidgetOptionsState {
            fun getIntOrNull(key: String): Int? =
                if (options.containsKey(key)) options.getInt(key) else null

            return WidgetOptionsState(
                widgetId = options.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID),
                minWidth = getIntOrNull(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
                minHeight = getIntOrNull(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT),
                maxWidth = getIntOrNull(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH),
                maxHeight = getIntOrNull(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT),
                sizes = getWidgetSizes(options),
            )
        }
    }
}

private fun getWidgetSizes(options: Bundle): List<SizeF>? {
    val rawSizes =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            options.getParcelableArrayList(
                AppWidgetManager.OPTION_APPWIDGET_SIZES,
                SizeF::class.java,
            )
        } else {
            @Suppress("DEPRECATION")
            options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
        } ?: return null

    // Copy to a new ArrayList to avoid mutating the Bundle's internal list in-place.
    // Note: ArrayList(Collection) + MutableList.sortWith is used instead of Iterable.sortedWith
    // to avoid iterator allocations flagged by AndroidX's ListIterator lint check.
    val sortedSizes = ArrayList(rawSizes)
    sortedSizes.sortWith { a, b ->
        val widthCompare = a.width.compareTo(b.width)
        if (widthCompare != 0) widthCompare else a.height.compareTo(b.height)
    }
    return sortedSizes
}
