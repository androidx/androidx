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

import androidx.ink.nativeloader.cinterop.BrushNative_computeComposeColorLong
import androidx.ink.nativeloader.cinterop.BrushNative_create
import androidx.ink.nativeloader.cinterop.BrushNative_free
import androidx.ink.nativeloader.cinterop.BrushNative_getEpsilon
import androidx.ink.nativeloader.cinterop.BrushNative_getSize
import androidx.ink.nativeloader.cinterop.BrushNative_newCopyOfBrushFamily
import androidx.ink.nativeloader.throwForNonOkStatusCallback
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual internal object BrushNative {
    actual fun create(
        familyNativePointer: Long,
        colorRed: Float,
        colorGreen: Float,
        colorBlue: Float,
        colorAlpha: Float,
        colorSpace: Int,
        size: Float,
        epsilon: Float,
    ): Long =
        BrushNative_create(
            jni_env_pass_through = null,
            familyNativePointer,
            colorRed,
            colorGreen,
            colorBlue,
            colorAlpha,
            colorSpace,
            size,
            epsilon,
            throwForNonOkStatusCallback,
        )

    actual fun free(nativePointer: Long) = BrushNative_free(nativePointer)

    actual fun computeComposeColorLong(nativePointer: Long): Long =
        BrushNative_computeComposeColorLong(
            jni_env_pass_through = null,
            nativePointer,
            composeColorLongFromComponentsCallback,
        )

    actual fun getSize(nativePointer: Long): Float = BrushNative_getSize(nativePointer)

    actual fun getEpsilon(nativePointer: Long): Float = BrushNative_getEpsilon(nativePointer)

    actual fun newCopyOfBrushFamily(nativePointer: Long): Long =
        BrushNative_newCopyOfBrushFamily(nativePointer)
}
