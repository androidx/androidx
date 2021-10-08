/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.unit

import android.util.SizeF
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * A two-dimensional Size using [Dp] for units.
 *
 * @property width The horizontal aspect of the size in [Dp]
 * @property height The vertical aspect of the size in [Dp]
 */
@Immutable
public class DpSize(
    public val width: Dp,
    public val height: Dp,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DpSize

        if (width != other.width) return false
        if (height != other.height) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width.hashCode()
        result = 31 * result + height.hashCode()
        return result
    }

    override fun toString(): String {
        return "DpSize(width=$width, height=$height)"
    }

    /**
     * Returns a copy of this [DpSize] instance optionally overriding the
     * width or height parameter
     */
    fun copy(width: Dp = this.width, height: Dp = this.height) =
        DpSize(width, height)

    /**
     * Adds a [DpSize] to another one.
     */
    @Stable
    public operator fun plus(other: DpSize): DpSize =
        DpSize(width + other.width, height + other.height)

    /**
     * Subtracts a [DpSize] from another one.
     */
    @Stable
    public operator fun minus(other: DpSize): DpSize =
        DpSize(width - other.width, height - other.height)

    @Stable
    public operator fun component1(): Dp = width

    @Stable
    public operator fun component2(): Dp = height

    /**
     * Multiplies the components of a [DpSize] by a constant factor.
     */
    @Stable
    public operator fun times(other: Int): DpSize = DpSize(width * other, height * other)

    /**
     * Multiplies the components of a [DpSize] by a constant factor.
     */
    @Stable
    public operator fun times(other: Float): DpSize = DpSize(width * other, height * other)

    /**
     * Divides the components of a [DpSize] by a constant factor.
     */
    @Stable
    public operator fun div(other: Int): DpSize = DpSize(width / other, height / other)

    /**
     * Divides the components of a [DpSize] by a constant factor.
     */
    @Stable
    public operator fun div(other: Float): DpSize = DpSize(width / other, height / other)

    companion object {
        /**
         * A [DpSize] with 0 DP [width] and 0 DP [height] values.
         */
        val Zero = DpSize(0.dp, 0.dp)
    }
}

/**
 * Multiplies the components of a [DpSize] by a constant factor.
 */
@Stable
public operator fun Int.times(size: DpSize) = size * this

/**
 * Multiplies the components of a [DpSize] by a constant factor.
 */
@Stable
public operator fun Float.times(size: DpSize) = size * this

/**
 * Creates a SizeF with the same values.
 */
@Stable
public fun DpSize.toSizeF(): SizeF = SizeF(width.value, height.value)
