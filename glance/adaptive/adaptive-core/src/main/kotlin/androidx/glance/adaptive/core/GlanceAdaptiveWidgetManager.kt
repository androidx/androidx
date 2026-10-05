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
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate

/**
 * Entry point for managing Glance Adaptive widgets.
 *
 * Only operations supported by every surface are members of this class. Surface-specific
 * operations, such as setting dynamic widget picker previews on phones, are provided as extension
 * functions by the corresponding surface artifact (for example `adaptive-appwidget`).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GlanceAdaptiveWidgetManager(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val delegate: GlanceAdaptiveWidgetDelegate
) {

    /**
     * Pushes live template data updates to all placed instances matching [widgetName].
     *
     * @param widgetName Developer widget definition String identifier matching
     *   `GlanceAdaptiveWidgetReceiver.widgetName`.
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
     *   `GlanceAdaptiveWidgetReceiver.widgetName`.
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
     *   `GlanceAdaptiveWidgetReceiver.widgetName`.
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
     * Returns the active widget instances matching [widgetName] currently placed on host surfaces
     * of this device.
     *
     * Instances are keyed by their widget instance String identifier. A single identifier can be
     * placed more than once, so each returned [WidgetInstanceInfo] reports how many placements it
     * has on each host surface through [WidgetInstanceInfo.surfacePlacements].
     *
     * Placements that have not yet been assigned a widget instance String identifier are omitted,
     * as they cannot be targeted by [pushUpdate].
     *
     * @param widgetName Developer widget definition String identifier matching
     *   `GlanceAdaptiveWidgetReceiver.widgetName`.
     * @return Active instances for [widgetName], or an empty list if none are placed.
     * @throws RuntimeException if the options of any placement cannot be read, rather than
     *   returning a partial result.
     */
    public suspend fun getActiveInstances(widgetName: String): List<WidgetInstanceInfo> =
        delegate.getActiveInstances(widgetName)
}
