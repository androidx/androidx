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

package androidx.glance.adaptive.core.ui.selection

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting

/**
 * Common base class representing a host display surface on which Glance Adaptive widgets can be
 * placed.
 *
 * Specific surfaces are defined by host modules (e.g. `adaptive-appwidget`, `adaptive-wear`).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class GlanceSurface
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    /** Unique canonical identifier for this surface (e.g. "home_screen", "wear_widget"). */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val tag: String
) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is GlanceSurface && tag == other.tag)

    override fun hashCode(): Int = tag.hashCode()

    override fun toString(): String = "GlanceSurface($tag)"

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /**
         * Creates a lightweight [GlanceSurface] instance for testing.
         *
         * @param tag The unique string identifier for the surface.
         * @return A [GlanceSurface] instance identified by [tag].
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @VisibleForTesting
        public fun fromTag(tag: String): GlanceSurface = NamedGlanceSurface(tag)
    }
}

private class NamedGlanceSurface(tag: String) : GlanceSurface(tag)
