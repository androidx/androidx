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

package androidx.xr.compose.unit

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Represents the offset of an object in 3D space. */
@Immutable
public class DpVolumeOffset(
    public val x: Dp = 0.dp,
    public val y: Dp = 0.dp,
    public val z: Dp = 0.dp,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DpVolumeOffset

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        return result
    }

    override fun toString(): String {
        return "DpVolumeOffset(x=$x, y=$y, z=$z)"
    }

    /** Contains a common constant */
    public companion object {
        /** A [DpVolumeOffset] with all offsets set to 0. */
        public val Zero: DpVolumeOffset = DpVolumeOffset(0.dp, 0.dp, 0.dp)
    }
}
