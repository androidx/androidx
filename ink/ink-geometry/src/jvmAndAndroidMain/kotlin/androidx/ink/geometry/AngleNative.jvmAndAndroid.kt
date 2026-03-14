/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.geometry

import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

@UsedByNative
actual internal object AngleNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative actual external fun normalizedDegrees(degrees: Float): Float

    @UsedByNative actual external fun normalizedAboutZeroDegrees(degrees: Float): Float

    @UsedByNative actual external fun normalizedRadians(radians: Float): Float

    @UsedByNative actual external fun normalizedAboutZeroRadians(radians: Float): Float
}
