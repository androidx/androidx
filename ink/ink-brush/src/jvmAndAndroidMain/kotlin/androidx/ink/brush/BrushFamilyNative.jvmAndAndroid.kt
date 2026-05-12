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
actual internal object BrushFamilyNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative
    actual external fun create(
        coatNativePointers: LongArray,
        inputModelPointer: Long,
        clientBrushFamilyId: String,
        developerComment: String,
    ): Long

    @UsedByNative actual external fun free(nativePointer: Long)

    @UsedByNative actual external fun getBrushCoatCount(nativePointer: Long): Int

    @UsedByNative actual external fun getClientBrushFamilyId(nativePointer: Long): String

    @UsedByNative actual external fun getDeveloperComment(nativePointer: Long): String

    @UsedByNative actual external fun calculateMinimumRequiredVersion(nativePointer: Long): Int

    @UsedByNative actual external fun hasFallbacks(nativePointer: Long): Boolean

    @UsedByNative actual external fun newCopyOfBrushCoat(nativePointer: Long, index: Int): Long

    @UsedByNative actual external fun getInputModelType(nativePointer: Long): Int

    @UsedByNative actual external fun newCopyOfInputModel(nativePointer: Long): Long
}

@UsedByNative
actual internal object InputModelNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative actual external fun createNoParametersModel(type: Int): Long

    @UsedByNative
    actual external fun createSlidingWindowModel(
        windowDurationMillis: Long,
        upsamplingFrequencyHz: Int,
    ): Long

    @UsedByNative actual external fun createSlidingWindowModelWithDefaultParameters(): Long

    @UsedByNative actual external fun free(nativePointer: Long)

    @UsedByNative actual external fun getSlidingWindowDurationMillis(nativePointer: Long): Long

    @UsedByNative actual external fun getSlidingUpsamplingFrequencyHz(nativePointer: Long): Int
}
