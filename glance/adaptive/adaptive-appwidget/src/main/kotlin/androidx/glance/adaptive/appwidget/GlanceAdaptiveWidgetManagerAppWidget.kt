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

import android.content.Context
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

private const val TAG = "GlanceAdaptiveAppWidget"
