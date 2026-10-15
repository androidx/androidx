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

package androidx.glance.adaptive.core.ui.action

import android.content.Context
import android.os.Bundle
import androidx.annotation.RestrictTo

/**
 * Handles interactive widget clicks in the background.
 *
 * Implementations must have a public zero-argument constructor so the runtime can instantiate them
 * when a [GlanceWidgetAction.Callback] triggers.
 *
 * [onAction] runs on a background thread within the lifetime of the click broadcast, so it must
 * finish within a few seconds (the system limit is about 10 seconds). Hand off longer work, for
 * example to a foreground service or a job.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface GlanceAdaptiveActionCallback {

    /**
     * Runs background work for a clicked widget element.
     *
     * @param context application or broadcast [Context] executing the callback
     * @param widgetName developer identifier of the clicked widget definition
     * @param widgetId developer instance identifier of the clicked placement, or `null` if unknown
     * @param parameters caller-supplied key-value arguments from [GlanceWidgetAction.Callback]
     */
    public suspend fun onAction(
        context: Context,
        widgetName: String,
        widgetId: String?,
        parameters: Bundle,
    )
}
