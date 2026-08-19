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

package androidx.glance.adaptive.core

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
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
 *
 *     class MyGlanceAdaptiveWidgetReceiver : GlanceAdaptiveWidgetReceiver() {
 *         override val widgetId: String = "my_widget_id"
 *         override suspend fun onUpdate(context: Context) {
 *             // Provide or push updated template data
 *         }
 *     }
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
    public abstract val widgetId: String

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
        goAsync(coroutineContext) { onUpdate(context) }
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
