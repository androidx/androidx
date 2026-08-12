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

import androidx.ink.nativeloader.InkInternalOnlyApi
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

@UsedByNative
@OptIn(InkInternalOnlyApi::class)
actual internal object AffineTransformNative {

    init {
        NativeLoader.load()
    }

    @UsedByNative
    actual external fun populateTransformedParallelogram(
        affineTransformM00: Float,
        affineTransformM10: Float,
        affineTransformM20: Float,
        affineTransformM01: Float,
        affineTransformM11: Float,
        affineTransformM21: Float,
        parallelogramCenterX: Float,
        parallelogramCenterY: Float,
        parallelogramWidth: Float,
        parallelogramHeight: Float,
        parallelogramRotationDegrees: Float,
        parallelogramShearFactor: Float,
        out: MutableParallelogram,
    )

    @UsedByNative
    actual external fun createTransformedParallelogram(
        affineTransformM00: Float,
        affineTransformM10: Float,
        affineTransformM20: Float,
        affineTransformM01: Float,
        affineTransformM11: Float,
        affineTransformM21: Float,
        parallelogramCenterX: Float,
        parallelogramCenterY: Float,
        parallelogramWidth: Float,
        parallelogramHeight: Float,
        parallelogramRotationDegrees: Float,
        parallelogramShearFactor: Float,
    ): ImmutableParallelogram
}
