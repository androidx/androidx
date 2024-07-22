/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.window.embedding

import android.graphics.Rect
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.annotation.Px
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.window.core.Bounds
import androidx.window.embedding.EmbeddingBounds.Alignment.Companion.ALIGN_BOTTOM
import androidx.window.embedding.EmbeddingBounds.Alignment.Companion.ALIGN_LEFT
import androidx.window.embedding.EmbeddingBounds.Alignment.Companion.ALIGN_RIGHT
import androidx.window.embedding.EmbeddingBounds.Alignment.Companion.ALIGN_TOP
import androidx.window.embedding.EmbeddingBounds.Dimension.Companion.DIMENSION_EXPANDED
import androidx.window.embedding.EmbeddingBounds.Dimension.Companion.DIMENSION_HINGE
import androidx.window.embedding.EmbeddingBounds.Dimension.Companion.ratio
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowLayoutInfo
import kotlin.math.min

/**
 * The bounds of a standalone [ActivityStack].
 *
 * It can be either described with `alignment`, `width` and `height` or predefined constant values.
 * Some important constants are:
 * - [BOUNDS_EXPANDED]: To indicate the bounds fills the parent window container.
 * - [BOUNDS_HINGE_TOP]: To indicate the bounds are at the top of the parent window container while
 *   its bottom follows the hinge position. Refer to [BOUNDS_HINGE_LEFT], [BOUNDS_HINGE_BOTTOM] and
 *   [BOUNDS_HINGE_RIGHT] for other bounds that follows the hinge position.
 *
 * @constructor creates an embedding bounds.
 * @property alignment The alignment of the bounds relative to parent window container.
 * @property width The width of the bounds.
 * @property height The height of the bounds.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class EmbeddingBounds(val alignment: Alignment, val width: Dimension, val height: Dimension) {
    override fun toString(): String {
        return "Bounds:{alignment=$alignment, width=$width, height=$height}"
    }

    override fun hashCode(): Int {
        var result = alignment.hashCode()
        result = result * 31 + width.hashCode()
        result = result * 31 + height.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmbeddingBounds) return false
        return alignment == other.alignment && width == other.width && height == other.height
    }

    /** Returns `true` if the [width] should fallback to half of parent task width. */
    internal fun shouldUseFallbackDimensionForWidth(windowLayoutInfo: WindowLayoutInfo): Boolean {
        if (width != DIMENSION_HINGE) {
            return false
        }
        return !windowLayoutInfo.isVertical() || alignment in listOf(ALIGN_TOP, ALIGN_BOTTOM)
    }

    /** Returns `true` if the [height] should fallback to half of parent task height. */
    internal fun shouldUseFallbackDimensionForHeight(windowLayoutInfo: WindowLayoutInfo): Boolean {
        if (height != DIMENSION_HINGE) {
            return false
        }
        return !windowLayoutInfo.isHorizontal() || alignment in listOf(ALIGN_LEFT, ALIGN_RIGHT)
    }

    private fun WindowLayoutInfo.isHorizontal(): Boolean {
        val foldingFeature = getOnlyFoldingFeatureOrNull() ?: return false
        return foldingFeature.orientation == FoldingFeature.Orientation.HORIZONTAL
    }

    private fun WindowLayoutInfo.isVertical(): Boolean {
        val foldingFeature = getOnlyFoldingFeatureOrNull() ?: return false
        return foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL
    }

    /**
     * Returns [FoldingFeature] if it's the only `FoldingFeature` in [WindowLayoutInfo]. Returns
     * `null`, otherwise.
     */
    private fun WindowLayoutInfo.getOnlyFoldingFeatureOrNull(): FoldingFeature? {
        val foldingFeatures = displayFeatures.filterIsInstance<FoldingFeature>()
        return if (foldingFeatures.size == 1) foldingFeatures[0] else null
    }

    /** Calculates [width] in pixel with [parentContainerBounds] and [windowLayoutInfo]. */
    @Px
    internal fun getWidthInPixel(
        parentContainerBounds: Bounds,
        windowLayoutInfo: WindowLayoutInfo
    ): Int {
        val taskWidth = parentContainerBounds.width
        val widthDimension =
            if (shouldUseFallbackDimensionForWidth(windowLayoutInfo)) {
                ratio(0.5f)
            } else {
                width
            }
        when (widthDimension) {
            is Dimension.Ratio -> return widthDimension * taskWidth
            is Dimension.Pixel -> return min(taskWidth, widthDimension.value)
            DIMENSION_HINGE -> {
                // Should be verified by #shouldUseFallbackDimensionForWidth
                val hingeBounds = windowLayoutInfo.getOnlyFoldingFeatureOrNull()!!.bounds
                return when (alignment) {
                    ALIGN_LEFT -> {
                        hingeBounds.left - parentContainerBounds.left
                    }
                    ALIGN_RIGHT -> {
                        parentContainerBounds.right - hingeBounds.right
                    }
                    else -> {
                        throw IllegalStateException(
                            "Unhandled condition to get height in pixel! " +
                                "embeddingBounds=$this taskBounds=$parentContainerBounds " +
                                "windowLayoutInfo=$windowLayoutInfo"
                        )
                    }
                }
            }
            else -> throw IllegalArgumentException("Unhandled width dimension=$width")
        }
    }

    /** Calculates [height] in pixel with [parentContainerBounds] and [windowLayoutInfo]. */
    @Px
    internal fun getHeightInPixel(
        parentContainerBounds: Bounds,
        windowLayoutInfo: WindowLayoutInfo
    ): Int {
        val taskHeight = parentContainerBounds.height
        val heightDimension =
            if (shouldUseFallbackDimensionForHeight(windowLayoutInfo)) {
                ratio(0.5f)
            } else {
                height
            }
        when (heightDimension) {
            is Dimension.Ratio -> return heightDimension * taskHeight
            is Dimension.Pixel -> return min(taskHeight, heightDimension.value)
            DIMENSION_HINGE -> {
                // Should be verified by #shouldUseFallbackDimensionForWidth
                val hingeBounds = windowLayoutInfo.getOnlyFoldingFeatureOrNull()!!.bounds
                return when (alignment) {
                    ALIGN_TOP -> {
                        hingeBounds.top - parentContainerBounds.top
                    }
                    ALIGN_BOTTOM -> {
                        parentContainerBounds.bottom - hingeBounds.bottom
                    }
                    else -> {
                        throw IllegalStateException(
                            "Unhandled condition to get height in pixel! " +
                                "embeddingBounds=$this taskBounds=$parentContainerBounds " +
                                "windowLayoutInfo=$windowLayoutInfo"
                        )
                    }
                }
            }
            else -> throw IllegalArgumentException("Unhandled width dimension=$width")
        }
    }

    /** The position of the bounds relative to parent window container. */
    class Alignment internal constructor(@IntRange(from = 0, to = 3) internal val value: Int) {

        init {
            require(value in 0..3)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Alignment) return false
            return value == other.value
        }

        override fun hashCode(): Int {
            return value
        }

        override fun toString(): String =
            when (value) {
                0 -> "left"
                1 -> "top"
                2 -> "right"
                3 -> "bottom"
                else -> "unknown position:$value"
            }

        companion object {

            /** Specifies that the bounds is at the left of the parent window container. */
            @JvmField val ALIGN_LEFT = Alignment(0)

            /** Specifies that the bounds is at the top of the parent window container. */
            @JvmField val ALIGN_TOP = Alignment(1)

            /** Specifies that the bounds is at the right of the parent window container. */
            @JvmField val ALIGN_RIGHT = Alignment(2)

            /** Specifies that the bounds is at the bottom of the parent window container. */
            @JvmField val ALIGN_BOTTOM = Alignment(3)
        }
    }

    /**
     * The dimension of the bounds, which can be represented as multiple formats:
     * - [DIMENSION_EXPANDED]: means the bounds' dimension fills parent window's dimension.
     * - in [pixel]: To specify the dimension value in pixel.
     * - in [ratio]: To specify the dimension that relative to the parent window container. For
     *   example, if [width] has [ratio] value 0.6, it means the bounds' width is 0.6 to the parent
     *   window container's width.
     */
    abstract class Dimension internal constructor(internal val description: String) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Dimension) return false
            return description == other.description
        }

        override fun hashCode(): Int = description.hashCode()

        override fun toString(): String = description

        /**
         * The [Dimension] represented in pixel format
         *
         * @param value The dimension length in pixel
         */
        internal class Pixel(@Px @IntRange(from = 1) internal val value: Int) :
            Dimension("dimension in pixel:$value") {

            init {
                require(value >= 1) { "Pixel value must be a positive integer." }
            }

            internal operator fun compareTo(dimen: Int): Int = value - dimen
        }

        /**
         * The [Dimension] represented in ratio format, which means the proportion of the parent
         * window dimension.
         *
         * @param value The ratio in (0.0, 1.0)
         */
        internal class Ratio(
            @FloatRange(from = 0.0, fromInclusive = false, to = 1.0) internal val value: Float
        ) : Dimension("dimension in ratio:$value") {

            init {
                require(value > 0.0 && value <= 1.0) { "Ratio must be in range (0.0, 1.0]" }
            }

            internal operator fun times(dimen: Int): Int = (value * dimen).toInt()
        }

        companion object {

            /** Represents this dimension follows its parent window dimension. */
            @JvmField val DIMENSION_EXPANDED: Dimension = Ratio(1.0f)

            /**
             * Represents this dimension follows the hinge position if the current window and device
             * state satisfies, or fallbacks to a half of the parent task dimension, otherwise.
             *
             * The [DIMENSION_HINGE] works only if:
             * - The parent container is not in multi-window mode (e.g., split-screen mode or
             *   picture-in-picture mode)
             * - The device has a hinge or separating fold reported by
             *   [androidx.window.layout.FoldingFeature.isSeparating]
             * - The hinge or separating fold orientation matches [EmbeddingBounds.alignment]:
             *     - The hinge or fold orientation is vertical, and the position is [POSITION_LEFT]
             *       or [POSITION_RIGHT]
             *     - The hinge or fold orientation is horizontal, and the position is [POSITION_TOP]
             *       or [POSITION_BOTTOM]
             */
            @JvmField val DIMENSION_HINGE: Dimension = object : Dimension("hinge") {}

            /**
             * Creates the dimension in pixel.
             *
             * If the dimension length exceeds the parent window dimension, the overlay container
             * will resize to fit the parent task dimension.
             *
             * @param value The dimension length in pixel
             */
            @JvmStatic fun pixel(@Px @IntRange(from = 1) value: Int): Dimension = Pixel(value)

            /**
             * Creates the dimension which takes a proportion of the parent window dimension.
             *
             * @param ratio The proportion of the parent window dimension this dimension should take
             */
            @JvmStatic
            fun ratio(
                @FloatRange(from = 0.0, fromInclusive = false, to = 1.0, toInclusive = false)
                ratio: Float
            ): Dimension = Ratio(ratio)
        }
    }

    companion object {

        /** The bounds fills the parent window bounds */
        @JvmField
        val BOUNDS_EXPANDED = EmbeddingBounds(ALIGN_TOP, DIMENSION_EXPANDED, DIMENSION_EXPANDED)

        /**
         * The bounds located on the top of the parent window, and the bounds' bottom side matches
         * the hinge position.
         */
        @JvmField
        val BOUNDS_HINGE_TOP =
            EmbeddingBounds(ALIGN_TOP, width = DIMENSION_EXPANDED, height = DIMENSION_HINGE)

        /**
         * The bounds located on the left of the parent window, and the bounds' right side matches
         * the hinge position.
         */
        @JvmField
        val BOUNDS_HINGE_LEFT =
            EmbeddingBounds(ALIGN_LEFT, width = DIMENSION_HINGE, height = DIMENSION_EXPANDED)

        /**
         * The bounds located on the bottom of the parent window, and the bounds' top side matches
         * the hinge position.
         */
        @JvmField
        val BOUNDS_HINGE_BOTTOM =
            EmbeddingBounds(ALIGN_BOTTOM, width = DIMENSION_EXPANDED, height = DIMENSION_HINGE)

        /**
         * The bounds located on the right of the parent window, and the bounds' left side matches
         * the hinge position.
         */
        @JvmField
        val BOUNDS_HINGE_RIGHT =
            EmbeddingBounds(ALIGN_RIGHT, width = DIMENSION_HINGE, height = DIMENSION_EXPANDED)

        /** Translates [EmbeddingBounds] to pure [Rect] bounds with given [ParentContainerInfo]. */
        @VisibleForTesting
        internal fun translateEmbeddingBounds(
            embeddingBounds: EmbeddingBounds,
            parentContainerBounds: Bounds,
            windowLayoutInfo: WindowLayoutInfo,
        ): Bounds {
            if (
                embeddingBounds.width == DIMENSION_EXPANDED &&
                    embeddingBounds.height == DIMENSION_EXPANDED
            ) {
                // If width and height are expanded, set bounds to empty to follow the parent task
                // bounds.
                return Bounds.EMPTY_BOUNDS
            }
            // 1. Fallbacks dimensions to ratio(0.5) if they can't follow the hinge with the current
            //    device and window state.
            val width =
                if (embeddingBounds.shouldUseFallbackDimensionForWidth(windowLayoutInfo)) {
                    ratio(0.5f)
                } else {
                    embeddingBounds.width
                }
            val height =
                if (embeddingBounds.shouldUseFallbackDimensionForHeight(windowLayoutInfo)) {
                    ratio(0.5f)
                } else {
                    embeddingBounds.height
                }

            // 2. Computes dimensions to pixel values. If it just matches parent task bounds,
            // returns
            //    the empty bounds to declare the bounds follow the parent task bounds.
            val sanitizedBounds = EmbeddingBounds(embeddingBounds.alignment, width, height)
            val widthInPixel =
                sanitizedBounds.getWidthInPixel(parentContainerBounds, windowLayoutInfo)
            val heightInPixel =
                sanitizedBounds.getHeightInPixel(parentContainerBounds, windowLayoutInfo)
            val taskWidth = parentContainerBounds.width
            val taskHeight = parentContainerBounds.height

            if (widthInPixel == taskWidth && heightInPixel == taskHeight) {
                return Bounds.EMPTY_BOUNDS
            }

            // 3. Offset the bounds by position:
            //     - For top or bottom position, the bounds should attach to the top or bottom of
            //       the parent task bounds and centered by the middle of the width.
            //     - For left or right position, the bounds should attach to the left or right of
            //       the parent task bounds and centered by the middle of the height.
            return Bounds(0, 0, widthInPixel, heightInPixel).let { bounds ->
                when (embeddingBounds.alignment) {
                    ALIGN_TOP -> bounds.offset(((taskWidth - widthInPixel) / 2), 0)
                    ALIGN_LEFT -> bounds.offset(0, ((taskHeight - heightInPixel) / 2))
                    ALIGN_BOTTOM ->
                        bounds.offset(((taskWidth - widthInPixel) / 2), taskHeight - heightInPixel)
                    ALIGN_RIGHT ->
                        bounds.offset(taskWidth - widthInPixel, ((taskHeight - heightInPixel) / 2))
                    else ->
                        throw IllegalArgumentException(
                            "Unknown alignment: ${embeddingBounds.alignment}"
                        )
                }
            }
        }

        private fun Bounds.offset(dx: Int, dy: Int): Bounds =
            Bounds(left + dx, top + dy, right + dx, bottom + dy)

        /** Translates [EmbeddingBounds] to pure [Rect] bounds with given [ParentContainerInfo]. */
        internal fun translateEmbeddingBounds(
            embeddingBounds: EmbeddingBounds,
            parentContainerInfo: ParentContainerInfo,
        ): Bounds =
            translateEmbeddingBounds(
                embeddingBounds,
                parentContainerInfo.windowBounds,
                parentContainerInfo.windowLayoutInfo
            )
    }
}
