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

import androidx.ink.nativeloader.cinterop.AffineTransformNative_apply
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents

@OptIn(ExperimentalForeignApi::class)
actual internal object AffineTransformNative {

    actual fun populateTransformedParallelogram(
        affineTransformA: Float,
        affineTransformB: Float,
        affineTransformC: Float,
        affineTransformD: Float,
        affineTransformE: Float,
        affineTransformF: Float,
        parallelogramCenterX: Float,
        parallelogramCenterY: Float,
        parallelogramWidth: Float,
        parallelogramHeight: Float,
        parallelogramRotationDegrees: Float,
        parallelogramShearFactor: Float,
        out: MutableParallelogram,
    ) {
        AffineTransformNative_apply(
                affineTransformA,
                affineTransformB,
                affineTransformC,
                affineTransformD,
                affineTransformE,
                affineTransformF,
                parallelogramCenterX,
                parallelogramCenterY,
                parallelogramWidth,
                parallelogramHeight,
                parallelogramRotationDegrees,
                parallelogramShearFactor,
            )
            .useContents {
                out.setCenterDimensionsRotationInDegreesAndSkew(
                    center.x,
                    center.y,
                    width,
                    height,
                    rotation_degrees,
                    skew,
                )
            }
    }

    actual fun createTransformedParallelogram(
        affineTransformA: Float,
        affineTransformB: Float,
        affineTransformC: Float,
        affineTransformD: Float,
        affineTransformE: Float,
        affineTransformF: Float,
        parallelogramCenterX: Float,
        parallelogramCenterY: Float,
        parallelogramWidth: Float,
        parallelogramHeight: Float,
        parallelogramRotationDegrees: Float,
        parallelogramShearFactor: Float,
    ): ImmutableParallelogram {
        return AffineTransformNative_apply(
                affineTransformA,
                affineTransformB,
                affineTransformC,
                affineTransformD,
                affineTransformE,
                affineTransformF,
                parallelogramCenterX,
                parallelogramCenterY,
                parallelogramWidth,
                parallelogramHeight,
                parallelogramRotationDegrees,
                parallelogramShearFactor,
            )
            .useContents {
                ImmutableParallelogram.fromCenterDimensionsRotationInDegreesAndSkew(
                    ImmutableVec(center.x, center.y),
                    width,
                    height,
                    rotation_degrees,
                    skew,
                )
            }
    }
}
