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
 * Host placement constraints capturing the physical container size and the target display surface.
 *
 * Pairing the two is deliberate: breakpoints are surface-dependent (the same width resolves to a
 * different [WidthTier] on a home screen than on a lock screen), so passing [dimensions] without
 * its [surface] is meaningless and the two must never drift apart.
 *
 * [S] is covariant, so a `HostConstraints<AppWidgetGlanceSurface>` is usable wherever a
 * `HostConstraints<GlanceSurface>` is expected.
 *
 * @param dimensions Bounding size in DP.
 * @param surface Host target surface.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HostConstraints<out S : GlanceSurface>(
    public val dimensions: Dimensions,
    public val surface: S,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HostConstraints<*>) return false
        return dimensions == other.dimensions && surface == other.surface
    }

    override fun hashCode(): Int = 31 * dimensions.hashCode() + surface.hashCode()

    override fun toString(): String = "HostConstraints(dimensions=$dimensions, surface=$surface)"
}
