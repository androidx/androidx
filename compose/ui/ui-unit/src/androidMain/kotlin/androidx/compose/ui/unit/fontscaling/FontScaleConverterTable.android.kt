/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.compose.ui.unit.fontscaling

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import java.util.Arrays
import kotlin.math.sign

/**
 * A lookup table for non-linear font scaling. Converts font sizes given in "sp" dimensions to a
 * "dp" dimension according to a non-linear curve by interpolating values in a lookup table.
 *
 * {@see FontScaleConverter}
 */
// TODO(b/294384826): move these into core:core when the FontScaleConverter APIs are available.
//  These are temporary shims until core and platform are in a stable state.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FontScaleConverterTable @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
/**
 * Creates a lookup table for the given conversions.
 *
 * Any "sp" value not in the lookup table will be derived via linear interpolation.
 *
 * The arrays must be sorted ascending and monotonically increasing.
 *
 * @param fromSp array of dimensions in SP
 * @param toDp array of dimensions in DP that correspond to an SP value in fromSp
 *
 * @throws IllegalArgumentException if the array lengths don't match or are empty
 */
constructor(
    fromSp: FloatArray,
    toDp: FloatArray
) : FontScaleConverter {
    @VisibleForTesting
    val mFromSpValues: FloatArray

    @VisibleForTesting
    val mToDpValues: FloatArray

    init {
        require(!(fromSp.size != toDp.size || fromSp.isEmpty())) {
            "Array lengths must match and be nonzero"
        }
        mFromSpValues = fromSp
        mToDpValues = toDp
    }

    override fun convertDpToSp(dp: Float): Float {
        return lookupAndInterpolate(dp, mToDpValues, mFromSpValues)
    }

    override fun convertSpToDp(sp: Float): Float {
        return lookupAndInterpolate(sp, mFromSpValues, mToDpValues)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other !is FontScaleConverterTable) return false
        return (mFromSpValues.contentEquals(other.mFromSpValues) &&
            mToDpValues.contentEquals(other.mToDpValues))
    }

    override fun hashCode(): Int {
        var result = mFromSpValues.contentHashCode()
        result = 31 * result + mToDpValues.contentHashCode()
        return result
    }

    override fun toString(): String {
        return ("FontScaleConverter{" +
            "fromSpValues=" + mFromSpValues.contentToString() +
            ", toDpValues=" + mToDpValues.contentToString() +
            '}')
    }

    companion object {
        private fun lookupAndInterpolate(
            sourceValue: Float,
            sourceValues: FloatArray,
            targetValues: FloatArray
        ): Float {
            val sourceValuePositive = Math.abs(sourceValue)
            // TODO(b/247861374): find a match at a higher index?
            val sign = sign(sourceValue)
            // We search for exact matches only, even if it's just a little off. The interpolation will
            // handle any non-exact matches.
            val index = Arrays.binarySearch(sourceValues, sourceValuePositive)
            return if (index >= 0) {
                // exact match, return the matching dp
                sign * targetValues[index]
            } else {
                // must be a value in between index and index + 1: interpolate.
                val lowerIndex = -(index + 1) - 1
                val startSp: Float
                val endSp: Float
                val startDp: Float
                val endDp: Float
                if (lowerIndex >= sourceValues.size - 1) {
                    // It's past our lookup table. Determine the last elements' scaling factor and use.
                    startSp = sourceValues[sourceValues.size - 1]
                    startDp = targetValues[sourceValues.size - 1]
                    if (startSp == 0f) return 0f
                    val scalingFactor = startDp / startSp
                    return sourceValue * scalingFactor
                } else if (lowerIndex == -1) {
                    // It's smaller than the smallest value in our table. Interpolate from 0.
                    startSp = 0f
                    startDp = 0f
                    endSp = sourceValues[0]
                    endDp = targetValues[0]
                } else {
                    startSp = sourceValues[lowerIndex]
                    endSp = sourceValues[lowerIndex + 1]
                    startDp = targetValues[lowerIndex]
                    endDp = targetValues[lowerIndex + 1]
                }
                sign * MathUtils.constrainedMap(
                    startDp,
                    endDp,
                    startSp,
                    endSp,
                    sourceValuePositive
                )
            }
        }
    }
}
