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

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate

/** Entry point for managing Glance Adaptive widgets. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GlanceAdaptiveWidgetManager(
    @get:VisibleForTesting
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val delegate: GlanceAdaptiveWidgetDelegate
) {

    /**
     * Pushes live template data updates to all placed instances matching [widgetName].
     *
     * @param widgetName Developer widget definition String identifier matching
     *   [androidx.glance.adaptive.appwidget.GlanceAdaptiveWidgetReceiver.widgetName].
     * @param currentData The declarative template data payload implementing
     *   [AdaptiveGlanceTemplate].
     */
    public suspend fun pushUpdate(widgetName: String, currentData: AdaptiveGlanceTemplate) {
        delegate.pushUpdate(widgetName = widgetName, currentData = currentData, widgetIds = null)
    }

    /**
     * Pushes live template data updates to a specific collection of target widget instance String
     * identifiers.
     *
     * @param widgetName Developer widget definition String identifier matching
     *   [androidx.glance.adaptive.appwidget.GlanceAdaptiveWidgetReceiver.widgetName].
     * @param currentData The declarative template data payload implementing
     *   [AdaptiveGlanceTemplate].
     * @param widgetIds Collection of target developer widget instance String identifiers to update.
     *   Passing an explicit empty collection restricts updates strictly to zero instances.
     */
    public suspend fun pushUpdate(
        widgetName: String,
        currentData: AdaptiveGlanceTemplate,
        widgetIds: Set<String>,
    ) {
        delegate.pushUpdate(
            widgetName = widgetName,
            currentData = currentData,
            widgetIds = widgetIds,
        )
    }

    /**
     * Pushes live template data updates to a single target widget instance String identifier.
     *
     * @param widgetName Developer widget definition String identifier matching
     *   [androidx.glance.adaptive.appwidget.GlanceAdaptiveWidgetReceiver.widgetName].
     * @param currentData The declarative template data payload implementing
     *   [AdaptiveGlanceTemplate].
     * @param widgetId Single target developer widget instance String identifier to update.
     */
    public suspend fun pushUpdate(
        widgetName: String,
        currentData: AdaptiveGlanceTemplate,
        widgetId: String,
    ) {
        delegate.pushUpdate(
            widgetName = widgetName,
            currentData = currentData,
            widgetIds = setOf(widgetId),
        )
    }

    /**
     * Sets dynamic preview data rendered in host widget pickers for the specified widget
     * definition.
     *
     * On devices running Android 14 and earlier (pre-API 35), dynamic widget previews are not
     * supported by the platform and this operation completes as a safe no-op.
     *
     * @param widgetName Developer widget definition String identifier matching
     *   [androidx.glance.adaptive.appwidget.GlanceAdaptiveWidgetReceiver.widgetName].
     * @param previewData Declarative template data payload implementing [AdaptiveGlanceTemplate] to
     *   render as a preview.
     */
    public suspend fun setPreview(widgetName: String, previewData: AdaptiveGlanceTemplate) {
        delegate.setPreview(widgetName = widgetName, previewData = previewData)
    }
}
