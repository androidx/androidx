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

import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

@UsedByNative
actual internal object BrushCoatNative {
    init {
        NativeLoader.load()
    }

    /** Create underlying native object and return reference for all subsequent native calls. */
    @UsedByNative
    actual external fun create(
        tipNativePointer: Long,
        paintPreferencesNativePointers: LongArray,
    ): Long

    /** Release the underlying memory allocated in [create]. */
    @UsedByNative actual external fun free(nativePointer: Long)

    @UsedByNative
    actual external fun isCompatibleWithMeshFormat(
        nativePointer: Long,
        meshFormatNativePointer: Long,
    ): Boolean

    /**
     * Returns a new, unowned native pointer to a copy of the `BrushTip` in the pointed-at
     * `BrushCoat`.
     */
    @UsedByNative actual external fun newCopyOfBrushTip(nativePointer: Long): Long

    @UsedByNative actual external fun getBrushPaintPreferencesCount(nativePointer: Long): Int

    /**
     * Returns a new, unowned native pointer to a copy of the `BrushPaint` in the pointed-at
     * `BrushCoat`.
     */
    @UsedByNative
    actual external fun newCopyOfBrushPaintPreference(nativePointer: Long, index: Int): Long
}
