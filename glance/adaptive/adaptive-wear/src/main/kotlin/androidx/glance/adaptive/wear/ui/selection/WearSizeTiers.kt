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

package androidx.glance.adaptive.wear.ui.selection

import androidx.annotation.RestrictTo
import androidx.glance.adaptive.core.ui.selection.Dimensions
import androidx.glance.adaptive.core.ui.selection.HeightTier
import androidx.glance.adaptive.core.ui.selection.SizeTiers
import androidx.glance.adaptive.core.ui.selection.WidthTier

/**
 * Breakpoint resolution utility mapping physical container dimensions to standardized [SizeTiers]
 * on Wear OS glanceable surfaces.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object WearSizeTiers {

    /** Resolves the [WidthTier] for a given [widthDp] dimension and [surface]. */
    public fun resolveWidthTier(widthDp: Float, surface: WearGlanceSurface): WidthTier {
        return when {
            widthDp < 140f -> WidthTier.W1
            else -> WidthTier.W2
        }
    }

    /** Resolves the [WidthTier] for a given [widthDp] integer dimension and [surface]. */
    public fun resolveWidthTier(widthDp: Int, surface: WearGlanceSurface): WidthTier =
        resolveWidthTier(widthDp.toFloat(), surface)

    /** Resolves the [HeightTier] for a given [heightDp] dimension and [surface]. */
    public fun resolveHeightTier(heightDp: Float, surface: WearGlanceSurface): HeightTier {
        return when (surface) {
            WearGlanceSurface.COMPLICATION -> HeightTier.H0
            WearGlanceSurface.TILE -> if (heightDp < 140f) HeightTier.H1 else HeightTier.H2
        }
    }

    /** Resolves the [HeightTier] for a given [heightDp] integer dimension and [surface]. */
    public fun resolveHeightTier(heightDp: Int, surface: WearGlanceSurface): HeightTier =
        resolveHeightTier(heightDp.toFloat(), surface)

    /** Resolves standardized [SizeTiers] given width/height in DP and target [surface]. */
    public fun from(widthDp: Float, heightDp: Float, surface: WearGlanceSurface): SizeTiers {
        val w = resolveWidthTier(widthDp, surface)
        val h = resolveHeightTier(heightDp, surface)
        return SizeTiers(w, h)
    }

    /** Resolves standardized [SizeTiers] given integer width/height in DP and target [surface]. */
    public fun from(widthDp: Int, heightDp: Int, surface: WearGlanceSurface): SizeTiers =
        from(widthDp.toFloat(), heightDp.toFloat(), surface)

    /** Resolves standardized [SizeTiers] given [dimensions] and target [surface]. */
    public fun from(dimensions: Dimensions, surface: WearGlanceSurface): SizeTiers =
        from(dimensions.widthDp.toFloat(), dimensions.heightDp.toFloat(), surface)
}

/** Resolves standardized [SizeTiers] for given [dimensions] on a [WearGlanceSurface]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun SizeTiers.Companion.from(dimensions: Dimensions, surface: WearGlanceSurface): SizeTiers =
    WearSizeTiers.from(dimensions, surface)
