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

import android.app.Activity
import android.app.PendingIntent
import android.os.Bundle
import androidx.glance.adaptive.core.GlanceAdaptiveWidgetDelegate
import androidx.glance.adaptive.core.GlanceAdaptiveWidgetManager
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate

/**
 * A [GlanceAdaptiveWidgetDelegate] for AppWidget hosts, adding the operations that only AppWidget
 * hosts support.
 *
 * Callers reach these operations through extension functions on [GlanceAdaptiveWidgetManager]
 * declared in this artifact, so only modules depending on it can call them. Every AppWidget
 * delegate must implement this interface: for any other delegate, the extensions are no-ops, except
 * `setConfigurationResult`, which throws.
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
     * @param widgetId The widget instance String identifier to assign to the pinned widget, or null
     *   to use the one in [options], or a generated one if there is none.
     * @param initialData The declarative template data payload to render as the preview shown while
     *   the launcher asks the user to confirm, if any.
     * @param options The configuration options to store in the options of the pinned widget.
     * @param successCallback The [PendingIntent] to send once the widget is pinned, if any.
     * @return true if the request was sent to the launcher, false otherwise.
     */
    suspend fun requestPin(
        widgetName: String,
        widgetId: String?,
        initialData: AdaptiveGlanceTemplate?,
        options: Bundle,
        successCallback: PendingIntent?,
    ): Boolean

    /**
     * Completes the configuration of the widget being configured by [activity], storing [widgetId]
     * and [options] in its options, then setting the [Activity.RESULT_OK] result expected by the
     * host. The result is not set if this throws. This is a blocking call that returns only once
     * the options are stored and the result is set.
     *
     * @param activity The configuration [Activity] started with
     *   [android.appwidget.AppWidgetManager.ACTION_APPWIDGET_CONFIGURE].
     * @param widgetId The widget instance String identifier to assign to the configured widget, or
     *   null to use the one in [options] or already stored for the widget, or a generated one if
     *   there is none.
     * @param options The configuration options to store in the options of the configured widget.
     * @throws IllegalArgumentException if [activity]'s intent does not carry a valid
     *   [android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID].
     */
    fun setConfigurationResult(activity: Activity, widgetId: String?, options: Bundle)
}
