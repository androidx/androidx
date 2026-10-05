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
 * Internal interface abstracting the surface-specific implementations of widget operations.
 *
 * Only operations supported by every surface belong here. Operations specific to one surface, such
 * as setting dynamic widget picker previews on phones, are implemented by that surface's delegate
 * and exposed by the surface artifact as extension functions on [GlanceAdaptiveWidgetManager].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface GlanceAdaptiveWidgetDelegate {
    /**
     * Pushes template data updates to target widget placements.
     *
     * @param widgetName The developer-defined identifier of the widget definition.
     * @param currentData The declarative template data payload to render.
     * @param widgetIds Optional collection of developer target widget instance String identifiers.
     *   If omitted (`null`), updates broadcast to all instances matching [widgetName]. If an empty
     *   collection is passed, no widgets will be updated.
     */
    public suspend fun pushUpdate(
        widgetName: String,
        currentData: AdaptiveGlanceTemplate,
        widgetIds: Set<String>? = null,
    )

    /**
     * Returns the active widget instances matching [widgetName] currently placed on host surfaces
     * of this device.
     *
     * @param widgetName The developer-defined identifier of the widget definition.
     */
    public suspend fun getActiveInstances(widgetName: String): List<WidgetInstanceInfo>
}
