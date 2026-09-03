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

/** Standardized horizontal size tiers representing canonical width breakpoint columns (W1..W4). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class WidthTier {
    /** Extra small / compact width column. */
    W1,

    /** Small width column. */
    W2,

    /** Medium width column. */
    W3,

    /** Large / expanded width column. */
    W4;

    public companion object {
        /**
         * Resolves the canonical baseline [WidthTier] for a given [widthDp] dimension.
         *
         * @param widthDp Container width in DP.
         * @return Resolved [WidthTier].
         */
        public fun fromDp(widthDp: Int): WidthTier = fromDp(widthDp.toFloat())

        /**
         * Resolves the canonical baseline [WidthTier] for a given [widthDp] dimension.
         *
         * @param widthDp Container width in DP.
         * @return Resolved [WidthTier].
         */
        public fun fromDp(widthDp: Float): WidthTier {
            return when {
                widthDp < 130f -> W1
                widthDp < 220f -> W2
                widthDp < 310f -> W3
                else -> W4
            }
        }
    }
}

/** Standardized vertical size tiers representing canonical height breakpoint rows (H0..H4). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class HeightTier {
    /** Compact single-line tier. */
    H0,

    /** 1-row widget layout tier. */
    H1,

    /** 2-row widget layout tier. */
    H2,

    /** 3-row widget layout tier. */
    H3,

    /** 4-row / expanded widget layout tier. */
    H4;

    public companion object {
        /**
         * Resolves the canonical baseline [HeightTier] for a given [heightDp] dimension.
         *
         * @param heightDp Container height in DP.
         * @return Resolved [HeightTier].
         */
        public fun fromDp(heightDp: Int): HeightTier = fromDp(heightDp.toFloat())

        /**
         * Resolves the canonical baseline [HeightTier] for a given [heightDp] dimension.
         *
         * @param heightDp Container height in DP.
         * @return Resolved [HeightTier].
         */
        public fun fromDp(heightDp: Float): HeightTier {
            return when {
                heightDp < 60f -> H0
                heightDp < 120f -> H1
                heightDp < 200f -> H2
                heightDp < 290f -> H3
                else -> H4
            }
        }
    }
}

/**
 * Standardized size tiers capturing both [width] ([WidthTier]) and [height] ([HeightTier])
 * coordinates for Glance Adaptive layout selection.
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
         * Resolves the canonical baseline [SizeTiers] for given container [dimensions].
         *
         * Specific host modules provide overloaded [from] extension methods taking host surfaces
         * (e.g. `AppWidgetGlanceSurface`, `WearGlanceSurface`) for surface-specific breakpoints.
         *
         * @param dimensions Active container dimensions in DP.
         * @return Resolved [SizeTiers].
         */
        public fun from(dimensions: Dimensions): SizeTiers =
            from(dimensions.widthDp, dimensions.heightDp)

        /**
         * Resolves the canonical baseline [SizeTiers] for given container [widthDp] and [heightDp].
         *
         * @param widthDp Container width in DP.
         * @param heightDp Container height in DP.
         * @return Resolved [SizeTiers].
         */
        public fun from(widthDp: Int, heightDp: Int): SizeTiers =
            from(widthDp.toFloat(), heightDp.toFloat())

        /**
         * Resolves the canonical baseline [SizeTiers] for given container [widthDp] and [heightDp].
         *
         * @param widthDp Container width in DP.
         * @param heightDp Container height in DP.
         * @return Resolved [SizeTiers].
         */
        public fun from(widthDp: Float, heightDp: Float): SizeTiers =
            SizeTiers(WidthTier.fromDp(widthDp), HeightTier.fromDp(heightDp))
    }
}
