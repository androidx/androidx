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

package androidx.ink.strokes

import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

@UsedByNative
actual internal object StrokeInputBatchNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative actual external fun create(): Long

    @UsedByNative actual external fun free(nativePointer: Long)

    @UsedByNative actual external fun getSize(nativePointer: Long): Int

    @UsedByNative actual external fun getToolType(nativePointer: Long): Int

    @UsedByNative actual external fun getDurationMillis(nativePointer: Long): Long

    @UsedByNative actual external fun getStrokeUnitLengthCm(nativePointer: Long): Float

    @UsedByNative actual external fun hasStrokeUnitLength(nativePointer: Long): Boolean

    @UsedByNative actual external fun hasPressure(nativePointer: Long): Boolean

    @UsedByNative actual external fun hasTilt(nativePointer: Long): Boolean

    @UsedByNative actual external fun hasOrientation(nativePointer: Long): Boolean

    @UsedByNative actual external fun getNoiseSeed(nativePointer: Long): Int

    @UsedByNative actual external fun populate(nativePointer: Long, index: Int, input: StrokeInput)
}

@UsedByNative
actual internal object MutableStrokeInputBatchNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative actual external fun clear(nativePointer: Long)

    @UsedByNative
    actual external fun appendSingle(
        nativePointer: Long,
        type: Int,
        x: Float,
        y: Float,
        elapsedTimeMillis: Long,
        strokeUnitLengthCm: Float,
        pressure: Float,
        tilt: Float,
        orientation: Float,
    ): Boolean

    @UsedByNative
    actual external fun appendBatch(nativePointer: Long, addedNativePointer: Long): Boolean

    @UsedByNative actual external fun newCopy(nativePointer: Long): Long

    @UsedByNative actual external fun setNoiseSeed(nativePointer: Long, seed: Int)
}
