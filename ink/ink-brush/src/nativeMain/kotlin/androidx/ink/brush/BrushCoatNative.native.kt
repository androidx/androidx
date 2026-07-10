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

import androidx.ink.nativeloader.cinterop.BrushCoatNative_create
import androidx.ink.nativeloader.cinterop.BrushCoatNative_free
import androidx.ink.nativeloader.cinterop.BrushCoatNative_getBrushPaintPreferencesCount
import androidx.ink.nativeloader.cinterop.BrushCoatNative_isCompatibleWithMeshFormat
import androidx.ink.nativeloader.cinterop.BrushCoatNative_newCopyOfBrushPaintPreference
import androidx.ink.nativeloader.cinterop.BrushCoatNative_newCopyOfBrushTip
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

@OptIn(ExperimentalForeignApi::class)
actual internal object BrushCoatNative {
    actual fun create(tipNativePointer: Long, paintPreferencesNativePointers: LongArray): Long =
        paintPreferencesNativePointers.usePinned { pinnedPaintPreferences ->
            BrushCoatNative_create(
                tipNativePointer,
                if (paintPreferencesNativePointers.isEmpty()) null
                else pinnedPaintPreferences.addressOf(0),
                paintPreferencesNativePointers.size,
            )
        }

    actual fun free(nativePointer: Long) = BrushCoatNative_free(nativePointer)

    actual fun isCompatibleWithMeshFormat(
        nativePointer: Long,
        meshFormatNativePointer: Long,
    ): Boolean = BrushCoatNative_isCompatibleWithMeshFormat(nativePointer, meshFormatNativePointer)

    actual fun newCopyOfBrushTip(nativePointer: Long): Long =
        BrushCoatNative_newCopyOfBrushTip(nativePointer)

    actual fun getBrushPaintPreferencesCount(nativePointer: Long): Int =
        BrushCoatNative_getBrushPaintPreferencesCount(nativePointer)

    actual fun newCopyOfBrushPaintPreference(nativePointer: Long, index: Int): Long =
        BrushCoatNative_newCopyOfBrushPaintPreference(nativePointer, index)
}
