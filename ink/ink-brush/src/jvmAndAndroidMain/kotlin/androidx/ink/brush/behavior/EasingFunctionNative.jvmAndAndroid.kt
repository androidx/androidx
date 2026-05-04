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

package androidx.ink.brush.behavior

import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

@UsedByNative
internal actual object EasingFunctionNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative actual external fun createCopyOf(otherEasingFunctionNativePointer: Long): Long

    @UsedByNative actual external fun createPredefined(value: Int): Long

    @UsedByNative
    actual external fun createCubicBezier(x1: Float, y1: Float, x2: Float, y2: Float): Long

    @UsedByNative actual external fun createLinear(points: FloatArray): Long

    @UsedByNative actual external fun createSteps(stepCount: Int, stepPosition: Int): Long

    @UsedByNative actual external fun free(nativePointer: Long)

    @UsedByNative actual external fun getParametersType(nativePointer: Long): Int

    // Predefined easing function accessors:

    @UsedByNative actual external fun getPredefinedValueInt(nativePointer: Long): Int

    // Cubic Bezier easing function accessors:

    @UsedByNative actual external fun getCubicBezierX1(nativePointer: Long): Float

    @UsedByNative actual external fun getCubicBezierY1(nativePointer: Long): Float

    @UsedByNative actual external fun getCubicBezierX2(nativePointer: Long): Float

    @UsedByNative actual external fun getCubicBezierY2(nativePointer: Long): Float

    // Linear easing function accessors:

    @UsedByNative actual external fun getLinearNumPoints(nativePointer: Long): Int

    @UsedByNative actual external fun getLinearPointX(nativePointer: Long, index: Int): Float

    @UsedByNative actual external fun getLinearPointY(nativePointer: Long, index: Int): Float

    // Steps easing function accessors:

    @UsedByNative actual external fun getStepsCount(nativePointer: Long): Int

    @UsedByNative actual external fun getStepsPositionInt(nativePointer: Long): Int
}
