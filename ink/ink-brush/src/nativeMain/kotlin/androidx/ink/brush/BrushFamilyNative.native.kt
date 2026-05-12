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

import androidx.ink.nativeloader.cinterop.BrushFamilyNative_calculateMinimumRequiredVersion
import androidx.ink.nativeloader.cinterop.BrushFamilyNative_create
import androidx.ink.nativeloader.cinterop.BrushFamilyNative_free
import androidx.ink.nativeloader.cinterop.BrushFamilyNative_getBrushCoatCount
import androidx.ink.nativeloader.cinterop.BrushFamilyNative_getClientBrushFamilyId
import androidx.ink.nativeloader.cinterop.BrushFamilyNative_getDeveloperComment
import androidx.ink.nativeloader.cinterop.BrushFamilyNative_getInputModelType
import androidx.ink.nativeloader.cinterop.BrushFamilyNative_hasFallbacks
import androidx.ink.nativeloader.cinterop.BrushFamilyNative_newCopyOfBrushCoat
import androidx.ink.nativeloader.cinterop.BrushFamilyNative_newCopyOfInputModel
import androidx.ink.nativeloader.cinterop.InputModelNative_createNoParametersModel
import androidx.ink.nativeloader.cinterop.InputModelNative_createSlidingWindowModel
import androidx.ink.nativeloader.cinterop.InputModelNative_createSlidingWindowModelWithDefaultParameters
import androidx.ink.nativeloader.cinterop.InputModelNative_free
import androidx.ink.nativeloader.cinterop.InputModelNative_getSlidingUpsamplingFrequencyHz
import androidx.ink.nativeloader.cinterop.InputModelNative_getSlidingWindowDurationMillis
import androidx.ink.nativeloader.throwForNonOkStatusCallback
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned

@OptIn(ExperimentalForeignApi::class)
actual internal object BrushFamilyNative {
    actual fun create(
        coatNativePointers: LongArray,
        inputModelPointer: Long,
        clientBrushFamilyId: String,
        developerComment: String,
    ): Long =
        coatNativePointers.usePinned { pinnedCoats ->
            BrushFamilyNative_create(
                jni_env_pass_through = null,
                if (coatNativePointers.isEmpty()) null else pinnedCoats.addressOf(0),
                coatNativePointers.size,
                inputModelPointer,
                clientBrushFamilyId,
                developerComment,
                throwForNonOkStatusCallback,
            )
        }

    actual fun free(nativePointer: Long) = BrushFamilyNative_free(nativePointer)

    actual fun getBrushCoatCount(nativePointer: Long): Int =
        BrushFamilyNative_getBrushCoatCount(nativePointer).toInt()

    actual fun getClientBrushFamilyId(nativePointer: Long): String =
        BrushFamilyNative_getClientBrushFamilyId(nativePointer)?.toKString() ?: ""

    actual fun getDeveloperComment(nativePointer: Long): String =
        BrushFamilyNative_getDeveloperComment(nativePointer)?.toKString() ?: ""

    actual fun calculateMinimumRequiredVersion(nativePointer: Long): Int =
        BrushFamilyNative_calculateMinimumRequiredVersion(nativePointer)

    actual fun hasFallbacks(nativePointer: Long): Boolean =
        BrushFamilyNative_hasFallbacks(nativePointer)

    actual fun newCopyOfBrushCoat(nativePointer: Long, index: Int): Long =
        BrushFamilyNative_newCopyOfBrushCoat(nativePointer, index)

    actual fun getInputModelType(nativePointer: Long): Int =
        BrushFamilyNative_getInputModelType(nativePointer)

    actual fun newCopyOfInputModel(nativePointer: Long): Long =
        BrushFamilyNative_newCopyOfInputModel(nativePointer)
}

@OptIn(ExperimentalForeignApi::class)
actual internal object InputModelNative {
    actual fun createNoParametersModel(type: Int): Long =
        InputModelNative_createNoParametersModel(type)

    actual fun createSlidingWindowModel(
        windowDurationMillis: Long,
        upsamplingFrequencyHz: Int,
    ): Long = InputModelNative_createSlidingWindowModel(windowDurationMillis, upsamplingFrequencyHz)

    actual fun createSlidingWindowModelWithDefaultParameters(): Long =
        InputModelNative_createSlidingWindowModelWithDefaultParameters()

    actual fun free(nativePointer: Long) = InputModelNative_free(nativePointer)

    actual fun getSlidingWindowDurationMillis(nativePointer: Long): Long =
        InputModelNative_getSlidingWindowDurationMillis(nativePointer)

    actual fun getSlidingUpsamplingFrequencyHz(nativePointer: Long): Int =
        InputModelNative_getSlidingUpsamplingFrequencyHz(nativePointer)
}
