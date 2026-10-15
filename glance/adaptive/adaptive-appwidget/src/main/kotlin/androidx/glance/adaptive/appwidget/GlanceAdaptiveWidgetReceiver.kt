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
import android.os.Bundle
import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.RestrictTo
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
 * Include the [Intent.ACTION_LOCALE_CHANGED] action in the receiver declaration in
 * `AndroidManifest.xml` so that widgets are updated when the device locale changes:
 * ```xml
 * <receiver android:name=".MyGlanceAdaptiveWidgetReceiver" android:exported="true">
 *     <intent-filter>
 *         <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
 *         <action android:name="android.intent.action.LOCALE_CHANGED" />
 *     </intent-filter>
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
     *
     * This is the single source of truth for the widget name of this receiver. It is read from an
     * instance created with the receiver's no-argument constructor, so it must return a constant
     * value.
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
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        runAndLogExceptions {
            initializeWidgetOptionsAndIds(appWidgetManager, appWidgetIds)
            goAsync(coroutineContext) { onUpdate(context) }
        }
    }

    private fun initializeWidgetOptionsAndIds(
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (id in appWidgetIds) {
            if (!WidgetOptionsCache.contains(id)) {
                val rawOptions = appWidgetManager.getAppWidgetOptions(id)
                val options =
                    ensureWidgetIdIsPresent(
                        appWidgetManager,
                        id,
                        rawOptions ?: Bundle(),
                        checkManagerIfMissing = false,
                    )
                WidgetOptionsCache.cacheWidgetOptionsIfMissing(id, options)
            }
        }
    }

    // Ensures a non-blank EXTRA_WIDGET_ID is present in options, retrieving it from memory cache
    // or AppWidgetManager, or falling back to a generated default ID. If checkManagerIfMissing is
    // false, skipping querying AppWidgetManager avoids a redundant IPC call when options was
    // already retrieved directly from AppWidgetManager.
    private fun ensureWidgetIdIsPresent(
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        options: Bundle,
        checkManagerIfMissing: Boolean = true,
    ): Bundle {
        if (!options.getString(EXTRA_WIDGET_ID).isNullOrBlank()) {
            return options
        }
        val widgetId =
            WidgetOptionsCache.getCachedWidgetId(appWidgetId)
                ?: appWidgetManager
                    .takeIf { checkManagerIfMissing }
                    ?.getAppWidgetOptions(appWidgetId)
                    ?.getString(EXTRA_WIDGET_ID)
                    ?.takeUnless { it.isBlank() }
                ?: WidgetOptionsCache.generateWidgetId()
        return Bundle(options)
            .apply { putString(EXTRA_WIDGET_ID, widgetId) }
            .also { appWidgetManager.updateAppWidgetOptions(appWidgetId, it) }
    }

    @CallSuper
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        runAndLogExceptions {
            super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
            val options = ensureWidgetIdIsPresent(appWidgetManager, appWidgetId, newOptions)
            val newState = WidgetOptionsState.from(options)
            val oldState = WidgetOptionsCache.put(appWidgetId, newState)
            if (oldState != newState) {
                onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
            }
        }
    }

    /**
     * Called when widget instances are deleted. Removes cached widget options associated with the
     * deleted [appWidgetIds] from the internal cache.
     *
     * If this method is overridden, `super.onDeleted` must be called to ensure internal cache
     * cleanup.
     */
    @CallSuper
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        runAndLogExceptions {
            super.onDeleted(context, appWidgetIds)
            WidgetOptionsCache.remove(appWidgetIds)
        }
    }

    @CallSuper
    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        runAndLogExceptions {
            super.onRestored(context, oldWidgetIds, newWidgetIds)
            WidgetOptionsCache.restore(oldWidgetIds, newWidgetIds)
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
