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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
    /**
     * Key of the re-render token in the [AppWidgetManager.ACTION_APPWIDGET_UPDATE] intent sent by
     * [updateOptionsAndRerender], see [markPendingRender].
     */
    const val EXTRA_RENDER_TOKEN: String = "androidx.glance.adaptive.appwidget.extra.RENDER_TOKEN"

    private val lock = Any()
    private val cache: MutableIntObjectMap<WidgetOptionsState> = mutableIntObjectMapOf()

    /**
     * Pending re-render token per `appWidgetId`, set once the library has stored a placement's
     * widgetId from outside [GlanceAdaptiveWidgetReceiver], and cleared by the update sent for that
     * write. While it is pending, options change broadcasts cannot replace that widgetId in the
     * cache.
     */
    private val pendingRenderTokens: MutableIntObjectMap<String> = mutableIntObjectMapOf()

    /**
     * Generates a unique default string identifier for a widget instance when one is not explicitly
     * provided or already configured.
     */
    fun generateWidgetId(): String = UUID.randomUUID().toString()

    fun contains(appWidgetId: Int): Boolean = synchronized(lock) { cache.containsKey(appWidgetId) }

    fun getCachedWidgetId(appWidgetId: Int): String? =
        synchronized(lock) { cache[appWidgetId]?.widgetId?.takeIf { it.isNotBlank() } }

    @VisibleForTesting
    fun put(appWidgetId: Int, state: WidgetOptionsState): WidgetOptionsState? =
        synchronized(lock) { cache.put(appWidgetId, state) }

    fun remove(appWidgetIds: IntArray) {
        synchronized(lock) {
            for (id in appWidgetIds) {
                cache.remove(id)
                pendingRenderTokens.remove(id)
            }
        }
    }

    /**
     * Records [widgetId] for [appWidgetId] right before the library stores it from outside
     * [GlanceAdaptiveWidgetReceiver], keeping any other cached state (or using [initialState] when
     * the cache has no entry yet), so that lookups see it immediately. As one indivisible
     * operation, also stores a fresh token as the pending re-render marker of [appWidgetId],
     * replacing any previous one.
     *
     * Until [consumePendingRender] clears the marker, options change broadcasts keep [widgetId] in
     * the cache, because one queued before the write carries a stale widgetId, such as the
     * broadcast of the fallback widgetId write racing a pin callback.
     *
     * @return The token to send in [EXTRA_RENDER_TOKEN] with the update for this write.
     */
    fun markPendingRender(
        appWidgetId: Int,
        widgetId: String,
        initialState: WidgetOptionsState? = null,
    ): String = lockPendingRender(appWidgetId, widgetId, initialState).token

    internal fun lockPendingRender(
        appWidgetId: Int,
        widgetId: String,
        initialState: WidgetOptionsState? = null,
    ): PendingRenderLock {
        val token = UUID.randomUUID().toString()
        return synchronized(lock) {
            val previousState = cache[appWidgetId]
            val previousToken = pendingRenderTokens[appWidgetId]
            cache[appWidgetId] =
                previousState?.copy(widgetId = widgetId)
                    ?: initialState?.copy(widgetId = widgetId)
                    ?: WidgetOptionsState(
                        widgetId = widgetId,
                        minWidth = null,
                        minHeight = null,
                        maxWidth = null,
                        maxHeight = null,
                        sizes = null,
                    )
            pendingRenderTokens[appWidgetId] = token
            PendingRenderLock(
                appWidgetId = appWidgetId,
                token = token,
                previousState = previousState,
                previousToken = previousToken,
            )
        }
    }

    internal class PendingRenderLock(
        val appWidgetId: Int,
        val token: String,
        private val previousState: WidgetOptionsState?,
        private val previousToken: String?,
    ) {
        fun rollback() {
            synchronized(lock) {
                if (pendingRenderTokens[appWidgetId] != token) return
                if (previousState != null) {
                    cache[appWidgetId] = previousState
                } else {
                    cache.remove(appWidgetId)
                }
                if (previousToken != null) {
                    pendingRenderTokens[appWidgetId] = previousToken
                } else {
                    pendingRenderTokens.remove(appWidgetId)
                }
            }
        }
    }

    /**
     * Clears the pending re-render marker of [appWidgetId] if, and only if, it equals [token], as
     * one indivisible operation, so that the update sent for an older write never clears the marker
     * of a newer one.
     *
     * @return true if [token] matched the pending marker.
     */
    fun consumePendingRender(appWidgetId: Int, token: String?): Boolean {
        if (token == null) return false
        synchronized(lock) {
            val currentToken = pendingRenderTokens[appWidgetId]
            if (currentToken == null) {
                // If an update carrying a token arrives when no marker is pending (for example,
                // after process death if a stale broadcast warmed the cache first), evict the
                // cached entry so onUpdate reloads the persisted options from AppWidgetManager.
                cache.remove(appWidgetId)
                return false
            }
            if (currentToken != token) return false
            pendingRenderTokens.remove(appWidgetId)
            return true
        }
    }

    /**
     * Caches [state], read from an [AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED] broadcast
     * for [appWidgetId]. While a re-render marker is pending, the widgetId cached by
     * [markPendingRender] is kept, and the rest of [state], such as a new size, is cached as usual.
     *
     * @return true if the cached state changed, meaning the placement must be updated.
     */
    fun putFromBroadcast(appWidgetId: Int, state: WidgetOptionsState): Boolean {
        synchronized(lock) {
            val newState =
                if (pendingRenderTokens.containsKey(appWidgetId)) {
                    cache[appWidgetId]
                        ?.widgetId
                        ?.takeIf { it.isNotBlank() }
                        ?.let { state.copy(widgetId = it) } ?: state
                } else {
                    state
                }
            return cache.put(appWidgetId, newState) != newState
        }
    }

    @VisibleForTesting
    fun hasPendingRender(appWidgetId: Int): Boolean =
        synchronized(lock) { pendingRenderTokens.containsKey(appWidgetId) }

    fun restore(oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        val count = minOf(oldWidgetIds.size, newWidgetIds.size)
        synchronized(lock) {
            val states = arrayOfNulls<WidgetOptionsState>(count)
            for (i in 0 until count) {
                states[i] = cache.remove(oldWidgetIds[i])
                // The update that would clear it targets the old appWidgetId.
                pendingRenderTokens.remove(oldWidgetIds[i])
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
        synchronized(lock) {
            cache.clear()
            pendingRenderTokens.clear()
        }
    }
}

/**
 * Stores [options] in the options of [appWidgetId], a widget of [provider], from outside
 * [GlanceAdaptiveWidgetReceiver], then sends an explicit [AppWidgetManager.ACTION_APPWIDGET_UPDATE]
 * for it to [provider], so that the widget is updated with them.
 *
 * The platform does not always send [AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED] back to the
 * provider that wrote its own options, and when it does, the options may look unchanged by then
 * because a lookup already cached them. The update is therefore what updates the widget, on every
 * Android version.
 *
 * Before writing to [AppWidgetManager], the target widgetId is locked in [WidgetOptionsCache] with
 * [WidgetOptionsCache.lockPendingRender], so that lookups see it immediately and broadcasts queued
 * before or during the write cannot roll it back. If [AppWidgetManager.updateAppWidgetOptions]
 * throws, the cached state and marker are rolled back and the exception propagates without sending
 * an update. The update carries the marker's token and clears it. On older Android versions, the
 * options change broadcast of the write then matches the cache and does not update the widget
 * again.
 */
internal fun AppWidgetManager.updateOptionsAndRerender(
    context: Context,
    provider: ComponentName,
    appWidgetId: Int,
    options: Bundle,
) {
    val widgetId =
        options.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID)?.takeIf { it.isNotBlank() }
    val initialState =
        if (widgetId != null && !WidgetOptionsCache.contains(appWidgetId)) {
            val mergedOptions =
                Bundle().apply {
                    runCatching { getAppWidgetOptions(appWidgetId) }.getOrNull()?.let(::putAll)
                    putAll(options)
                }
            WidgetOptionsState.from(mergedOptions)
        } else {
            null
        }
    val pendingLock = widgetId?.let {
        WidgetOptionsCache.lockPendingRender(appWidgetId, it, initialState)
    }
    try {
        updateAppWidgetOptions(appWidgetId, options)
    } catch (t: Throwable) {
        pendingLock?.rollback()
        throw t
    }
    context.sendBroadcast(
        Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .setComponent(provider)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            .putExtra(WidgetOptionsCache.EXTRA_RENDER_TOKEN, pendingLock?.token)
            // As prompt as the options change broadcasts of the platform: the user is waiting
            // for the widget, after pinning or configuring it.
            .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
    )
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
