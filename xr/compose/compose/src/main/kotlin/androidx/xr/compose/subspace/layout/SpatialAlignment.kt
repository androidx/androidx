/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.subspace.layout

import androidx.annotation.FloatRange
import androidx.compose.ui.unit.LayoutDirection
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.runtime.math.Vector3
import kotlin.math.roundToInt

/**
 * An interface to calculate the position of a sized box inside an available 3D space.
 * [SpatialAlignment] is often used to define the alignment of a layout inside a parent layout.
 *
 * @see SpatialBiasAlignment
 * @see SpatialAbsoluteAlignment
 */
public interface SpatialAlignment {
    /**
     * Provides the horizontal offset from the origin of the space to the origin of the content.
     *
     * @param width The content width in pixels.
     * @param space The available space in pixels.
     * @param layoutDirection LTR or RTL.
     */
    public fun horizontalOffset(width: Int, space: Int, layoutDirection: LayoutDirection): Int

    /**
     * Provides the vertical offset from the origin of the space to the origin of the content.
     *
     * @param height The content height in pixels.
     * @param space The available space in pixels.
     */
    public fun verticalOffset(height: Int, space: Int): Int

    /**
     * Provides the depth offset from the origin of the space to the origin of the content.
     *
     * @param depth The content depth in pixels.
     * @param space The available space in pixels.
     */
    public fun depthOffset(depth: Int, space: Int): Int

    /**
     * Provides the origin-based position of the content in the available space.
     *
     * @param size The content size in pixels.
     * @param space The available space in pixels.
     * @param layoutDirection LTR or RTL.
     */
    public fun position(
        size: IntVolumeSize,
        space: IntVolumeSize,
        layoutDirection: LayoutDirection,
    ): Vector3

    /**
     * An interface to calculate the position of a box of a certain width inside an available width.
     */
    public interface Horizontal {
        /**
         * Provides the horizontal offset from the origin of the space to the origin of the content.
         *
         * @param width The content width in pixels.
         * @param space The available space in pixels.
         * @param layoutDirection LTR or RTL.
         * @see [SpatialAlignment.horizontalOffset]
         */
        public fun offset(width: Int, space: Int, layoutDirection: LayoutDirection): Int
    }

    /**
     * An interface to calculate the position of a box of a certain height inside an available
     * height.
     */
    public interface Vertical {
        /**
         * Provides the vertical offset from the origin of the space to the origin of the content.
         *
         * @param height The content height in pixels.
         * @param space The available space in pixels.
         * @see [SpatialAlignment.verticalOffset]
         */
        public fun offset(height: Int, space: Int): Int
    }

    /**
     * An interface to calculate the position of a box of a certain depth inside an available depth.
     */
    public interface Depth {
        /**
         * Provides the depth offset from the origin of the space to the origin of the content.
         *
         * @param depth The content depth in pixels.
         * @param space The available space in pixels.
         * @see [SpatialAlignment.depthOffset]
         */
        public fun offset(depth: Int, space: Int): Int
    }

    public companion object {
        // 2D alignments
        @JvmStatic public val TopStart: SpatialAlignment = SpatialBiasAlignment(-1f, 1f, 0f)
        @JvmStatic public val TopCenter: SpatialAlignment = SpatialBiasAlignment(0f, 1f, 0f)
        @JvmStatic public val TopEnd: SpatialAlignment = SpatialBiasAlignment(1f, 1f, 0f)
        @JvmStatic public val CenterStart: SpatialAlignment = SpatialBiasAlignment(-1f, 0f, 0f)
        @JvmStatic public val Center: SpatialAlignment = SpatialBiasAlignment(0f, 0f, 0f)
        @JvmStatic public val CenterEnd: SpatialAlignment = SpatialBiasAlignment(1f, 0f, 0f)
        @JvmStatic public val BottomStart: SpatialAlignment = SpatialBiasAlignment(-1f, -1f, 0f)
        @JvmStatic public val BottomCenter: SpatialAlignment = SpatialBiasAlignment(0f, -1f, 0f)
        @JvmStatic public val BottomEnd: SpatialAlignment = SpatialBiasAlignment(1f, -1f, 0f)

        // Horizontal alignments
        @JvmStatic public val Start: Horizontal = SpatialBiasAlignment.Horizontal(-1f)
        @JvmStatic public val CenterHorizontally: Horizontal = SpatialBiasAlignment.Horizontal(0f)
        @JvmStatic public val End: Horizontal = SpatialBiasAlignment.Horizontal(1f)

        // Vertical alignments
        @JvmStatic public val Bottom: Vertical = SpatialBiasAlignment.Vertical(-1f)
        @JvmStatic public val CenterVertically: Vertical = SpatialBiasAlignment.Vertical(0f)
        @JvmStatic public val Top: Vertical = SpatialBiasAlignment.Vertical(1f)

        // Depth alignments
        @JvmStatic public val Back: Depth = SpatialBiasAlignment.Depth(-1f)
        @JvmStatic public val CenterDepthwise: Depth = SpatialBiasAlignment.Depth(0f)
        @JvmStatic public val Front: Depth = SpatialBiasAlignment.Depth(1f)
    }
}

/** A collection of common [SpatialAlignment]s unaware of the layout direction. */
public object SpatialAbsoluteAlignment {
    /**
     * Aligns at the top of the vertical axis and left of the horizontal axis irrespective of the
     * layout direction.
     */
    @JvmStatic public val TopLeft: SpatialAlignment = SpatialBiasAbsoluteAlignment(-1f, 1f, 0f)
    /**
     * Aligns at the top of the vertical axis and right of the horizontal axis irrespective of the
     * layout direction.
     */
    @JvmStatic public val TopRight: SpatialAlignment = SpatialBiasAbsoluteAlignment(1f, 1f, 0f)
    /**
     * Aligns at the center of the vertical axis and left of the horizontal axis irrespective of the
     * layout direction.
     */
    @JvmStatic public val CenterLeft: SpatialAlignment = SpatialBiasAbsoluteAlignment(-1f, 0f, 0f)
    /**
     * Aligns at the center of the vertical axis and right of the horizontal axis irrespective of
     * the layout direction.
     */
    @JvmStatic public val CenterRight: SpatialAlignment = SpatialBiasAbsoluteAlignment(1f, 0f, 0f)
    /**
     * Aligns at the bottom of the vertical axis and left of the horizontal axis irrespective of the
     * layout direction.
     */
    @JvmStatic public val BottomLeft: SpatialAlignment = SpatialBiasAbsoluteAlignment(-1f, -1f, 0f)
    /**
     * Aligns at the bottom of the vertical axis and right of the horizontal axis irrespective of
     * the layout direction.
     */
    @JvmStatic public val BottomRight: SpatialAlignment = SpatialBiasAbsoluteAlignment(1f, -1f, 0f)

    // Horizontal alignments
    /** Aligns at the left of the horizontal axis irrespective of the layout direction. */
    @JvmStatic
    public val Left: SpatialAlignment.Horizontal = SpatialBiasAbsoluteAlignment.Horizontal(-1f)
    /** Aligns at the right of the horizontal axis irrespective of the layout direction. */
    @JvmStatic
    public val Right: SpatialAlignment.Horizontal = SpatialBiasAbsoluteAlignment.Horizontal(1f)
}

/**
 * Positions content in 3D space using horizontal, vertical, and depth bias.
 *
 * Bias refers to a normalized floating-point value, typically within the range of [-1.0f, 1.0f],
 * that determines how a child's content is positioned along a specific axis relative to its
 * parent's available space.
 * - A bias of 0.0f centers the content along that axis.
 * - A bias of -1.0f aligns the content to one extreme (e.g., start for horizontal, bottom for
 *   vertical, back for depth).
 * - A bias of 1.0f aligns the content to the opposite extreme (e.g., end for horizontal, top for
 *   vertical, front for depth).
 * - Values between -1.0f and 1.0f linearly interpolate the position between these extremes.
 *
 * @param horizontalBias Must be within the range of [-1, 1] with -1 being start and 1 being end.
 * @param verticalBias Must be within the range of [-1, 1] with -1 being bottom and 1 being top.
 * @param depthBias Must be within the range of [-1, 1] with -1 being back and 1 being front.
 */
public class SpatialBiasAlignment(
    @param:FloatRange(-1.0, 1.0) public val horizontalBias: Float,
    @param:FloatRange(-1.0, 1.0) public val verticalBias: Float,
    @param:FloatRange(-1.0, 1.0) public val depthBias: Float,
) : SpatialAlignment {
    override fun horizontalOffset(width: Int, space: Int, layoutDirection: LayoutDirection): Int =
        offset(horizontalBias, width, space, layoutDirection)

    override fun verticalOffset(height: Int, space: Int): Int = offset(verticalBias, height, space)

    override fun depthOffset(depth: Int, space: Int): Int = offset(depthBias, depth, space)

    override fun position(
        size: IntVolumeSize,
        space: IntVolumeSize,
        layoutDirection: LayoutDirection,
    ): Vector3 =
        Vector3(
            horizontalOffset(size.width, space.width, layoutDirection).toFloat(),
            verticalOffset(size.height, space.height).toFloat(),
            depthOffset(size.depth, space.depth).toFloat(),
        )

    public fun copy(
        horizontalBias: Float = this.horizontalBias,
        verticalBias: Float = this.verticalBias,
        depthBias: Float = this.depthBias,
    ): SpatialBiasAlignment =
        SpatialBiasAlignment(
            horizontalBias = horizontalBias,
            verticalBias = verticalBias,
            depthBias = depthBias,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpatialBiasAlignment) return false

        if (horizontalBias != other.horizontalBias) return false
        if (verticalBias != other.verticalBias) return false
        if (depthBias != other.depthBias) return false

        return true
    }

    override fun hashCode(): Int {
        var result = horizontalBias.hashCode()
        result = 31 * result + verticalBias.hashCode()
        result = 31 * result + depthBias.hashCode()
        return result
    }

    override fun toString(): String {
        return "SpatialBiasAlignment(horizontalBias=$horizontalBias, verticalBias=$verticalBias, depthBias=$depthBias)"
    }

    /**
     * Creates a weighted alignment that specifies a horizontal bias.
     *
     * @param horizontalBias Must be within the range of [-1, 1] with -1 being start and 1 being
     *   end.
     */
    public class Horizontal(@param:FloatRange(-1.0, 1.0) public val horizontalBias: Float) :
        SpatialAlignment.Horizontal {
        override fun offset(width: Int, space: Int, layoutDirection: LayoutDirection): Int =
            offset(horizontalBias, width, space, layoutDirection)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Horizontal) return false

            if (horizontalBias != other.horizontalBias) return false

            return true
        }

        override fun hashCode(): Int {
            return horizontalBias.hashCode()
        }

        override fun toString(): String {
            return "SpatialBiasAlignment#Horizontal(horizontalBias=$horizontalBias)"
        }

        public fun copy(bias: Float = this.horizontalBias): Horizontal =
            Horizontal(horizontalBias = bias)
    }

    /**
     * Creates a weighted alignment that specifies a vertical bias.
     *
     * @param verticalBias Must be within the range of [-1, 1] with -1 being bottom and 1 being top.
     */
    public class Vertical(@param:FloatRange(-1.0, 1.0) public val verticalBias: Float) :
        SpatialAlignment.Vertical {
        override fun offset(height: Int, space: Int): Int = offset(verticalBias, height, space)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Vertical) return false

            if (verticalBias != other.verticalBias) return false

            return true
        }

        override fun hashCode(): Int {
            return verticalBias.hashCode()
        }

        override fun toString(): String {
            return "SpatialBiasAlignment#Vertical(verticalBias=$verticalBias)"
        }

        public fun copy(bias: Float = this.verticalBias): Vertical = Vertical(verticalBias = bias)
    }

    /**
     * Creates a weighted alignment that specifies a depth bias.
     *
     * @param depthBias Must be within the range of [-1, 1] with -1 being back and 1 being front.
     */
    public class Depth(@param:FloatRange(-1.0, 1.0) public val depthBias: Float) :
        SpatialAlignment.Depth {
        override fun offset(depth: Int, space: Int): Int = offset(depthBias, depth, space)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Depth) return false

            if (depthBias != other.depthBias) return false

            return true
        }

        override fun hashCode(): Int {
            return depthBias.hashCode()
        }

        override fun toString(): String {
            return "SpatialBiasAlignment#Depth(depthBias=$depthBias)"
        }

        public fun copy(bias: Float = this.depthBias): Depth = Depth(depthBias = bias)
    }

    public companion object {
        private fun offset(bias: Float, size: Int, space: Int): Int =
            ((space - size) / 2.0f * bias).roundToInt()

        private fun offset(
            bias: Float,
            size: Int,
            space: Int,
            layoutDirection: LayoutDirection,
        ): Int {
            val center = (space - size) / 2.0f
            val biasWithLayoutDirection =
                if (layoutDirection == LayoutDirection.Ltr) bias else -1 * bias
            return (center * biasWithLayoutDirection).roundToInt()
        }
    }
}

/**
 * Positions content in 3D space using horizontal, vertical, and depth bias.
 *
 * Bias refers to a normalized floating-point value, typically within the range of [-1.0f, 1.0f],
 * that determines how a child's content is positioned along a specific axis relative to its
 * parent's available space.
 * - A bias of 0.0f centers the content along that axis.
 * - A bias of -1.0f aligns the content to one extreme (e.g., left for horizontal, bottom for
 *   vertical, back for depth).
 * - A bias of 1.0f aligns the content to the opposite extreme (e.g., right for horizontal, top for
 *   vertical, front for depth).
 * - Values between -1.0f and 1.0f linearly interpolate the position between these extremes.
 *
 * Unlike [SpatialBiasAlignment], the [horizontalBias] in this alignment is absolute; -1.0f always
 * means left and 1.0f` always means right, irrespective of the current [LayoutDirection].
 *
 * @param horizontalBias Must be within the range of [-1, 1] with -1 being left and 1 being right.
 * @param verticalBias Must be within the range of [-1, 1] with -1 being bottom and 1 being top.
 * @param depthBias Must be within the range of [-1, 1] with -1 being back and 1 being front.
 */
public class SpatialBiasAbsoluteAlignment(
    @param:FloatRange(-1.0, 1.0) public val horizontalBias: Float,
    @param:FloatRange(-1.0, 1.0) public val verticalBias: Float,
    @param:FloatRange(-1.0, 1.0) public val depthBias: Float,
) : SpatialAlignment {
    override fun horizontalOffset(width: Int, space: Int, layoutDirection: LayoutDirection): Int =
        offset(this@SpatialBiasAbsoluteAlignment.horizontalBias, width, space)

    override fun verticalOffset(height: Int, space: Int): Int = offset(verticalBias, height, space)

    override fun depthOffset(depth: Int, space: Int): Int = offset(depthBias, depth, space)

    override fun position(
        size: IntVolumeSize,
        space: IntVolumeSize,
        layoutDirection: LayoutDirection,
    ): Vector3 =
        Vector3(
            horizontalOffset(size.width, space.width, layoutDirection).toFloat(),
            verticalOffset(size.height, space.height).toFloat(),
            depthOffset(size.depth, space.depth).toFloat(),
        )

    public fun copy(
        horizontalBias: Float = this.horizontalBias,
        verticalBias: Float = this.verticalBias,
        depthBias: Float = this.depthBias,
    ): SpatialBiasAbsoluteAlignment =
        SpatialBiasAbsoluteAlignment(
            horizontalBias = horizontalBias,
            verticalBias = verticalBias,
            depthBias = depthBias,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpatialBiasAbsoluteAlignment) return false

        if (horizontalBias != other.horizontalBias) return false
        if (verticalBias != other.verticalBias) return false
        if (depthBias != other.depthBias) return false

        return true
    }

    override fun hashCode(): Int {
        var result = horizontalBias.hashCode()
        result = 31 * result + verticalBias.hashCode()
        result = 31 * result + depthBias.hashCode()
        return result
    }

    override fun toString(): String {
        return "SpatialBiasAbsoluteAlignment(horizontalBias=$horizontalBias, verticalBias=$verticalBias, depthBias=$depthBias)"
    }

    /**
     * Creates a weighted alignment that specifies a horizontal bias and independent of layout
     * direction
     *
     * @param horizontalBias Must be within the range of [-1, 1] with -1 being left and 1 being
     *   right.
     */
    public class Horizontal(@param:FloatRange(-1.0, 1.0) public val horizontalBias: Float) :
        SpatialAlignment.Horizontal {
        override fun offset(width: Int, space: Int, layoutDirection: LayoutDirection): Int =
            offset(horizontalBias, width, space)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Horizontal) return false

            if (horizontalBias != other.horizontalBias) return false

            return true
        }

        override fun hashCode(): Int {
            return horizontalBias.hashCode()
        }

        override fun toString(): String {
            return "SpatialBiasAbsoluteAlignment#Horizontal(horizontalBias=$horizontalBias)"
        }

        public fun copy(bias: Float = this.horizontalBias): Horizontal =
            Horizontal(horizontalBias = bias)
    }

    public companion object {
        private fun offset(bias: Float, size: Int, space: Int): Int =
            ((space - size) / 2.0f * bias).roundToInt()
    }
}
