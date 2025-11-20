/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.runtime.math

/** Size of a 2d object represented as a Float, such as the dimensions of a panel in meters. */
public class FloatSize2d(public val width: Float = 0f, public val height: Float = 0f) {
    override fun toString(): String {
        return super.toString() + ": w $width x h $height"
    }

    /**
     * Returns a new [FloatSize3d] with the same `width` and `height` of this [FloatSize2d], and the
     * given `depth`.
     */
    public fun to3d(depth: Float = 0f): FloatSize3d = FloatSize3d(width, height, depth)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FloatSize2d

        if (width != other.width) return false
        if (height != other.height) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width.hashCode()
        result = 31 * result + height.hashCode()
        return result
    }

    /** Returns a new [FloatSize2d] that's uniformly divided by the `divisor`. */
    public operator fun div(divisor: Float): FloatSize2d {
        return FloatSize2d(this.width / divisor, this.height / divisor)
    }

    /** Returns a new [FloatSize2d] that's uniformly divided by the `divisor`. */
    public operator fun div(divisor: Int): FloatSize2d {
        return this / divisor.toFloat()
    }

    /** Returns a new [FloatSize2d] that's uniformly multiplied by the `scalar`. */
    public operator fun times(scalar: Float): FloatSize2d {
        return FloatSize2d(this.width * scalar, this.height * scalar)
    }

    /** Returns a new [FloatSize2d] that's uniformly multiplied by the `scalar`. */
    public operator fun times(scalar: Int): FloatSize2d {
        return this * scalar.toFloat()
    }
}
