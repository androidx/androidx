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

import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.runtime.math.Vector3
import kotlin.math.roundToInt

/**
 * An interface to calculate the position of a sized box inside of an available 3D space.
 * [SpatialAlignment] is often used to define the alignment of a layout inside a parent layout.
 *
 * @see SpatialBiasAlignment
 */
public interface SpatialAlignment {
    /**
     * Provides the horizontal offset from the origin of the space to the origin of the content.
     *
     * @param width The content width in pixels.
     * @param space The available space in pixels.
     */
    public fun horizontalOffset(width: Int, space: Int): Int

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
     */
    public fun position(size: IntVolumeSize, space: IntVolumeSize): Vector3

    /**
     * An interface to calculate the position of a box of a certain width inside an available width.
     */
    public interface Horizontal {
        /** @see horizontalOffset */
        public fun offset(width: Int, space: Int): Int
    }

    /**
     * An interface to calculate the position of a box of a certain height inside an available
     * height.
     */
    public interface Vertical {
        /** @see verticalOffset */
        public fun offset(height: Int, space: Int): Int
    }

    /**
     * An interface to calculate the position of a box of a certain depth inside an available depth.
     */
    public interface Depth {
        /** @see depthOffset */
        public fun offset(depth: Int, space: Int): Int
    }

    public companion object {
        // 2D alignments
        @JvmStatic public val TopLeft: SpatialAlignment = SpatialBiasAlignment(-1f, 1f, 0f)
        @JvmStatic public val TopCenter: SpatialAlignment = SpatialBiasAlignment(0f, 1f, 0f)
        @JvmStatic public val TopRight: SpatialAlignment = SpatialBiasAlignment(1f, 1f, 0f)
        @JvmStatic public val CenterLeft: SpatialAlignment = SpatialBiasAlignment(-1f, 0f, 0f)
        @JvmStatic public val Center: SpatialAlignment = SpatialBiasAlignment(0f, 0f, 0f)
        @JvmStatic public val CenterRight: SpatialAlignment = SpatialBiasAlignment(1f, 0f, 0f)
        @JvmStatic public val BottomLeft: SpatialAlignment = SpatialBiasAlignment(-1f, -1f, 0f)
        @JvmStatic public val BottomCenter: SpatialAlignment = SpatialBiasAlignment(0f, -1f, 0f)
        @JvmStatic public val BottomRight: SpatialAlignment = SpatialBiasAlignment(1f, -1f, 0f)

        // Horizontal alignments
        @JvmStatic
        public val Left: SpatialBiasAlignment.Horizontal = SpatialBiasAlignment.Horizontal(-1f)
        @JvmStatic
        public val CenterHorizontally: SpatialBiasAlignment.Horizontal =
            SpatialBiasAlignment.Horizontal(0f)
        @JvmStatic
        public val Right: SpatialBiasAlignment.Horizontal = SpatialBiasAlignment.Horizontal(1f)

        // Vertical alignments
        @JvmStatic
        public val Bottom: SpatialBiasAlignment.Vertical = SpatialBiasAlignment.Vertical(-1f)
        @JvmStatic
        public val CenterVertically: SpatialBiasAlignment.Vertical =
            SpatialBiasAlignment.Vertical(0f)
        @JvmStatic public val Top: SpatialBiasAlignment.Vertical = SpatialBiasAlignment.Vertical(1f)

        // Depth alignments
        @JvmStatic public val Back: SpatialBiasAlignment.Depth = SpatialBiasAlignment.Depth(-1f)
        @JvmStatic
        public val CenterDepthwise: SpatialBiasAlignment.Depth = SpatialBiasAlignment.Depth(0f)
        @JvmStatic public val Front: SpatialBiasAlignment.Depth = SpatialBiasAlignment.Depth(1f)
    }
}

/**
 * Creates a weighted alignment that specifies a horizontal, vertical, and depth bias.
 *
 * @param horizontalBias Must be within the range of [-1, 1] with -1 being left and 1 being right.
 * @param verticalBias Must be within the range of [-1, 1] with -1 being bottom and 1 being top.
 * @param depthBias Must be within the range of [-1, 1] with -1 being back and 1 being front.
 */
public class SpatialBiasAlignment(
    public val horizontalBias: Float,
    public val verticalBias: Float,
    public val depthBias: Float,
) : SpatialAlignment {
    override fun horizontalOffset(width: Int, space: Int): Int =
        offset(horizontalBias, width, space)

    override fun verticalOffset(height: Int, space: Int): Int = offset(verticalBias, height, space)

    override fun depthOffset(depth: Int, space: Int): Int = offset(depthBias, depth, space)

    override fun position(size: IntVolumeSize, space: IntVolumeSize): Vector3 =
        Vector3(
            horizontalOffset(size.width, space.width).toFloat(),
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
     * @param bias Must be within the range of [-1, 1] with -1 being left and 1 being right.
     */
    public class Horizontal(public val bias: Float) : SpatialAlignment.Horizontal {
        override fun offset(width: Int, space: Int): Int = offset(bias, width, space)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Horizontal) return false

            if (bias != other.bias) return false

            return true
        }

        override fun hashCode(): Int {
            return bias.hashCode()
        }

        override fun toString(): String {
            return "Horizontal(bias=$bias)"
        }

        public fun copy(bias: Float = this.bias): Horizontal = Horizontal(bias = bias)
    }

    /**
     * Creates a weighted alignment that specifies a vertical bias.
     *
     * @param bias Must be within the range of [-1, 1] with -1 being bottom and 1 being top.
     */
    public class Vertical(public val bias: Float) : SpatialAlignment.Vertical {
        override fun offset(height: Int, space: Int): Int = offset(bias, height, space)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Vertical) return false

            if (bias != other.bias) return false

            return true
        }

        override fun hashCode(): Int {
            return bias.hashCode()
        }

        override fun toString(): String {
            return "Vertical(bias=$bias)"
        }

        public fun copy(bias: Float = this.bias): Vertical = Vertical(bias = bias)
    }

    /**
     * Creates a weighted alignment that specifies a depth bias.
     *
     * @param bias Must be within the range of [-1, 1] with -1 being back and 1 being front.
     */
    public class Depth(public val bias: Float) : SpatialAlignment.Depth {
        override fun offset(depth: Int, space: Int): Int = offset(bias, depth, space)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Depth) return false

            if (bias != other.bias) return false

            return true
        }

        override fun hashCode(): Int {
            return bias.hashCode()
        }

        override fun toString(): String {
            return "Depth(bias=$bias)"
        }

        public fun copy(bias: Float = this.bias): Depth = Depth(bias = bias)
    }

    public companion object {
        private fun offset(bias: Float, size: Int, space: Int): Int =
            ((space - size) / 2.0f * bias).roundToInt()
    }
}
