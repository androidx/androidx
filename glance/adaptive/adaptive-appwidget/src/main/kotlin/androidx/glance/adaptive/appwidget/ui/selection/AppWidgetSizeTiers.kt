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

package androidx.glance.adaptive.appwidget.ui.selection

import androidx.annotation.RestrictTo
import androidx.glance.adaptive.core.ui.selection.Dimensions
import androidx.glance.adaptive.core.ui.selection.HeightTier
import androidx.glance.adaptive.core.ui.selection.SizeTiers
import androidx.glance.adaptive.core.ui.selection.WidthTier

/**
 * Breakpoint resolution utility mapping physical container dimensions to standardized [SizeTiers]
 * on platform AppWidget surfaces.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object AppWidgetSizeTiers {

    /**
     * Resolves the [WidthTier] for a given [widthDp] dimension and [surface].
     *
     * A home screen widget is measured against the design's own columns, so it defers to
     * [WidthTier.fromDp]. The lock screen packs the same columns into a narrower strip and so
     * carries its own scale.
     */
    public fun resolveWidthTier(widthDp: Float, surface: AppWidgetSurface): WidthTier {
        return when (surface) {
            AppWidgetSurface.MOBILE_HOME_SCREEN -> WidthTier.fromDp(widthDp)
            AppWidgetSurface.MOBILE_LOCK_SCREEN ->
                when {
                    widthDp < 100f -> WidthTier.W1
                    widthDp < 200f -> WidthTier.W2
                    widthDp < 300f -> WidthTier.W3
                    else -> WidthTier.W4
                }
            else -> WidthTier.fromDp(widthDp)
        }
    }

    /** Resolves the [WidthTier] for a given [widthDp] integer dimension and [surface]. */
    public fun resolveWidthTier(widthDp: Int, surface: AppWidgetSurface): WidthTier =
        resolveWidthTier(widthDp.toFloat(), surface)

    /**
     * Resolves the [HeightTier] for a given [heightDp] dimension and [surface].
     *
     * The lock screen has one band of its own: however tall the slot is, the content is a single
     * line, so it is always [HeightTier.H0].
     */
    public fun resolveHeightTier(heightDp: Float, surface: AppWidgetSurface): HeightTier {
        return when (surface) {
            AppWidgetSurface.MOBILE_LOCK_SCREEN -> HeightTier.H0
            AppWidgetSurface.MOBILE_HOME_SCREEN -> HeightTier.fromDp(heightDp)
            else -> HeightTier.fromDp(heightDp)
        }
    }

    /** Resolves the [HeightTier] for a given [heightDp] integer dimension and [surface]. */
    public fun resolveHeightTier(heightDp: Int, surface: AppWidgetSurface): HeightTier =
        resolveHeightTier(heightDp.toFloat(), surface)

    /** Resolves standardized [SizeTiers] given width/height in DP and target [surface]. */
    public fun from(widthDp: Float, heightDp: Float, surface: AppWidgetSurface): SizeTiers {
        val w = resolveWidthTier(widthDp, surface)
        val h = resolveHeightTier(heightDp, surface)
        return SizeTiers(w, h)
    }

    /** Resolves standardized [SizeTiers] given integer width/height in DP and target [surface]. */
    public fun from(widthDp: Int, heightDp: Int, surface: AppWidgetSurface): SizeTiers =
        from(widthDp.toFloat(), heightDp.toFloat(), surface)

    /** Resolves standardized [SizeTiers] given [dimensions] and target [surface]. */
    public fun from(dimensions: Dimensions, surface: AppWidgetSurface): SizeTiers =
        from(dimensions.widthDp.toFloat(), dimensions.heightDp.toFloat(), surface)
}

/** Resolves standardized [SizeTiers] for given [dimensions] on an [AppWidgetSurface]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun SizeTiers.Companion.from(
    dimensions: Dimensions,
    surface: AppWidgetSurface,
): SizeTiers = AppWidgetSizeTiers.from(dimensions, surface)
