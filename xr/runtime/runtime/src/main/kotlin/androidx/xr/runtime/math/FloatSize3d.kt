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

/**
 * Size of a 3d object represented as a Float, such as the dimensions of a spatial volume in meters.
 */
public class FloatSize3d(
    public val width: Float = 0f,
    public val height: Float = 0f,
    public val depth: Float = 0f,
) {

    /** Returns a new [FloatSize2d] with the same `width` and `height` as this FloatSize3d. */
    public fun to2d(): FloatSize2d = FloatSize2d(width, height)

    override fun toString(): String {
        return super.toString() + ": w $width x h $height x d $depth"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FloatSize3d

        if (width != other.width) return false
        if (height != other.height) return false
        if (depth != other.depth) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width.hashCode()
        result = 31 * result + height.hashCode()
        result = 31 * result + depth.hashCode()
        return result
    }

    /** Returns a new [FloatSize3d] that's uniformly divided by the `divisor`. */
    public operator fun div(divisor: Float): FloatSize3d {
        return FloatSize3d(this.width / divisor, this.height / divisor, this.depth / divisor)
    }

    /** Returns a new [FloatSize3d] that's uniformly divided by the `divisor`. */
    public operator fun div(divisor: Int): FloatSize3d {
        return this / divisor.toFloat()
    }

    /** Returns a new [FloatSize3d] that's uniformly multiplied by the `scalar`. */
    public operator fun times(scalar: Float): FloatSize3d {
        return FloatSize3d(this.width * scalar, this.height * scalar, this.depth * scalar)
    }

    /** Returns a new [FloatSize3d] that's uniformly multiplied by the `scalar`. */
    public operator fun times(scalar: Int): FloatSize3d {
        return this * scalar.toFloat()
    }

    public companion object {
        /**
         * Creates a [FloatSize3d] from a [Vector3], using the Vector3's x, y, and z components as
         * the width, height, and depth respectively.
         *
         * @param vector The source [Vector3].
         * @return A new [FloatSize3d] instance.
         */
        @JvmStatic
        public fun fromVector3(vector: Vector3): FloatSize3d {
            return FloatSize3d(vector.x, vector.y, vector.z)
        }
    }
}
