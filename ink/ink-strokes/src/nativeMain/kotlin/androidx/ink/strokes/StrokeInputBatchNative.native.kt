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
import androidx.ink.nativeloader.cinterop.MutableStrokeInputBatchNative_appendBatch
import androidx.ink.nativeloader.cinterop.MutableStrokeInputBatchNative_appendSingle
import androidx.ink.nativeloader.cinterop.MutableStrokeInputBatchNative_clear
import androidx.ink.nativeloader.cinterop.MutableStrokeInputBatchNative_newCopy
import androidx.ink.nativeloader.cinterop.MutableStrokeInputBatchNative_setNoiseSeed
import androidx.ink.nativeloader.cinterop.StrokeInputBatchNative_create
import androidx.ink.nativeloader.cinterop.StrokeInputBatchNative_free
import androidx.ink.nativeloader.cinterop.StrokeInputBatchNative_getDurationMillis
import androidx.ink.nativeloader.cinterop.StrokeInputBatchNative_getNoiseSeed
import androidx.ink.nativeloader.cinterop.StrokeInputBatchNative_getSize
import androidx.ink.nativeloader.cinterop.StrokeInputBatchNative_getStrokeInput
import androidx.ink.nativeloader.cinterop.StrokeInputBatchNative_getStrokeUnitLengthCm
import androidx.ink.nativeloader.cinterop.StrokeInputBatchNative_getToolType
import androidx.ink.nativeloader.cinterop.StrokeInputBatchNative_hasOrientation
import androidx.ink.nativeloader.cinterop.StrokeInputBatchNative_hasPressure
import androidx.ink.nativeloader.cinterop.StrokeInputBatchNative_hasStrokeUnitLength
import androidx.ink.nativeloader.cinterop.StrokeInputBatchNative_hasTilt
import androidx.ink.nativeloader.throwForNonOkStatusCallback
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents

@OptIn(ExperimentalForeignApi::class)
actual internal object StrokeInputBatchNative {
    actual fun create(): Long = StrokeInputBatchNative_create()

    actual fun free(nativePointer: Long) {
        StrokeInputBatchNative_free(nativePointer)
    }

    actual fun getSize(nativePointer: Long): Int = StrokeInputBatchNative_getSize(nativePointer)

    actual fun getToolType(nativePointer: Long): Int =
        StrokeInputBatchNative_getToolType(nativePointer)

    actual fun getDurationMillis(nativePointer: Long): Long =
        StrokeInputBatchNative_getDurationMillis(nativePointer)

    actual fun getStrokeUnitLengthCm(nativePointer: Long): Float =
        StrokeInputBatchNative_getStrokeUnitLengthCm(nativePointer)

    actual fun hasStrokeUnitLength(nativePointer: Long): Boolean =
        StrokeInputBatchNative_hasStrokeUnitLength(nativePointer)

    actual fun hasPressure(nativePointer: Long): Boolean =
        StrokeInputBatchNative_hasPressure(nativePointer)

    actual fun hasTilt(nativePointer: Long): Boolean = StrokeInputBatchNative_hasTilt(nativePointer)

    actual fun hasOrientation(nativePointer: Long): Boolean =
        StrokeInputBatchNative_hasOrientation(nativePointer)

    actual fun getNoiseSeed(nativePointer: Long): Int =
        StrokeInputBatchNative_getNoiseSeed(nativePointer)

    actual fun populate(nativePointer: Long, index: Int, input: StrokeInput) {
        StrokeInputBatchNative_getStrokeInput(nativePointer, index).useContents {
            input.update(
                x = x,
                y = y,
                elapsedTimeMillis = elapsed_time_millis,
                toolType = InputToolType.fromInt(tool_type),
                strokeUnitLengthCm = stroke_unit_length_cm,
                pressure = pressure,
                tiltRadians = tilt_radians,
                orientationRadians = orientation_radians,
            )
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual internal object MutableStrokeInputBatchNative {
    actual fun clear(nativePointer: Long) {
        MutableStrokeInputBatchNative_clear(nativePointer)
    }

    actual fun appendSingle(
        nativePointer: Long,
        type: Int,
        x: Float,
        y: Float,
        elapsedTimeMillis: Long,
        strokeUnitLengthCm: Float,
        pressure: Float,
        tilt: Float,
        orientation: Float,
    ): Boolean =
        MutableStrokeInputBatchNative_appendSingle(
            jni_env_pass_through = null,
            nativePointer,
            type,
            x,
            y,
            elapsedTimeMillis,
            strokeUnitLengthCm,
            pressure,
            tilt,
            orientation,
            throwForNonOkStatusCallback,
        )

    actual fun appendBatch(nativePointer: Long, addedNativePointer: Long): Boolean =
        MutableStrokeInputBatchNative_appendBatch(
            jni_env_pass_through = null,
            nativePointer,
            addedNativePointer,
            throwForNonOkStatusCallback,
        )

    actual fun newCopy(nativePointer: Long): Long =
        MutableStrokeInputBatchNative_newCopy(nativePointer)

    actual fun setNoiseSeed(nativePointer: Long, seed: Int) {
        MutableStrokeInputBatchNative_setNoiseSeed(nativePointer, seed)
    }
}
