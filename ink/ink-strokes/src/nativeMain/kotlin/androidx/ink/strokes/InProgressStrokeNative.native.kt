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

import androidx.ink.brush.InputToolType
import androidx.ink.geometry.BoxAccumulator
import androidx.ink.geometry.MutableVec
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_changesWithTime
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_clear
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_create
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_enqueueInputs
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_finishInput
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_free
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_getBrushCoatCount
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_getInput
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_getInputCount
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_getMeshBounds
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_getMeshPartitionCount
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_getOutlineCount
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_getOutlinePosition
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_getOutlineVertexCount
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_getPosition
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_getPredictedInputCount
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_getRealInputCount
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_getUpdatedRegion
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_getVertexCount
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_hasMeshBounds
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_hasUpdatedRegion
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_isInputFinished
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_isUpdateNeeded
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_newCopyOfMeshFormat
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_newStrokeFromCopy
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_newStrokeFromPrunedCopy
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_populateInputs
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_resetUpdatedRegion
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_start
import androidx.ink.nativeloader.cinterop.InProgressStrokeNative_updateShape
import androidx.ink.nativeloader.throwForNonOkStatusCallback
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents

@OptIn(ExperimentalForeignApi::class)
internal actual object InProgressStrokeNative {
    actual fun create(): Long = InProgressStrokeNative_create()

    actual fun clear(nativePointer: Long) = InProgressStrokeNative_clear(nativePointer)

    actual fun start(
        nativePointer: Long,
        brushNativePointer: Long,
        noiseSeed: Int,
        baseAnimationPhase: Float,
    ) =
        InProgressStrokeNative_start(
            nativePointer,
            brushNativePointer,
            noiseSeed,
            baseAnimationPhase,
        )

    actual fun enqueueInputs(
        nativePointer: Long,
        realInputsPointer: Long,
        predictedInputsPointer: Long,
    ): Boolean =
        InProgressStrokeNative_enqueueInputs(
            jni_env_pass_through = null,
            nativePointer,
            realInputsPointer,
            predictedInputsPointer,
            throwForNonOkStatusCallback,
        )

    actual fun updateShape(nativePointer: Long, currentElapsedTime: Long): Boolean =
        InProgressStrokeNative_updateShape(
            jni_env_pass_through = null,
            nativePointer,
            currentElapsedTime,
            throwForNonOkStatusCallback,
        )

    actual fun finishInput(nativePointer: Long) = InProgressStrokeNative_finishInput(nativePointer)

    actual fun isInputFinished(nativePointer: Long): Boolean =
        InProgressStrokeNative_isInputFinished(nativePointer)

    actual fun isUpdateNeeded(nativePointer: Long): Boolean =
        InProgressStrokeNative_isUpdateNeeded(nativePointer)

    actual fun changesWithTime(nativePointer: Long): Boolean =
        InProgressStrokeNative_changesWithTime(nativePointer)

    actual fun newStrokeFromCopy(nativePointer: Long): Long =
        InProgressStrokeNative_newStrokeFromCopy(nativePointer)

    actual fun newStrokeFromPrunedCopy(nativePointer: Long): Long =
        InProgressStrokeNative_newStrokeFromPrunedCopy(nativePointer)

    actual fun getInputCount(nativePointer: Long): Int =
        InProgressStrokeNative_getInputCount(nativePointer)

    actual fun getRealInputCount(nativePointer: Long): Int =
        InProgressStrokeNative_getRealInputCount(nativePointer)

    actual fun getPredictedInputCount(nativePointer: Long): Int =
        InProgressStrokeNative_getPredictedInputCount(nativePointer)

    actual fun populateInputs(
        nativePointer: Long,
        mutableStrokeInputBatchPointer: Long,
        from: Int,
        to: Int,
    ) =
        InProgressStrokeNative_populateInputs(
            nativePointer,
            mutableStrokeInputBatchPointer,
            from,
            to,
        )

    actual fun getAndOverwriteInput(nativePointer: Long, input: StrokeInput, index: Int) {
        InProgressStrokeNative_getInput(nativePointer, index).useContents {
            input.update(
                x = x,
                y = y,
                elapsedTimeMillis = elapsed_time_millis,
                toolType = InputToolType.fromInt(tool_type_int),
                strokeUnitLengthCm = stroke_unit_length_cm,
                pressure = pressure,
                tiltRadians = tilt_radians,
                orientationRadians = orientation_radians,
            )
        }
    }

    actual fun getBrushCoatCount(nativePointer: Long): Int =
        InProgressStrokeNative_getBrushCoatCount(nativePointer)

    actual fun fillMeshBounds(nativePointer: Long, coatIndex: Int, outEnvelope: BoxAccumulator) {
        if (InProgressStrokeNative_hasMeshBounds(nativePointer, coatIndex)) {
            InProgressStrokeNative_getMeshBounds(nativePointer, coatIndex).useContents {
                outEnvelope.populateFrom(min_x, min_y, max_x, max_y)
            }
        } else {
            outEnvelope.reset()
        }
    }

    actual fun getMeshPartitionCount(nativePointer: Long, coatIndex: Int): Int =
        InProgressStrokeNative_getMeshPartitionCount(nativePointer, coatIndex)

    actual fun getVertexCount(nativePointer: Long, coatIndex: Int, partitionIndex: Int): Int =
        InProgressStrokeNative_getVertexCount(nativePointer, coatIndex, partitionIndex)

    actual fun newCopyOfMeshFormat(nativePointer: Long, coatIndex: Int): Long =
        InProgressStrokeNative_newCopyOfMeshFormat(nativePointer, coatIndex)

    actual fun fillUpdatedRegion(nativePointer: Long, outEnvelope: BoxAccumulator) {
        if (InProgressStrokeNative_hasUpdatedRegion(nativePointer)) {
            InProgressStrokeNative_getUpdatedRegion(nativePointer).useContents {
                outEnvelope.populateFrom(min_x, min_y, max_x, max_y)
            }
        } else {
            outEnvelope.reset()
        }
    }

    actual fun resetUpdatedRegion(nativePointer: Long) =
        InProgressStrokeNative_resetUpdatedRegion(nativePointer)

    actual fun getOutlineCount(nativePointer: Long, coatIndex: Int): Int =
        InProgressStrokeNative_getOutlineCount(nativePointer, coatIndex)

    actual fun getOutlineVertexCount(nativePointer: Long, coatIndex: Int, outlineIndex: Int): Int =
        InProgressStrokeNative_getOutlineVertexCount(nativePointer, coatIndex, outlineIndex)

    actual fun fillOutlinePosition(
        nativePointer: Long,
        coatIndex: Int,
        outlineIndex: Int,
        outlineVertexIndex: Int,
        outPosition: MutableVec,
    ) {
        InProgressStrokeNative_getOutlinePosition(
                nativePointer,
                coatIndex,
                outlineIndex,
                outlineVertexIndex,
            )
            .useContents {
                outPosition.x = x
                outPosition.y = y
            }
    }

    actual fun fillPosition(
        nativePointer: Long,
        coatIndex: Int,
        partitionIndex: Int,
        vertexIndex: Int,
        outPosition: MutableVec,
    ) {
        InProgressStrokeNative_getPosition(nativePointer, coatIndex, partitionIndex, vertexIndex)
            .useContents {
                outPosition.x = x
                outPosition.y = y
            }
    }

    actual fun free(nativePointer: Long) = InProgressStrokeNative_free(nativePointer)
}
