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

package androidx.glance.adaptive.appwidget.ui.templates

import androidx.annotation.RestrictTo
import androidx.glance.adaptive.core.ui.selection.WidthTier
import androidx.glance.adaptive.core.ui.templates.TrackTemplate

/**
 * Layout archetypes for [TrackTemplate] on platform AppWidget surfaces.
 *
 * Resolved by [TrackSizeSelector] and consumed by [TrackTemplateRenderer].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class TrackArchetype {
    /** Width-constrained layout for [WidthTier.W1] containers. */
    THIN,

    /** Default layout for [WidthTier.W2] and wider containers. */
    STANDARD,
}
