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

package androidx.ink.geometry

import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

@UsedByNative
actual internal object BoxNative {

    init {
        NativeLoader.load()
    }

    @UsedByNative
    actual external fun createCenter(
        rectXMin: Float,
        rectYMin: Float,
        rectXMax: Float,
        rectYMax: Float,
    ): ImmutableVec

    @UsedByNative
    actual external fun populateCenter(
        rectXMin: Float,
        rectYMin: Float,
        rectXMax: Float,
        rectYMax: Float,
        out: MutableVec,
    )

    @UsedByNative
    actual external fun containsPoint(
        rectXMin: Float,
        rectYMin: Float,
        rectXMax: Float,
        rectYMax: Float,
        pointX: Float,
        pointY: Float,
    ): Boolean

    @UsedByNative
    actual external fun containsBox(
        rectXMin: Float,
        rectYMin: Float,
        rectXMax: Float,
        rectYMax: Float,
        otherXMin: Float,
        otherYMin: Float,
        otherXMax: Float,
        otherYMax: Float,
    ): Boolean
}
