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

/**
 * Common interface representing a host display surface on which Glance Adaptive widgets can be
 * placed.
 *
 * Specific surfaces are defined by host modules (e.g. `adaptive-appwidget`, `adaptive-wear`).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface GlanceSurface {
    /** Unique canonical identifier for this surface (e.g. "home_screen", "wear_tile"). */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val tag: String

    public companion object {
        /**
         * Creates a lightweight [GlanceSurface] instance for testing or dynamic host registration.
         *
         * @param tag The unique string identifier for the surface.
         * @return A [GlanceSurface] instance identified by [tag].
         */
        public fun of(tag: String): GlanceSurface = NamedGlanceSurface(tag)
    }
}

private data class NamedGlanceSurface(override val tag: String) : GlanceSurface

/**
 * Host placement constraints capturing the physical container size and target display surface.
 *
 * @param dimensions Bounding size in DP.
 * @param surface Host target surface, or `null` if unspecified.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HostConstraints(
    public val dimensions: Dimensions,
    public val surface: GlanceSurface? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HostConstraints) return false
        return dimensions == other.dimensions && surface == other.surface
    }

    override fun hashCode(): Int = 31 * dimensions.hashCode() + (surface?.hashCode() ?: 0)

    override fun toString(): String = "HostConstraints(dimensions=$dimensions, surface=$surface)"
}
