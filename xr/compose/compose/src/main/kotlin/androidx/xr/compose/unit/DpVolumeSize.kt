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

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Represents the size of a volume in density-independent pixels ([Dp]).
 *
 * This class provides a convenient way to store and manipulate the [width], [height], and [depth]
 * of a 3D volume in Dp. It also provides methods to convert to and from
 * [androidx.xr.scenecore.runtime.Dimensions] in meters.
 *
 * @property width the size of the volume along the x dimension, in Dp.
 * @property height the size of the volume along the y dimension, in Dp.
 * @property depth the size of the volume along the z dimension, in Dp. Panels have 0 depth and
 *   cannot be set to non-zero depth.
 */
public class DpVolumeSize(public val width: Dp, public val height: Dp, public val depth: Dp) {

    override fun toString(): String {
        return "DpVolumeSize(width=$width, height=$height, depth=$depth)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DpVolumeSize) return false

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

    /** Contains a common constant */
    public companion object {
        /** A [DpVolumeSize] with all dimensions set to 0.dp. */
        public val Zero: DpVolumeSize = DpVolumeSize(0.dp, 0.dp, 0.dp)
    }
}
