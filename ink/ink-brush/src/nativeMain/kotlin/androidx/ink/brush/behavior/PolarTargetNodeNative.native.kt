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

import androidx.ink.nativeloader.cinterop.PolarTargetNodeNative_create
import androidx.ink.nativeloader.cinterop.PolarTargetNodeNative_getAngleRangeEnd
import androidx.ink.nativeloader.cinterop.PolarTargetNodeNative_getAngleRangeStart
import androidx.ink.nativeloader.cinterop.PolarTargetNodeNative_getMagnitudeRangeEnd
import androidx.ink.nativeloader.cinterop.PolarTargetNodeNative_getMagnitudeRangeStart
import androidx.ink.nativeloader.cinterop.PolarTargetNodeNative_getTargetInt
import androidx.ink.nativeloader.throwForNonOkStatusCallback
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual internal object PolarTargetNodeNative {
    actual fun create(
        polarTarget: Int,
        angleRangeStart: Float,
        angleRangeEnd: Float,
        magnitudeRangeStart: Float,
        magnitudeRangeEnd: Float,
    ): Long =
        PolarTargetNodeNative_create(
            jni_env_pass_through = null,
            polarTarget,
            angleRangeStart,
            angleRangeEnd,
            magnitudeRangeStart,
            magnitudeRangeEnd,
            throwForNonOkStatusCallback,
        )

    actual fun getTargetInt(nativePointer: Long): Int =
        PolarTargetNodeNative_getTargetInt(nativePointer)

    actual fun getAngleRangeStart(nativePointer: Long): Float =
        PolarTargetNodeNative_getAngleRangeStart(nativePointer)

    actual fun getAngleRangeEnd(nativePointer: Long): Float =
        PolarTargetNodeNative_getAngleRangeEnd(nativePointer)

    actual fun getMagnitudeRangeStart(nativePointer: Long): Float =
        PolarTargetNodeNative_getMagnitudeRangeStart(nativePointer)

    actual fun getMagnitudeRangeEnd(nativePointer: Long): Float =
        PolarTargetNodeNative_getMagnitudeRangeEnd(nativePointer)
}
