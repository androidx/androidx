/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.tv.foundation

import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable

/**
 * Holds the offsets needed for scrolling-with-offset.
 *
 * @property parentFraction defines the offset of the starting edge of the child
 * element from the starting edge of the parent element. This value should be between 0 and 1.
 * @property childFraction defines the offset of the starting edge of the child from
 * the pivot defined by parentFraction. This value should be between 0 and 1.
 */
@Immutable
class PivotOffsets constructor(
    @FloatRange(
        from = 0.0,
        to = 1.0,
        fromInclusive = true,
        toInclusive = true
    ) val parentFraction: Float = 0.3f,
    @FloatRange(
        from = 0.0,
        to = 1.0,
        fromInclusive = true,
        toInclusive = true
    ) val childFraction: Float = 0f,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PivotOffsets) return false

        if (parentFraction != other.parentFraction) return false
        if (childFraction != other.childFraction) return false

        return true
    }

    override fun hashCode(): Int {
        var result = parentFraction.hashCode()
        result = 31 * result + childFraction.hashCode()
        return result
    }
}
