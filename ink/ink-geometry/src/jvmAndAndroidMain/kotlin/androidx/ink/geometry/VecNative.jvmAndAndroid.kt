/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.geometry

import androidx.annotation.FloatRange
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

@UsedByNative
actual internal object VecNative {

    init {
        NativeLoader.load()
    }

    @UsedByNative actual external fun unitVec(vecX: Float, vecY: Float): ImmutableVec

    @UsedByNative actual external fun populateUnitVec(vecX: Float, vecY: Float, output: MutableVec)

    @UsedByNative
    @AngleDegreesFloat
    @FloatRange(from = 0.0, to = 180.0)
    actual external fun absoluteAngleBetweenInDegrees(
        firstVecX: Float,
        firstVecY: Float,
        secondVecX: Float,
        secondVecY: Float,
    ): Float

    @UsedByNative
    @AngleDegreesFloat
    @FloatRange(from = -180.0, to = 180.0, fromInclusive = false)
    actual external fun signedAngleBetweenInDegrees(
        firstVecX: Float,
        firstVecY: Float,
        secondVecX: Float,
        secondVecY: Float,
    ): Float
}
