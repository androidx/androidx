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

/** Represents the offset of an object in 3D space. */
@Immutable
public class IntVolumeOffset(public val x: Int, public val y: Int, public val z: Int) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IntVolumeOffset

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + z
        return result
    }

    override fun toString(): String {
        return "IntVolumeOffset(x=$x, y=$y, z=$z)"
    }

    /** Contains a common constant */
    public companion object {
        /** A [IntVolumeOffset] with all offsets set to 0. */
        public val Zero: IntVolumeOffset = IntVolumeOffset(0, 0, 0)
    }
}
