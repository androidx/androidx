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

import androidx.xr.runtime.math.FloatSize3d

/**
 * Represents the size of a volume in pixels.
 *
 * This class provides a convenient way to store and manipulate the [width], [height], and [depth]
 * of a 3D volume in pixels. It also provides methods to convert to and from [FloatSize3d] in
 * meters.
 *
 * Note: As with all [Int] values in Compose XR, the values in this class represent pixels.
 *
 * @property width the size of the volume along the x dimension, in pixels.
 * @property height the size of the volume along the y dimension, in pixels.
 * @property depth the size of the volume along the z dimension, in pixels. Panels have 0 depth and
 *   cannot be set to non-zero depth.
 */
public class IntVolumeSize(public val width: Int, public val height: Int, public val depth: Int) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IntVolumeSize) return false

        if (width != other.width) return false
        if (height != other.height) return false
        if (depth != other.depth) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + depth
        return result
    }

    override fun toString(): String {
        return "IntVolumeSize(width=$width, height=$height, depth=$depth)"
    }

    /** Contains common constants and factory methods for creating [IntVolumeSize] objects */
    public companion object {
        /** An [IntVolumeSize] with all dimensions set to 0. */
        public val Zero: IntVolumeSize = IntVolumeSize(0, 0, 0)
    }
}
