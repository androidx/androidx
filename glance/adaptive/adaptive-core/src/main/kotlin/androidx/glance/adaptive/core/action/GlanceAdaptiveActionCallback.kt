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

package androidx.glance.adaptive.core.action

import android.content.Context
import android.os.Bundle

/**
 * Handles interactive widget clicks in the background.
 *
 * Implementations must have a public zero-argument constructor so the runtime can instantiate them
 * when a [GlanceAdaptiveAction.CallbackAction] triggers.
 *
 * Implement [onAction] to handle the user interaction and, if the widget UI needs to reflect the
 * new state, call [androidx.glance.adaptive.core.GlanceAdaptiveWidgetManager.pushUpdate].
 *
 * [onAction] runs on a background thread while the click broadcast is handled, which the system
 * gives 10 seconds to finish. If [onAction] has not returned after 9 seconds, it is cancelled and
 * an error is logged. Cancellation can only stop suspending calls, so avoid blocking ones. Hand off
 * longer work, for example to a foreground service or a job.
 *
 * Exceptions thrown by [onAction] are not caught, as in a plain
 * [android.content.BroadcastReceiver]: they crash the app. Catch the ones you expect, such as
 * network errors. A [kotlinx.coroutines.CancellationException], such as from an expired
 * `withTimeout`, only ends the call, without being reported.
 *
 * @see GlanceAdaptiveAction.CallbackAction
 * @see androidx.glance.adaptive.core.GlanceAdaptiveWidgetManager.pushUpdate
 */
public interface GlanceAdaptiveActionCallback {

    /**
     * Called on a background coroutine when the user triggers a
     * [GlanceAdaptiveAction.CallbackAction].
     *
     * @param context application or broadcast [Context] executing the callback.
     * @param widgetName developer widget definition String identifier matching
     *   `GlanceAdaptiveWidgetReceiver.widgetName`.
     * @param widgetId widget instance String identifier of the clicked widget instance (matching
     *   [androidx.glance.adaptive.core.WidgetInstanceInfo.widgetId]), or `null` if unknown.
     * @param parameters key-value arguments supplied to [GlanceAdaptiveAction.CallbackAction].
     */
    public suspend fun onAction(
        context: Context,
        widgetName: String,
        widgetId: String?,
        parameters: Bundle,
    )
}
