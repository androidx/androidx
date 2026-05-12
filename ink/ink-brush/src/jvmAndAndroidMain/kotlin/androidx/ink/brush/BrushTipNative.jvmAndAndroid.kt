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

import androidx.ink.geometry.AngleDegreesFloat
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

@UsedByNative
actual internal object BrushTipNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative
    actual external fun create(
        scaleX: Float,
        scaleY: Float,
        cornerRounding: Float,
        slantDegrees: Float,
        pinch: Float,
        rotationDegrees: Float,
        particleGapDistanceScale: Float,
        particleGapDurationMillis: Long,
        behaviorNativePointersArray: LongArray,
    ): Long

    @UsedByNative actual external fun free(nativePointer: Long)

    @UsedByNative actual external fun getScaleX(nativePointer: Long): Float

    @UsedByNative actual external fun getScaleY(nativePointer: Long): Float

    @UsedByNative actual external fun getCornerRounding(nativePointer: Long): Float

    @UsedByNative @AngleDegreesFloat actual external fun getSlantDegrees(nativePointer: Long): Float

    @UsedByNative actual external fun getPinch(nativePointer: Long): Float

    @UsedByNative
    @AngleDegreesFloat
    actual external fun getRotationDegrees(nativePointer: Long): Float

    @UsedByNative actual external fun getParticleGapDistanceScale(nativePointer: Long): Float

    @UsedByNative actual external fun getParticleGapDurationMillis(nativePointer: Long): Long

    @UsedByNative actual external fun getBehaviorCount(nativePointer: Long): Int

    @UsedByNative actual external fun newCopyOfBrushBehavior(nativePointer: Long, index: Int): Long
}
