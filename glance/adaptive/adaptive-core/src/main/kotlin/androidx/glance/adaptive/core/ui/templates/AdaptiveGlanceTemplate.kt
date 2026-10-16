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

package androidx.glance.adaptive.core.ui.templates

import androidx.annotation.RestrictTo

/** Base interface for all Glance Adaptive templates. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface AdaptiveGlanceTemplate {
    /**
     * Unique semantic name of the template for human-readable identification (e.g.,
     * "list_template_v1").
     *
     * Implementations must provide a stable constant string name that is unique across all
     * templates within the application to avoid collisions under R8/ProGuard class minification.
     */
    // AdaptiveGlanceTemplate is only implemented by library-group templates, not by external
    // callers. Keeping `templateName` and `templateId` restricted without default getters triggers
    // Metalava's HiddenAbstractMethodInInterface check, which we suppress here.
    @get:Suppress("HiddenAbstractMethodInInterface")
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val templateName: String

    /**
     * Stable integer identifier representing this template type for OS platform telemetry (e.g.,
     * `1` for [TrackTemplate], or [TEMPLATE_ID_UNKNOWN] (`0`) when unspecified).
     */
    @get:Suppress("HiddenAbstractMethodInInterface")
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val templateId: Int

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /** Default unknown template identifier (`0`) when no template type is specified. */
        public const val TEMPLATE_ID_UNKNOWN: Int = 0
    }
}
