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

import android.app.PendingIntent
import androidx.glance.adaptive.core.GlanceAdaptiveWidgetDelegate
import androidx.glance.adaptive.core.GlanceAdaptiveWidgetManager
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate

/**
 * A [GlanceAdaptiveWidgetDelegate] for AppWidget hosts, adding the operations that only AppWidget
 * hosts support.
 *
 * Callers reach these operations through extension functions on [GlanceAdaptiveWidgetManager]
 * declared in this artifact, so only modules depending on it can call them. The extensions are
 * no-ops for a manager whose delegate does not implement this interface, so every AppWidget
 * delegate must implement it.
 */
internal interface GlanceAdaptiveAppWidgetDelegate : GlanceAdaptiveWidgetDelegate {
    /**
     * Sets or updates dynamic preview data rendered in host widget pickers for [widgetName].
     *
     * @param widgetName The developer-defined identifier of the widget definition.
     * @param previewData The declarative template data payload to render as a preview.
     */
    suspend fun setPreview(widgetName: String, previewData: AdaptiveGlanceTemplate)

    /**
     * Requests that the launcher pin [widgetName] to the home screen.
     *
     * @param widgetName The developer-defined identifier of the widget definition.
     * @param initialData The declarative template data payload to render as the preview shown while
     *   the launcher asks the user to confirm, if any.
     * @param successCallback The [PendingIntent] to send once the widget is pinned, if any.
     * @return true if the request was sent to the launcher, false otherwise.
     */
    suspend fun requestPin(
        widgetName: String,
        initialData: AdaptiveGlanceTemplate?,
        successCallback: PendingIntent?,
    ): Boolean
}
