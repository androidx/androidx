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

import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

@UsedByNative
actual internal object ParallelogramNative {

    init {
        NativeLoader.load()
    }

    @UsedByNative
    actual external fun createBoundingBox(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotationDegrees: Float,
        skew: Float,
    ): ImmutableBox

    @UsedByNative
    actual external fun populateBoundingBox(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotationDegrees: Float,
        skew: Float,
        outBox: MutableBox,
    )

    @UsedByNative
    actual external fun createSemiAxes(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotationDegrees: Float,
        skew: Float,
    ): Array<ImmutableVec>

    @UsedByNative
    actual external fun populateSemiAxes(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotationDegrees: Float,
        skew: Float,
        outAxis1: MutableVec,
        outAxis2: MutableVec,
    )

    @UsedByNative
    actual external fun createCorners(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotationDegrees: Float,
        skew: Float,
    ): Array<ImmutableVec>

    @UsedByNative
    actual external fun populateCorners(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotationDegrees: Float,
        skew: Float,
        outCorner1: MutableVec,
        outCorner2: MutableVec,
        outCorner3: MutableVec,
        outCorner4: MutableVec,
    )

    @UsedByNative
    actual external fun contains(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotationDegrees: Float,
        skew: Float,
        pointX: Float,
        pointY: Float,
    ): Boolean
}
