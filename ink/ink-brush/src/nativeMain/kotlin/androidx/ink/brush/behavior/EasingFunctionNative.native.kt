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

import androidx.ink.nativeloader.cinterop.EasingFunctionNative_createCopyOf
import androidx.ink.nativeloader.cinterop.EasingFunctionNative_createCubicBezier
import androidx.ink.nativeloader.cinterop.EasingFunctionNative_createLinear
import androidx.ink.nativeloader.cinterop.EasingFunctionNative_createPredefined
import androidx.ink.nativeloader.cinterop.EasingFunctionNative_createSteps
import androidx.ink.nativeloader.cinterop.EasingFunctionNative_free
import androidx.ink.nativeloader.cinterop.EasingFunctionNative_getCubicBezierX1
import androidx.ink.nativeloader.cinterop.EasingFunctionNative_getCubicBezierX2
import androidx.ink.nativeloader.cinterop.EasingFunctionNative_getCubicBezierY1
import androidx.ink.nativeloader.cinterop.EasingFunctionNative_getCubicBezierY2
import androidx.ink.nativeloader.cinterop.EasingFunctionNative_getLinearNumPoints
import androidx.ink.nativeloader.cinterop.EasingFunctionNative_getLinearPointX
import androidx.ink.nativeloader.cinterop.EasingFunctionNative_getLinearPointY
import androidx.ink.nativeloader.cinterop.EasingFunctionNative_getParametersType
import androidx.ink.nativeloader.cinterop.EasingFunctionNative_getPredefinedValueInt
import androidx.ink.nativeloader.cinterop.EasingFunctionNative_getStepsCount
import androidx.ink.nativeloader.cinterop.EasingFunctionNative_getStepsPositionInt
import androidx.ink.nativeloader.throwForNonOkStatusCallback
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

@OptIn(ExperimentalForeignApi::class)
actual internal object EasingFunctionNative {

    actual fun createCopyOf(otherEasingFunctionNativePointer: Long): Long =
        EasingFunctionNative_createCopyOf(otherEasingFunctionNativePointer)

    actual fun createPredefined(value: Int): Long =
        EasingFunctionNative_createPredefined(
            jni_env_pass_through = null,
            value,
            throwForNonOkStatusCallback,
        )

    actual fun createCubicBezier(x1: Float, y1: Float, x2: Float, y2: Float): Long =
        EasingFunctionNative_createCubicBezier(
            jni_env_pass_through = null,
            x1,
            y1,
            x2,
            y2,
            throwForNonOkStatusCallback,
        )

    actual fun createLinear(points: FloatArray): Long {
        check(points.size % 2 == 0) { "points must have an even number of elements" }
        points.usePinned { pinned ->
            return EasingFunctionNative_createLinear(
                jni_env_pass_through = null,
                if (points.isEmpty()) null else pinned.addressOf(0),
                points.size,
                throwForNonOkStatusCallback,
            )
        }
    }

    actual fun createSteps(stepCount: Int, stepPosition: Int): Long =
        EasingFunctionNative_createSteps(
            jni_env_pass_through = null,
            stepCount,
            stepPosition,
            throwForNonOkStatusCallback,
        )

    actual fun free(nativePointer: Long) = EasingFunctionNative_free(nativePointer)

    actual fun getParametersType(nativePointer: Long): Int =
        EasingFunctionNative_getParametersType(nativePointer)

    // Predefined easing function accessors:

    actual fun getPredefinedValueInt(nativePointer: Long): Int =
        EasingFunctionNative_getPredefinedValueInt(nativePointer)

    // Cubic Bezier easing function accessors:

    actual fun getCubicBezierX1(nativePointer: Long): Float =
        EasingFunctionNative_getCubicBezierX1(nativePointer)

    actual fun getCubicBezierY1(nativePointer: Long): Float =
        EasingFunctionNative_getCubicBezierY1(nativePointer)

    actual fun getCubicBezierX2(nativePointer: Long): Float =
        EasingFunctionNative_getCubicBezierX2(nativePointer)

    actual fun getCubicBezierY2(nativePointer: Long): Float =
        EasingFunctionNative_getCubicBezierY2(nativePointer)

    // Linear easing function accessors:

    actual fun getLinearNumPoints(nativePointer: Long): Int =
        EasingFunctionNative_getLinearNumPoints(nativePointer)

    actual fun getLinearPointX(nativePointer: Long, index: Int): Float =
        EasingFunctionNative_getLinearPointX(nativePointer, index)

    actual fun getLinearPointY(nativePointer: Long, index: Int): Float =
        EasingFunctionNative_getLinearPointY(nativePointer, index)

    // Steps easing function accessors:

    actual fun getStepsCount(nativePointer: Long): Int =
        EasingFunctionNative_getStepsCount(nativePointer)

    actual fun getStepsPositionInt(nativePointer: Long): Int =
        EasingFunctionNative_getStepsPositionInt(nativePointer)
}
