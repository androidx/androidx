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

package androidx.ink.brush

import androidx.ink.nativeloader.cinterop.BrushTipNative_create
import androidx.ink.nativeloader.cinterop.BrushTipNative_free
import androidx.ink.nativeloader.cinterop.BrushTipNative_getBehaviorCount
import androidx.ink.nativeloader.cinterop.BrushTipNative_getCornerRounding
import androidx.ink.nativeloader.cinterop.BrushTipNative_getParticleGapDistanceScale
import androidx.ink.nativeloader.cinterop.BrushTipNative_getParticleGapDurationMillis
import androidx.ink.nativeloader.cinterop.BrushTipNative_getPinch
import androidx.ink.nativeloader.cinterop.BrushTipNative_getRotationDegrees
import androidx.ink.nativeloader.cinterop.BrushTipNative_getScaleX
import androidx.ink.nativeloader.cinterop.BrushTipNative_getScaleY
import androidx.ink.nativeloader.cinterop.BrushTipNative_getSlantDegrees
import androidx.ink.nativeloader.cinterop.BrushTipNative_newCopyOfBrushBehavior
import androidx.ink.nativeloader.throwForNonOkStatusCallback
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

@OptIn(ExperimentalForeignApi::class)
actual internal object BrushTipNative {
    actual fun create(
        scaleX: Float,
        scaleY: Float,
        cornerRounding: Float,
        slantDegrees: Float,
        pinch: Float,
        rotationDegrees: Float,
        particleGapDistanceScale: Float,
        particleGapDurationMillis: Long,
        behaviorNativePointersArray: LongArray,
    ): Long =
        behaviorNativePointersArray.usePinned { pinned ->
            BrushTipNative_create(
                jni_env_pass_through = null,
                scaleX,
                scaleY,
                cornerRounding,
                slantDegrees,
                pinch,
                rotationDegrees,
                particleGapDistanceScale,
                particleGapDurationMillis,
                if (behaviorNativePointersArray.isEmpty()) null else pinned.addressOf(0),
                behaviorNativePointersArray.size,
                throwForNonOkStatusCallback,
            )
        }

    actual fun free(nativePointer: Long) = BrushTipNative_free(nativePointer)

    actual fun getScaleX(nativePointer: Long): Float = BrushTipNative_getScaleX(nativePointer)

    actual fun getScaleY(nativePointer: Long): Float = BrushTipNative_getScaleY(nativePointer)

    actual fun getCornerRounding(nativePointer: Long): Float =
        BrushTipNative_getCornerRounding(nativePointer)

    actual fun getSlantDegrees(nativePointer: Long): Float =
        BrushTipNative_getSlantDegrees(nativePointer)

    actual fun getPinch(nativePointer: Long): Float = BrushTipNative_getPinch(nativePointer)

    actual fun getRotationDegrees(nativePointer: Long): Float =
        BrushTipNative_getRotationDegrees(nativePointer)

    actual fun getParticleGapDistanceScale(nativePointer: Long): Float =
        BrushTipNative_getParticleGapDistanceScale(nativePointer)

    actual fun getParticleGapDurationMillis(nativePointer: Long): Long =
        BrushTipNative_getParticleGapDurationMillis(nativePointer)

    actual fun getBehaviorCount(nativePointer: Long): Int =
        BrushTipNative_getBehaviorCount(nativePointer)

    actual fun newCopyOfBrushBehavior(nativePointer: Long, index: Int): Long =
        BrushTipNative_newCopyOfBrushBehavior(nativePointer, index)
}
