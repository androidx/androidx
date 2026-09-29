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
     * Unique semantic identifier of the template for telemetry and registry lookups (e.g.,
     * "list_template_v1").
     *
     * Implementations must provide a stable constant string identifier that is unique across all
     * templates within the application to ensure human-readable telemetry, accurate data
     * partitioning, and no identifier collisions under R8/ProGuard class minification.
     */
    public val templateId: String
}
