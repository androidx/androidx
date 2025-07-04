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

package androidx.xr.compose.unit

import androidx.annotation.IntRange

/**
 * Defines constraints for a 3D volume, specifying minimum and maximum values for width, height, and
 * depth.
 *
 * This class is similar in concept to [androidx.compose.ui.unit.Constraints], but adapted for 3D
 * volumes.
 *
 * @property minWidth the minimum allowed width in pixels.
 * @property maxWidth the maximum allowed width in pixels, use [INFINITY] to indicate no maximum.
 * @property minHeight the minimum allowed height in pixels.
 * @property maxHeight the maximum allowed height in pixels. Can be [INFINITY] to indicate no
 *   maximum.
 * @property minDepth the minimum allowed depth in pixels. Defaults to 0.
 * @property maxDepth the maximum allowed depth in pixels. Can be [INFINITY] to indicate no maximum.
 *   Defaults to [INFINITY].
 */
public class VolumeConstraints(
    @IntRange(from = 0) public val minWidth: Int = 0,
    @IntRange(from = 0) public val maxWidth: Int = INFINITY,
    @IntRange(from = 0) public val minHeight: Int = 0,
    @IntRange(from = 0) public val maxHeight: Int = INFINITY,
    @IntRange(from = 0) public val minDepth: Int = 0,
    @IntRange(from = 0) public val maxDepth: Int = INFINITY,
) {
    init {
        require(maxWidth >= minWidth) { "maxWidth ($maxWidth) must be >= minWidth ($minWidth)." }
        require(maxHeight >= minHeight) {
            "maxHeight ($maxHeight) must be >= minHeight ($minHeight)."
        }
        require(maxDepth >= minDepth) { "maxDepth ($maxDepth) must be >= minDepth ($minDepth)." }
    }

    /** Indicates whether the width is bounded (has a maximum value other than [INFINITY]). */
    @get:JvmName("hasBoundedWidth")
    public val hasBoundedWidth: Boolean
        get() {
            return maxWidth != INFINITY
        }

    /** Indicates whether the height is bounded (has a maximum value other than [INFINITY]). */
    @get:JvmName("hasBoundedHeight")
    public val hasBoundedHeight: Boolean
        get() {
            return maxHeight != INFINITY
        }

    /** Indicates whether the depth is bounded (has a maximum value other than [INFINITY]). */
    @get:JvmName("hasBoundedDepth")
    public val hasBoundedDepth: Boolean
        get() {
            return maxDepth != INFINITY
        }

    /** Returns a string representation of the [VolumeConstraints]. */
    override fun toString(): String =
        "width: $minWidth-$maxWidth, height: $minHeight-$maxHeight, depth=$minDepth-$maxDepth"

    /** Checks if this [VolumeConstraints] object is equal to [other] object. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VolumeConstraints) return false

        if (minWidth != other.minWidth) return false
        if (maxWidth != other.maxWidth) return false
        if (minHeight != other.minHeight) return false
        if (maxHeight != other.maxHeight) return false
        if (minDepth != other.minDepth) return false
        if (maxDepth != other.maxDepth) return false

        return true
    }

    /** Calculates a hash code for this [VolumeConstraints] object. */
    override fun hashCode(): Int {
        var result = minWidth
        result = 31 * result + maxWidth
        result = 31 * result + minHeight
        result = 31 * result + maxHeight
        result = 31 * result + minDepth
        result = 31 * result + maxDepth
        return result
    }

    /**
     * Creates a copy of this [VolumeConstraints] object with modifications to its properties.
     *
     * @param minWidth the minimum allowed width in the new constraints.
     * @param maxWidth the maximum allowed width in the new constraints.
     * @param minHeight the minimum allowed height in the new constraints.
     * @param maxHeight the maximum allowed height in the new constraints.
     * @param minDepth the minimum allowed depth in the new constraints.
     * @param maxDepth the maximum allowed depth in the new constraints.
     * @return a new [VolumeConstraints] object with the specified modifications.
     */
    public fun copy(
        minWidth: Int = this.minWidth,
        maxWidth: Int = this.maxWidth,
        minHeight: Int = this.minHeight,
        maxHeight: Int = this.maxHeight,
        minDepth: Int = this.minDepth,
        maxDepth: Int = this.maxDepth,
    ): VolumeConstraints =
        VolumeConstraints(
            minWidth = minWidth,
            maxWidth = maxWidth,
            minHeight = minHeight,
            maxHeight = maxHeight,
            minDepth = minDepth,
            maxDepth = maxDepth,
        )

    public companion object {
        /** Represents an unbounded (infinite) constraint value. */
        public const val INFINITY: Int = Int.MAX_VALUE
    }
}

/**
 * Constrains the dimensions of this [VolumeConstraints] object to fit within the bounds of the
 * other [VolumeConstraints] object.
 *
 * @param otherConstraints the other [VolumeConstraints] to constrain against.
 * @return a new [VolumeConstraints] object with dimensions constrained within the bounds of
 *   [otherConstraints].
 */
public fun VolumeConstraints.constrain(otherConstraints: VolumeConstraints): VolumeConstraints =
    VolumeConstraints(
        minWidth = otherConstraints.minWidth.coerceIn(minWidth, maxWidth),
        maxWidth = otherConstraints.maxWidth.coerceIn(minWidth, maxWidth),
        minHeight = otherConstraints.minHeight.coerceIn(minHeight, maxHeight),
        maxHeight = otherConstraints.maxHeight.coerceIn(minHeight, maxHeight),
        minDepth = otherConstraints.minDepth.coerceIn(minDepth, maxDepth),
        maxDepth = otherConstraints.maxDepth.coerceIn(minDepth, maxDepth),
    )

/**
 * Constrains a given [width] value to fit within the bounds of this [VolumeConstraints] object.
 *
 * @param width the width value to constrain.
 * @return the constrained width value, ensuring it's within the minimum and maximum width bounds.
 */
public fun VolumeConstraints.constrainWidth(width: Int): Int = width.coerceIn(minWidth, maxWidth)

/**
 * Constrains a given height value to fit within the bounds of this [VolumeConstraints] object.
 *
 * @param height the height value to constrain.
 * @return the constrained height value, ensuring it's within the minimum and maximum height bounds.
 */
public fun VolumeConstraints.constrainHeight(height: Int): Int =
    height.coerceIn(minHeight, maxHeight)

/**
 * Constrains a given depth value to fit within the bounds of this [VolumeConstraints] object.
 *
 * @param depth the depth value to constrain.
 * @return the constrained depth value, ensuring it's within the minimum and maximum depth bounds.
 */
public fun VolumeConstraints.constrainDepth(depth: Int): Int = depth.coerceIn(minDepth, maxDepth)

/**
 * Creates a new [VolumeConstraints] object by offsetting the minimum and maximum values of this
 * one.
 *
 * @param horizontal the horizontal offset to apply.
 * @param vertical the vertical offset to apply.
 * @param depth the depth offset to apply.
 * @param resetMins if true, the minimum values in the new constraints will be set to 0, otherwise.
 *   they will be offset.
 * @return a new [VolumeConstraints] object with offset values.
 */
public fun VolumeConstraints.offset(
    horizontal: Int = 0,
    vertical: Int = 0,
    depth: Int = 0,
    resetMins: Boolean = false,
): VolumeConstraints =
    VolumeConstraints(
        if (resetMins) 0 else (minWidth + horizontal).coerceAtLeast(0),
        addMaxWithMinimum(maxWidth, horizontal),
        if (resetMins) 0 else (minHeight + vertical).coerceAtLeast(0),
        addMaxWithMinimum(maxHeight, vertical),
        if (resetMins) 0 else (minDepth + depth).coerceAtLeast(0),
        addMaxWithMinimum(maxDepth, depth),
    )

/** Adds a value to a maximum value, ensuring it stays within the minimum bound. */
private fun addMaxWithMinimum(max: Int, value: Int): Int {
    return if (max == VolumeConstraints.INFINITY) {
        max
    } else {
        (max + value).coerceAtLeast(0)
    }
}
