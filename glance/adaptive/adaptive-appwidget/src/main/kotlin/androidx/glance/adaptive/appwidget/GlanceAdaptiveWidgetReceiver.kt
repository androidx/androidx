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
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SizeF
import androidx.annotation.CallSuper
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.collection.MutableIntObjectMap
import androidx.collection.mutableIntObjectMapOf
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// Tag length must not exceed 23 characters for pre-API 24 log compatibility.
private const val TAG = "GlanceAdaptiveReceiver"

/**
 * [AppWidgetProvider] using Glance Adaptive to process widget lifecycle events and updates.
 *
 * This should typically be used as:
 * ```kotlin
 * class MyGlanceAdaptiveWidgetReceiver : GlanceAdaptiveWidgetReceiver() {
 *     override val widgetName: String = "profile_widget"
 *     override suspend fun onUpdate(context: Context) {
 *         // Provide or push updated template data
 *     }
 * }
 * ```
 *
 * To filter broadcast updates by widget name at the manifest level (recommended when using multiple
 * widget receivers in the same application), add the [META_DATA_WIDGET_NAME] meta-data tag to the
 * receiver declaration in `AndroidManifest.xml`:
 * ```xml
 * <receiver android:name=".MyGlanceAdaptiveWidgetReceiver" android:exported="true">
 *     <intent-filter>
 *         <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
 *         <action android:name="android.intent.action.LOCALE_CHANGED" />
 *     </intent-filter>
 *     <meta-data
 *         android:name="androidx.glance.adaptive.WIDGET_NAME"
 *         android:value="profile_widget" />
 * </receiver>
 * ```
 *
 * Note: If you override any methods of this class or [AppWidgetProvider], ensure you call their
 * superclass implementation. For the [onUpdate] flow managed by this receiver, do not call
 * [AppWidgetProvider.goAsync] manually, as broadcast lifecycle completion is handled by the
 * superclass implementation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class GlanceAdaptiveWidgetReceiver : AppWidgetProvider() {

    /**
     * Unique developer-defined identifier used to associate this receiver with a specific widget
     * definition (e.g., "profile_widget").
     *
     * This identifier should correspond to the widget template definition or layout configuration
     * registered with the adaptive widget framework.
     */
    public abstract val widgetName: String

    /**
     * Override [coroutineContext] to provide a custom [CoroutineContext] in which background update
     * tasks are executed.
     *
     * This context is used when launching the asynchronous broadcast block. It should include a
     * dispatcher suitable for background work (e.g., [Dispatchers.Default] or [Dispatchers.IO]).
     */
    public open val coroutineContext: CoroutineContext = Dispatchers.Default

    /**
     * Asynchronous lifecycle hook implemented by developers to provide fresh data for active widget
     * instances.
     *
     * Invoked on a background coroutine in response to widget updates (such as initial placement or
     * periodic update alarms).
     *
     * @param context The application Context.
     */
    public open suspend fun onUpdate(context: Context) {
        // Default no-op. Overridden by developers to provide initial or refreshed data.
    }

    @CallSuper
    override fun onUpdate(
        @Suppress("InvalidNullabilityOverride") context: Context,
        @Suppress("InvalidNullabilityOverride") appWidgetManager: AppWidgetManager,
        @Suppress("InvalidNullabilityOverride") appWidgetIds: IntArray?,
    ) {
        runAndLogExceptions {
            cacheWidgetOptionsIfMissing(appWidgetManager, appWidgetIds)
            goAsync(coroutineContext) { onUpdate(context) }
        }
    }

    private fun cacheWidgetOptionsIfMissing(
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray?,
    ) {
        appWidgetIds?.forEach { id ->
            val isCached = synchronized(lock) { lastOptionsCache.containsKey(id) }
            if (!isCached) {
                val options = appWidgetManager.getAppWidgetOptions(id)
                if (options != null) {
                    val state = WidgetOptionsState.from(options)
                    synchronized(lock) { lastOptionsCache[id] = state }
                }
            }
        }
    }

    @CallSuper
    override fun onAppWidgetOptionsChanged(
        @Suppress("InvalidNullabilityOverride") context: Context,
        @Suppress("InvalidNullabilityOverride") appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        @Suppress("InvalidNullabilityOverride") newOptions: Bundle,
    ) {
        runAndLogExceptions {
            super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
            val newState = WidgetOptionsState.from(newOptions)
            val oldState =
                synchronized(lock) {
                    val prev = lastOptionsCache[appWidgetId]
                    lastOptionsCache[appWidgetId] = newState
                    prev
                }
            if (oldState != newState) {
                onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
            }
        }
    }

    @CallSuper
    override fun onDeleted(
        @Suppress("InvalidNullabilityOverride") context: Context,
        @Suppress("InvalidNullabilityOverride") appWidgetIds: IntArray,
    ) {
        runAndLogExceptions {
            super.onDeleted(context, appWidgetIds)
            synchronized(lock) {
                for (id in appWidgetIds) {
                    lastOptionsCache.remove(id)
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        runAndLogExceptions {
            when (intent.action) {
                Intent.ACTION_LOCALE_CHANGED -> {
                    handleUpdateBroadcast(context, intent)
                }
                ACTION_DEBUG_UPDATE -> {
                    if ((context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                        handleUpdateBroadcast(context, intent)
                    }
                }
                else -> super.onReceive(context, intent)
            }
        }
    }

    private fun handleUpdateBroadcast(context: Context, intent: Intent) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, javaClass)
        val ids =
            intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ?: appWidgetManager.getAppWidgetIds(componentName)
        if (ids.isNotEmpty()) {
            onUpdate(context, appWidgetManager, ids)
        }
    }

    public companion object {
        private val lock = Any()
        private val lastOptionsCache: MutableIntObjectMap<WidgetOptionsState> =
            mutableIntObjectMapOf()

        @VisibleForTesting
        internal fun clearOptionsCache() {
            synchronized(lock) { lastOptionsCache.clear() }
        }

        /**
         * Broadcast action to force a debug update of the Glance Adaptive widget via adb: `adb
         * shell am broadcast -a androidx.glance.adaptive.action.DEBUG_UPDATE -n APP/COMPONENT`
         *
         * To target specific widget IDs, pass [AppWidgetManager.EXTRA_APPWIDGET_IDS]: `adb shell am
         * broadcast -a androidx.glance.adaptive.action.DEBUG_UPDATE -n APP/COMPONENT --eia
         * appWidgetIds 1,2`
         */
        public const val ACTION_DEBUG_UPDATE: String =
            "androidx.glance.adaptive.action.DEBUG_UPDATE"

        /**
         * Manifest `<meta-data>` name used to associate an [AppWidgetProvider] receiver with a
         * specific developer [widgetName] string identifier in `AndroidManifest.xml`.
         */
        public const val META_DATA_WIDGET_NAME: String = "androidx.glance.adaptive.WIDGET_NAME"

        /**
         * Key for storing the developer String instance identifier in [AppWidgetManager] widget
         * options bundle.
         */
        public const val EXTRA_WIDGET_ID: String = "androidx.glance.adaptive.WIDGET_ID"
    }
}

/**
 * Execute the block asynchronously in a scope with the lifetime of the broadcast.
 *
 * The coroutine scope finishes once the block returns, finishing the broadcast pending result.
 */
internal fun BroadcastReceiver.goAsync(
    coroutineContext: CoroutineContext = Dispatchers.Default,
    block: suspend CoroutineScope.() -> Unit,
) {
    val parentScope = CoroutineScope(coroutineContext + Job())
    val pendingResult = goAsync()

    parentScope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            // Regular cancellation, do not log as failure
        } catch (e: Throwable) {
            Log.e(TAG, "BroadcastReceiver execution failed", e)
        } finally {
            parentScope.cancel()
            try {
                pendingResult?.finish()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Error thrown when trying to finish broadcast", e)
            }
        }
    }
}

private inline fun runAndLogExceptions(block: () -> Unit) {
    try {
        block()
    } catch (ex: CancellationException) {
        // Regular cancellation, ignore
    } catch (ex: Exception) {
        Log.e(TAG, "Error in Glance Adaptive Widget Receiver", ex)
    }
}

private data class WidgetOptionsState(
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

    rawSizes.sortWith { a, b ->
        val widthCompare = a.width.compareTo(b.width)
        if (widthCompare != 0) widthCompare else a.height.compareTo(b.height)
    }
    return rawSizes
}
