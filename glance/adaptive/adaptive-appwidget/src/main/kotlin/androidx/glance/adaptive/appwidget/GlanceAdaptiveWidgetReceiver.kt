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
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// Tag length must not exceed 23 characters for pre-API 24 log compatibility.
private const val TAG = "GlanceAdaptiveReceiver"

/**
 * [AppWidgetProvider] using Glance Adaptive to process widget lifecycle events and updates.
 *
 * Subclass this receiver to associate a widget declared in `AndroidManifest.xml` with a
 * [widgetName]. Rather than providing a custom `GlanceAppWidget` composable layout, applications
 * push [androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate] data via
 * [androidx.glance.adaptive.core.GlanceAdaptiveWidgetManager.pushUpdate], and the library adapts
 * and renders the layout for each target surface.
 *
 * This should typically be used as:
 * ```kotlin
 * class MyGlanceAdaptiveWidgetReceiver : GlanceAdaptiveWidgetReceiver() {
 *     override val widgetName: String = "my_widget"
 *     override suspend fun onUpdate(context: Context) {
 *         GlanceAdaptiveWidgetManager(context).pushUpdate(
 *             widgetName = widgetName,
 *             currentData = ...
 *         )
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
public abstract class GlanceAdaptiveWidgetReceiver : AppWidgetProvider() {

    /**
     * Unique developer-defined identifier used to associate this receiver with a specific widget
     * definition (e.g., "my_widget").
     *
     * This identifier should correspond to the widget template definition or layout configuration
     * registered with the adaptive widget framework.
     *
     * It is read from an instance created with the receiver's no-argument constructor, so it must
     * return a constant value. It must be unique per app. Every operation that takes a `widgetName`
     * throws [IllegalArgumentException] if no receiver, or more than one receiver, declares it, and
     * [IllegalStateException] if a receiver of the app cannot be instantiated to read it. Disabled
     * receivers are included.
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
     * Called on a background coroutine when the system requests an update for this widget (such as
     * on initial placement from the widget picker, device boot, locale or size changes, or periodic
     * update alarms).
     *
     * Implement this method to fetch your widget data and call
     * [androidx.glance.adaptive.core.GlanceAdaptiveWidgetManager.pushUpdate].
     *
     * Exceptions thrown by this method are not caught, exactly like exceptions thrown by a plain
     * [AppWidgetProvider]: once the broadcast is finished, they reach the
     * [kotlinx.coroutines.CoroutineExceptionHandler] of [coroutineContext] if it has one, and
     * otherwise crash the app. A [kotlinx.coroutines.CancellationException], such as from an
     * expired `withTimeout`, only ends the call, without being reported. It is called even if the
     * library fails to assign a widgetId to a new widget, as `pushUpdate` with `widgetIds = null`
     * still updates that widget. The library logs such failures and retries on the next update of
     * the widget.
     *
     * @param context The application Context.
     */
    public abstract suspend fun onUpdate(context: Context)

    // AppWidgetProvider in the platform SDK leaves these parameters unannotated for
    // backwards source-compatibility with existing Kotlin subclasses that override them
    // with nullable types. Since GlanceAdaptiveWidgetReceiver is a new Kotlin base class,
    // we expose non-null parameters to match GlanceAppWidgetReceiver and suppress Metalava's
    // InvalidNullabilityOverride check.
    @CallSuper
    override fun onUpdate(
        @Suppress("InvalidNullabilityOverride") context: Context,
        @Suppress("InvalidNullabilityOverride") appWidgetManager: AppWidgetManager,
        @Suppress("InvalidNullabilityOverride") appWidgetIds: IntArray,
    ) {
        initializeWidgetOptionsAndIds(appWidgetManager, appWidgetIds)
        // The receiver Context is a ReceiverRestrictedContext, which dependency injection
        // frameworks such as Hilt reject. Pass the application Context, as documented on
        // onUpdate(Context).
        val appContext = context.applicationContext ?: context
        goAsyncUncaught(coroutineContext) { onUpdate(appContext) }
    }

    /**
     * Assigns a widgetId to each widget of [appWidgetIds] that has none yet, and caches its
     * options.
     *
     * Failures are logged rather than thrown, as no caller can handle them, and retried on the next
     * update or options change of the widget.
     */
    private fun initializeWidgetOptionsAndIds(
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (id in appWidgetIds) {
            // Checked by widgetId rather than by presence in the cache, so that a widget cached
            // after its widgetId failed to be stored is retried.
            if (WidgetOptionsCache.getCachedWidgetId(id) != null) continue
            try {
                val rawOptions = appWidgetManager.getAppWidgetOptions(id)
                val options =
                    ensureWidgetIdIsPresent(
                        appWidgetManager,
                        id,
                        rawOptions ?: Bundle(),
                        checkManagerIfMissing = false,
                    )
                WidgetOptionsCache.cacheWidgetOptionsIfMissing(id, options)
            } catch (e: Exception) {
                logWidgetIdAssignmentFailure(id, e)
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
        val existingWidgetId =
            WidgetOptionsCache.getCachedWidgetId(appWidgetId)
                ?: appWidgetManager
                    .takeIf { checkManagerIfMissing }
                    ?.getAppWidgetOptions(appWidgetId)
                    ?.getString(EXTRA_WIDGET_ID)
                    ?.takeUnless { it.isBlank() }
        if (existingWidgetId != null) {
            return Bundle(options).apply { putString(EXTRA_WIDGET_ID, existingWidgetId) }
        }
        val generatedWidgetId = WidgetOptionsCache.generateWidgetId()
        return Bundle(options)
            .apply { putString(EXTRA_WIDGET_ID, generatedWidgetId) }
            .also { appWidgetManager.updateAppWidgetOptions(appWidgetId, it) }
    }

    private fun logWidgetIdAssignmentFailure(appWidgetId: Int, e: Exception) {
        Log.e(
            TAG,
            "Cannot assign a widgetId to appWidgetId $appWidgetId of ${javaClass.name}, " +
                "retrying on its next update",
            e,
        )
    }

    @CallSuper
    override fun onAppWidgetOptionsChanged(
        @Suppress("InvalidNullabilityOverride") context: Context,
        @Suppress("InvalidNullabilityOverride") appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        @Suppress("InvalidNullabilityOverride") newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val options =
            try {
                ensureWidgetIdIsPresent(appWidgetManager, appWidgetId, newOptions)
            } catch (e: Exception) {
                // Cached without a widgetId below, so that the update this triggers, or the next
                // one, retries.
                logWidgetIdAssignmentFailure(appWidgetId, e)
                newOptions
            }
        // A broadcast queued before a library write from outside this receiver (pin callback,
        // configuration) must not roll back the widgetId that write stored.
        val updateNeeded =
            WidgetOptionsCache.putFromBroadcast(appWidgetId, WidgetOptionsState.from(options))
        if (updateNeeded) {
            onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
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
    override fun onDeleted(
        @Suppress("InvalidNullabilityOverride") context: Context,
        @Suppress("InvalidNullabilityOverride") appWidgetIds: IntArray,
    ) {
        // Not caught: only in-memory bookkeeping, which has no failure to recover from.
        super.onDeleted(context, appWidgetIds)
        WidgetOptionsCache.remove(appWidgetIds)
    }

    @CallSuper
    override fun onRestored(
        @Suppress("InvalidNullabilityOverride") context: Context,
        @Suppress("InvalidNullabilityOverride") oldWidgetIds: IntArray,
        @Suppress("InvalidNullabilityOverride") newWidgetIds: IntArray,
    ) {
        // Not caught: only in-memory bookkeeping, which has no failure to recover from.
        super.onRestored(context, oldWidgetIds, newWidgetIds)
        WidgetOptionsCache.restore(oldWidgetIds, newWidgetIds)
    }

    override fun onReceive(
        @Suppress("InvalidNullabilityOverride") context: Context,
        @Suppress("InvalidNullabilityOverride") intent: Intent,
    ) {
        // Not caught, as this calls the callbacks of the developer, whose exceptions must surface
        // like in a plain AppWidgetProvider.
        when (intent.action) {
            Intent.ACTION_LOCALE_CHANGED -> {
                handleUpdateBroadcast(context, intent)
            }
            ACTION_DEBUG_UPDATE -> {
                if ((context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                    handleUpdateBroadcast(context, intent)
                }
            }
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                consumePendingRenders(intent)
                super.onReceive(context, intent)
            }
            else -> super.onReceive(context, intent)
        }
    }

    /**
     * Clears the re-render markers that the update [intent] carries the token of, if it was sent
     * after a library write from outside this receiver, see [updateOptionsAndRerender]. The widgets
     * are updated by [onUpdate] either way.
     */
    private fun consumePendingRenders(intent: Intent) {
        val token = intent.getStringExtra(WidgetOptionsCache.EXTRA_RENDER_TOKEN) ?: return
        intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)?.forEach {
            WidgetOptionsCache.consumePendingRender(it, token)
        }
    }

    private fun handleUpdateBroadcast(context: Context, intent: Intent) {
        val appWidgetManager: AppWidgetManager
        val ids: IntArray
        try {
            appWidgetManager = AppWidgetManager.getInstance(context)
            ids =
                intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                    ?: appWidgetManager.getAppWidgetIds(ComponentName(context, javaClass))
        } catch (e: Exception) {
            // The widgets are still updated by their next update.
            Log.e(TAG, "Cannot list the widgets of ${javaClass.name} for ${intent.action}", e)
            return
        }
        if (ids.isNotEmpty()) {
            onUpdate(context, appWidgetManager, ids)
        }
    }

    public companion object {
        /**
         * Broadcast action to force a debug update of the Glance Adaptive widget via adb: `adb
         * shell am broadcast -a androidx.glance.adaptive.appwidget.action.DEBUG_UPDATE -n
         * APP/COMPONENT`
         *
         * To target specific widget IDs, pass [AppWidgetManager.EXTRA_APPWIDGET_IDS]: `adb shell am
         * broadcast -a androidx.glance.adaptive.appwidget.action.DEBUG_UPDATE -n APP/COMPONENT
         * --eia appWidgetIds 1,2`
         */
        public const val ACTION_DEBUG_UPDATE: String =
            "androidx.glance.adaptive.appwidget.action.DEBUG_UPDATE"

        /**
         * Key for storing the widget instance identifier in the [AppWidgetManager] widget options
         * bundle.
         */
        public const val EXTRA_WIDGET_ID: String =
            "androidx.glance.adaptive.appwidget.extra.WIDGET_ID"
    }
}

/**
 * Executes the developer's [block] asynchronously in a scope with the lifetime of the broadcast.
 *
 * Exceptions thrown by [block] are not caught: once the broadcast pending result is finished, they
 * reach the [kotlinx.coroutines.CoroutineExceptionHandler] of [coroutineContext] if it has one, and
 * otherwise the uncaught exception handler of the thread, which crashes the app, exactly like an
 * exception thrown by a plain [AppWidgetProvider]. A [CancellationException] is not an error, so it
 * is not reported.
 */
private fun BroadcastReceiver.goAsyncUncaught(
    coroutineContext: CoroutineContext,
    block: suspend () -> Unit,
) {
    val pendingResult = goAsync()
    CoroutineScope(coroutineContext + Job()).launch {
        try {
            block()
        } finally {
            try {
                pendingResult?.finish()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Error thrown when trying to finish broadcast", e)
            }
        }
    }
}
