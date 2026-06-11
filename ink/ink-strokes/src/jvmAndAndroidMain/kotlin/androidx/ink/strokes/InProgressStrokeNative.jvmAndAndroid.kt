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

import androidx.ink.geometry.BoxAccumulator
import androidx.ink.geometry.MutableVec
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

@UsedByNative
internal actual object InProgressStrokeNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative actual external fun create(): Long

    @UsedByNative actual external fun clear(nativePointer: Long)

    @UsedByNative
    actual external fun start(
        nativePointer: Long,
        brushNativePointer: Long,
        noiseSeed: Int,
        baseAnimationPhase: Float,
    )

    @UsedByNative
    actual external fun enqueueInputs(
        nativePointer: Long,
        realInputsPointer: Long,
        predictedInputsPointer: Long,
    ): Boolean

    @UsedByNative
    actual external fun updateShape(nativePointer: Long, currentElapsedTime: Long): Boolean

    @UsedByNative actual external fun finishInput(nativePointer: Long)

    @UsedByNative actual external fun isInputFinished(nativePointer: Long): Boolean

    @UsedByNative actual external fun isUpdateNeeded(nativePointer: Long): Boolean

    @UsedByNative actual external fun changesWithTime(nativePointer: Long): Boolean

    @UsedByNative actual external fun newStrokeFromCopy(nativePointer: Long): Long

    @UsedByNative actual external fun newStrokeFromPrunedCopy(nativePointer: Long): Long

    @UsedByNative actual external fun getInputCount(nativePointer: Long): Int

    @UsedByNative actual external fun getRealInputCount(nativePointer: Long): Int

    @UsedByNative actual external fun getPredictedInputCount(nativePointer: Long): Int

    @UsedByNative
    actual external fun populateInputs(
        nativePointer: Long,
        mutableStrokeInputBatchPointer: Long,
        from: Int,
        to: Int,
    )

    @UsedByNative
    actual external fun getAndOverwriteInput(nativePointer: Long, input: StrokeInput, index: Int)

    @UsedByNative actual external fun getBrushCoatCount(nativePointer: Long): Int

    @UsedByNative
    actual external fun fillMeshBounds(
        nativePointer: Long,
        coatIndex: Int,
        outEnvelope: BoxAccumulator,
    )

    @UsedByNative
    actual external fun getMeshPartitionCount(nativePointer: Long, coatIndex: Int): Int

    @UsedByNative
    actual external fun getVertexCount(
        nativePointer: Long,
        coatIndex: Int,
        partitionIndex: Int,
    ): Int

    @UsedByNative actual external fun newCopyOfMeshFormat(nativePointer: Long, coatIndex: Int): Long

    @UsedByNative
    actual external fun fillUpdatedRegion(nativePointer: Long, outEnvelope: BoxAccumulator)

    @UsedByNative actual external fun resetUpdatedRegion(nativePointer: Long)

    @UsedByNative actual external fun getOutlineCount(nativePointer: Long, coatIndex: Int): Int

    @UsedByNative
    actual external fun getOutlineVertexCount(
        nativePointer: Long,
        coatIndex: Int,
        outlineIndex: Int,
    ): Int

    @UsedByNative
    actual external fun fillOutlinePosition(
        nativePointer: Long,
        coatIndex: Int,
        outlineIndex: Int,
        outlineVertexIndex: Int,
        outPosition: MutableVec,
    )

    @UsedByNative
    actual external fun fillPosition(
        nativePointer: Long,
        coatIndex: Int,
        partitionIndex: Int,
        vertexIndex: Int,
        outPosition: MutableVec,
    )

    @UsedByNative actual external fun free(nativePointer: Long)
}
