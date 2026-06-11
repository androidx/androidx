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

import androidx.ink.nativeloader.cinterop.MultipleStrokesNative_createWithPartialErase
import androidx.ink.nativeloader.cinterop.MultipleStrokesNative_free
import androidx.ink.nativeloader.cinterop.MultipleStrokesNative_getStrokeCount
import androidx.ink.nativeloader.cinterop.MultipleStrokesNative_releaseStroke
import androidx.ink.nativeloader.cinterop.StrokeNative_createWithBrushAndInputs
import androidx.ink.nativeloader.cinterop.StrokeNative_createWithBrushInputsAndShape
import androidx.ink.nativeloader.cinterop.StrokeNative_free
import androidx.ink.nativeloader.cinterop.StrokeNative_newShallowCopyOfInputs
import androidx.ink.nativeloader.cinterop.StrokeNative_newShallowCopyOfShape
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
internal actual object StrokeNative {
    actual fun createWithBrushAndInputs(brushNativePointer: Long, inputs: Long): Long =
        StrokeNative_createWithBrushAndInputs(brushNativePointer, inputs)

    actual fun createWithBrushInputsAndShape(
        brushNativePointer: Long,
        inputs: Long,
        shape: Long,
    ): Long = StrokeNative_createWithBrushInputsAndShape(brushNativePointer, inputs, shape)

    actual fun newShallowCopyOfInputs(nativePointer: Long): Long =
        StrokeNative_newShallowCopyOfInputs(nativePointer)

    actual fun newShallowCopyOfShape(nativePointer: Long): Long =
        StrokeNative_newShallowCopyOfShape(nativePointer)

    actual fun free(nativePointer: Long) = StrokeNative_free(nativePointer)
}

@OptIn(ExperimentalForeignApi::class)
internal actual object MultipleStrokesNative {

    actual fun createWithPartialErase(
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
    ): Long =
        MultipleStrokesNative_createWithPartialErase(
            targetStrokePointer,
            eraserShapePointer,
            eraserA,
            eraserB,
            eraserC,
            eraserD,
            eraserE,
            eraserF,
            strokeA,
            strokeB,
            strokeC,
            strokeD,
            strokeE,
            strokeF,
        )

    actual fun getStrokeCount(nativePointer: Long): Int =
        MultipleStrokesNative_getStrokeCount(nativePointer)

    actual fun releaseStroke(nativePointer: Long, index: Int): Long =
        MultipleStrokesNative_releaseStroke(nativePointer, index)

    actual fun free(nativePointer: Long) = MultipleStrokesNative_free(nativePointer)
}
