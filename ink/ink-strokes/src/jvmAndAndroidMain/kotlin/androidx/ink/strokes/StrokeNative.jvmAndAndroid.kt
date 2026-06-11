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
internal actual object StrokeNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative
    actual external fun createWithBrushAndInputs(brushNativePointer: Long, inputs: Long): Long

    @UsedByNative
    actual external fun createWithBrushInputsAndShape(
        brushNativePointer: Long,
        inputs: Long,
        shape: Long,
    ): Long

    @UsedByNative actual external fun newShallowCopyOfInputs(nativePointer: Long): Long

    @UsedByNative actual external fun newShallowCopyOfShape(nativePointer: Long): Long

    @UsedByNative actual external fun free(nativePointer: Long)
}

@UsedByNative
internal actual object MultipleStrokesNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative
    actual external fun createWithPartialErase(
        targetStrokePointer: Long,
        eraserShapePointer: Long,
        eraserA: Float,
        eraserB: Float,
        eraserC: Float,
        eraserD: Float,
        eraserE: Float,
        eraserF: Float,
        strokeA: Float,
        strokeB: Float,
        strokeC: Float,
        strokeD: Float,
        strokeE: Float,
        strokeF: Float,
    ): Long

    @UsedByNative actual external fun getStrokeCount(nativePointer: Long): Int

    @UsedByNative actual external fun releaseStroke(nativePointer: Long, index: Int): Long

    @UsedByNative actual external fun free(nativePointer: Long)
}
