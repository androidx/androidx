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
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

/**
 * Represents 2D container dimensions in density-independent pixels.
 *
 * @param widthDp Container width in DP.
 * @param heightDp Container height in DP.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Dimensions(public val widthDp: Int, public val heightDp: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Dimensions) return false
        return widthDp == other.widthDp && heightDp == other.heightDp
    }

    override fun hashCode(): Int = 31 * widthDp + heightDp

    override fun toString(): String = "Dimensions(widthDp=$widthDp, heightDp=$heightDp)"
}

/** CompositionLocal providing current container [Dimensions] for local host layout resolution. */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val LocalContainerDimensions: ProvidableCompositionLocal<Dimensions> = compositionLocalOf {
    Dimensions(0, 0)
}

/**
 * Standardized horizontal size tiers representing canonical width breakpoint columns (W1..W4).
 *
 * Breakpoints are resolved dynamically based on the target [GlanceSurface].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class WidthTier {
    /** Extra small / compact width column (e.g. <130 dp on Mobile Home Screen). */
    W1,

    /** Small width column (e.g. 130..219 dp on Mobile Home Screen). */
    W2,

    /** Medium width column (e.g. 220..309 dp on Mobile Home Screen). */
    W3,

    /** Large / expanded width column (e.g. >=310 dp on Mobile Home Screen). */
    W4;

    public companion object {
        /**
         * Resolves the [WidthTier] for a given [widthDp] dimension in density-independent pixels
         * and the target [surface].
         */
        public fun fromDp(
            widthDp: Int,
            surface: GlanceSurface = GlanceSurface.MOBILE_HOME_SCREEN,
        ): WidthTier = fromDp(widthDp.toFloat(), surface)

        /**
         * Resolves the [WidthTier] for a given [widthDp] dimension in density-independent pixels
         * and the target [surface].
         */
        public fun fromDp(
            widthDp: Float,
            surface: GlanceSurface = GlanceSurface.MOBILE_HOME_SCREEN,
        ): WidthTier {
            return when (surface) {
                GlanceSurface.MOBILE_HOME_SCREEN,
                GlanceSurface.TABLET_HOME_SCREEN ->
                    when {
                        widthDp < 130f -> W1
                        widthDp < 220f -> W2
                        widthDp < 310f -> W3
                        else -> W4
                    }
                GlanceSurface.MOBILE_LOCK_SCREEN ->
                    when {
                        widthDp < 100f -> W1
                        widthDp < 200f -> W2
                        widthDp < 300f -> W3
                        else -> W4
                    }
                GlanceSurface.WEAR_TILE,
                GlanceSurface.WEAR_COMPLICATION ->
                    when {
                        widthDp < 140f -> W1
                        else -> W2
                    }
                GlanceSurface.XR_GLASSES ->
                    when {
                        widthDp < 200f -> W1
                        widthDp < 350f -> W2
                        widthDp < 500f -> W3
                        else -> W4
                    }
            }
        }
    }
}

/**
 * Standardized vertical size tiers representing canonical height breakpoint rows (H0..H4).
 *
 * Breakpoints are resolved dynamically based on the target [GlanceSurface].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class HeightTier {
    /** Lockscreen / Glanceable compact single-line tier (<60 dp on Mobile Home Screen). */
    H0,

    /** 1-row widget layout tier (60..119 dp on Mobile Home Screen). */
    H1,

    /** 2-row widget layout tier (120..199 dp on Mobile Home Screen). */
    H2,

    /** 3-row widget layout tier (200..289 dp on Mobile Home Screen). */
    H3,

    /** 4-row / expanded widget layout tier (>=290 dp on Mobile Home Screen). */
    H4;

    public companion object {
        /**
         * Resolves the [HeightTier] for a given [heightDp] dimension in density-independent pixels
         * and the target [surface].
         */
        public fun fromDp(
            heightDp: Int,
            surface: GlanceSurface = GlanceSurface.MOBILE_HOME_SCREEN,
        ): HeightTier = fromDp(heightDp.toFloat(), surface)

        /**
         * Resolves the [HeightTier] for a given [heightDp] dimension in density-independent pixels
         * and the target [surface].
         */
        public fun fromDp(
            heightDp: Float,
            surface: GlanceSurface = GlanceSurface.MOBILE_HOME_SCREEN,
        ): HeightTier {
            return when (surface) {
                GlanceSurface.MOBILE_LOCK_SCREEN,
                GlanceSurface.WEAR_COMPLICATION -> H0
                GlanceSurface.WEAR_TILE -> if (heightDp < 140f) H1 else H2
                GlanceSurface.MOBILE_HOME_SCREEN,
                GlanceSurface.TABLET_HOME_SCREEN ->
                    when {
                        heightDp < 60f -> H0
                        heightDp < 120f -> H1
                        heightDp < 200f -> H2
                        heightDp < 290f -> H3
                        else -> H4
                    }
                GlanceSurface.XR_GLASSES ->
                    when {
                        heightDp < 150f -> H1
                        heightDp < 300f -> H2
                        heightDp < 450f -> H3
                        else -> H4
                    }
            }
        }
    }
}

/**
 * Standardized size tiers capturing both [width] ([WidthTier]) and [height] ([HeightTier])
 * coordinates for Glance Adaptive layout selection.
 *
 * Provides factory methods on [companion object] to resolve physical container dimensions into
 * canonical size tiers based on target [GlanceSurface].
 *
 * @param width Resolved canonical horizontal size tier.
 * @param height Resolved canonical vertical size tier.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SizeTiers(public val width: WidthTier, public val height: HeightTier) {
    public operator fun component1(): WidthTier = width

    public operator fun component2(): HeightTier = height

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SizeTiers) return false
        return width == other.width && height == other.height
    }

    override fun hashCode(): Int = 31 * width.hashCode() + height.hashCode()

    override fun toString(): String = "SizeTiers(width=$width, height=$height)"

    public companion object {
        /**
         * Resolves the standardized [SizeTiers] given physical dimensions in DP and target
         * [surface].
         *
         * @param widthDp Container width in DP.
         * @param heightDp Container height in DP.
         * @param surface Target physical surface.
         * @return Resolved [SizeTiers].
         */
        public fun from(
            widthDp: Int,
            heightDp: Int,
            surface: GlanceSurface = GlanceSurface.MOBILE_HOME_SCREEN,
        ): SizeTiers = from(widthDp.toFloat(), heightDp.toFloat(), surface)

        /**
         * Resolves the standardized [SizeTiers] given physical dimensions in DP and target
         * [surface].
         *
         * @param widthDp Container width in DP.
         * @param heightDp Container height in DP.
         * @param surface Target physical surface.
         * @return Resolved [SizeTiers].
         */
        public fun from(
            widthDp: Float,
            heightDp: Float,
            surface: GlanceSurface = GlanceSurface.MOBILE_HOME_SCREEN,
        ): SizeTiers {
            val wTier = WidthTier.fromDp(widthDp, surface)
            val hTier = HeightTier.fromDp(heightDp, surface)
            return SizeTiers(wTier, hTier)
        }

        /** Resolves the standardized [SizeTiers] for given [dimensions] and target [surface]. */
        public fun from(
            dimensions: Dimensions,
            surface: GlanceSurface = GlanceSurface.MOBILE_HOME_SCREEN,
        ): SizeTiers = from(dimensions.widthDp, dimensions.heightDp, surface)
    }
}
