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
import androidx.glance.adaptive.core.templates.AdaptiveGlanceTemplate

/** Internal interface abstracting underlying framework vs compat widget operations. */
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
     * Sets or updates dynamic preview data rendered in host widget pickers for [widgetName].
     *
     * @param widgetName The developer-defined identifier of the widget definition.
     * @param previewData The declarative template data payload to render as a preview.
     */
    public suspend fun setPreview(widgetName: String, previewData: AdaptiveGlanceTemplate)
}
