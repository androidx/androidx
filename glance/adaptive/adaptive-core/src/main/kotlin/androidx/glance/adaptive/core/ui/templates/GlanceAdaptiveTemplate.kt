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

/** Base class for all Glance Adaptive templates. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class GlanceAdaptiveTemplate
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    /**
     * Stable integer identifier representing this template type for OS platform telemetry (e.g.,
     * `1` for [TrackTemplate], or [TEMPLATE_ID_UNKNOWN] (`0`) when unspecified).
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val templateId: Int = TEMPLATE_ID_UNKNOWN
) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /** Default unknown template identifier (`0`) when no template type is specified. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val TEMPLATE_ID_UNKNOWN: Int = 0
    }
}
