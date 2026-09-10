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

    /** Resolves the [WidthTier] for a given [widthDp] dimension and [surface]. */
    public fun resolveWidthTier(widthDp: Float, surface: AppWidgetGlanceSurface): WidthTier {
        return when (surface) {
            AppWidgetGlanceSurface.MOBILE_HOME_SCREEN,
            AppWidgetGlanceSurface.TABLET_HOME_SCREEN ->
                when {
                    widthDp < 130f -> WidthTier.W1
                    widthDp < 220f -> WidthTier.W2
                    widthDp < 310f -> WidthTier.W3
                    else -> WidthTier.W4
                }
            AppWidgetGlanceSurface.MOBILE_LOCK_SCREEN ->
                when {
                    widthDp < 100f -> WidthTier.W1
                    widthDp < 200f -> WidthTier.W2
                    widthDp < 300f -> WidthTier.W3
                    else -> WidthTier.W4
                }
        }
    }

    /** Resolves the [WidthTier] for a given [widthDp] integer dimension and [surface]. */
    public fun resolveWidthTier(widthDp: Int, surface: AppWidgetGlanceSurface): WidthTier =
        resolveWidthTier(widthDp.toFloat(), surface)

    /** Resolves the [HeightTier] for a given [heightDp] dimension and [surface]. */
    public fun resolveHeightTier(heightDp: Float, surface: AppWidgetGlanceSurface): HeightTier {
        return when (surface) {
            AppWidgetGlanceSurface.MOBILE_LOCK_SCREEN -> HeightTier.H0
            AppWidgetGlanceSurface.MOBILE_HOME_SCREEN,
            AppWidgetGlanceSurface.TABLET_HOME_SCREEN ->
                when {
                    heightDp < 60f -> HeightTier.H0
                    heightDp < 120f -> HeightTier.H1
                    heightDp < 200f -> HeightTier.H2
                    heightDp < 290f -> HeightTier.H3
                    else -> HeightTier.H4
                }
        }
    }

    /** Resolves the [HeightTier] for a given [heightDp] integer dimension and [surface]. */
    public fun resolveHeightTier(heightDp: Int, surface: AppWidgetGlanceSurface): HeightTier =
        resolveHeightTier(heightDp.toFloat(), surface)

    /** Resolves standardized [SizeTiers] given width/height in DP and target [surface]. */
    public fun from(widthDp: Float, heightDp: Float, surface: AppWidgetGlanceSurface): SizeTiers {
        val w = resolveWidthTier(widthDp, surface)
        val h = resolveHeightTier(heightDp, surface)
        return SizeTiers(w, h)
    }

    /** Resolves standardized [SizeTiers] given integer width/height in DP and target [surface]. */
    public fun from(widthDp: Int, heightDp: Int, surface: AppWidgetGlanceSurface): SizeTiers =
        from(widthDp.toFloat(), heightDp.toFloat(), surface)

    /** Resolves standardized [SizeTiers] given [dimensions] and target [surface]. */
    public fun from(dimensions: Dimensions, surface: AppWidgetGlanceSurface): SizeTiers =
        from(dimensions.widthDp.toFloat(), dimensions.heightDp.toFloat(), surface)
}

/** Resolves standardized [SizeTiers] for given [dimensions] on an [AppWidgetGlanceSurface]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun SizeTiers.Companion.from(
    dimensions: Dimensions,
    surface: AppWidgetGlanceSurface,
): SizeTiers = AppWidgetSizeTiers.from(dimensions, surface)
