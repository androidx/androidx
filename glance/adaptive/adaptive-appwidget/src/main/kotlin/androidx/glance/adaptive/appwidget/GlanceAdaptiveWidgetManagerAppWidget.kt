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
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.glance.adaptive.core.GlanceAdaptiveWidgetManager
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate

/**
 * Creates a [GlanceAdaptiveWidgetManager] configured with the platform AppWidget delegate for phone
 * widget operations.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun GlanceAdaptiveWidgetManager(context: Context): GlanceAdaptiveWidgetManager =
    GlanceAdaptiveWidgetManager(BaseWidgetDelegate(context))

/**
 * Sets dynamic preview data rendered in host widget pickers for the specified widget definition.
 *
 * Dynamic previews are only supported by AppWidget hosts, so this operation is provided by this
 * artifact as an extension rather than by [GlanceAdaptiveWidgetManager] itself.
 *
 * On devices running Android 14 and earlier (pre-API 35), dynamic widget previews are not supported
 * by the platform and this operation completes as a safe no-op. It is also a no-op if this manager
 * is not backed by an AppWidget delegate, for example if it was created for another surface.
 *
 * @param widgetName Developer widget definition String identifier matching
 *   [GlanceAdaptiveWidgetReceiver.widgetName].
 * @param previewData Declarative template data payload implementing [AdaptiveGlanceTemplate] to
 *   render as a preview.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public suspend fun GlanceAdaptiveWidgetManager.setPreview(
    widgetName: String,
    previewData: AdaptiveGlanceTemplate,
) {
    // A local copy, as Kotlin can't smart cast a property declared in another module.
    val delegate = delegate
    if (delegate !is GlanceAdaptiveAppWidgetDelegate) {
        Log.w(TAG, "Ignoring setPreview for widgetName $widgetName: no AppWidget delegate")
        return
    }
    delegate.setPreview(widgetName = widgetName, previewData = previewData)
}

/**
 * Requests that the launcher pin the specified widget definition to the home screen.
 *
 * It is up to the launcher to accept the request, typically after asking the user to confirm, so
 * `true` only means that the request was sent. [successCallback] is sent once the widget is pinned.
 *
 * Pinning is only supported by AppWidget hosts, so this operation is provided by this artifact as
 * an extension rather than by [GlanceAdaptiveWidgetManager] itself.
 *
 * On devices running Android 7.1 and earlier (pre-API 26), pinning is not supported by the platform
 * and this returns `false`. It also returns `false` if the launcher does not support pinning, if no
 * receiver matching [widgetName] can be placed on the home screen, or if this manager is not backed
 * by an AppWidget delegate.
 *
 * Widget names should be unique per app. If several receivers match [widgetName], a warning is
 * logged and the first one that can be placed on the home screen is pinned.
 *
 * @param widgetName Developer widget definition String identifier matching
 *   [GlanceAdaptiveWidgetReceiver.widgetName].
 * @param widgetId Optional developer-assigned widget instance String identifier for the pinned
 *   widget. If omitted or blank, the [GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID] in [options] is
 *   used, or a unique identifier is generated if there is none.
 * @param initialData Optional declarative template data payload implementing
 *   [AdaptiveGlanceTemplate] to render as the preview shown while the launcher asks the user to
 *   confirm. If omitted, the launcher shows the preview declared by the widget instead.
 * @param options Optional initial configuration options to store in the options of the pinned
 *   widget.
 * @param successCallback Optional [PendingIntent] sent once the widget is pinned and [widgetId] and
 *   [options] are stored.
 * @return `true` if the request was sent to the launcher, `false` otherwise.
 * @throws IllegalStateException if the app has no foreground activity or foreground service.
 * @see android.appwidget.AppWidgetManager.requestPinAppWidget
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public suspend fun GlanceAdaptiveWidgetManager.requestPin(
    widgetName: String,
    widgetId: String? = null,
    initialData: AdaptiveGlanceTemplate? = null,
    options: Bundle = Bundle.EMPTY,
    successCallback: PendingIntent? = null,
): Boolean {
    // A local copy, as Kotlin can't smart cast a property declared in another module.
    val delegate = delegate
    if (delegate !is GlanceAdaptiveAppWidgetDelegate) {
        Log.w(TAG, "Ignoring requestPin for widgetName $widgetName: no AppWidget delegate")
        return false
    }
    return delegate.requestPin(
        widgetName = widgetName,
        widgetId = widgetId,
        initialData = initialData,
        options = options,
        successCallback = successCallback,
    )
}

private const val TAG = "GlanceAdaptiveAppWidget"
