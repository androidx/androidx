/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.annotation.RestrictTo
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.RenderParams

/**
 * Calculates a transformation matrix for scaling and translating content.
 *
 * This matrix scales the content to fit the given `scaledPageWidth` and `scaledPageHeight` while
 * maintaining aspect ratio. It then translates the content to offset it by the specified `left` and
 * `top` values.
 *
 * @param left The left offset in pixels.
 * @param top The top offset in pixels.
 * @param scaledPageWidth The scaled width of the page in pixels.
 * @param scaledPageHeight The scaled height of the page in pixels.
 * @param pageWidth The original width of the page in pixels.
 * @param pageHeight The original height of the page in pixels.
 * @return The calculated transformation matrix.
 * @throws IllegalArgumentException If `pageWidth` or `pageHeight` is less than or equal to 0.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun getTransformationMatrix(
    left: Int,
    top: Int,
    scaledPageWidth: Float,
    scaledPageHeight: Float,
    pageWidth: Int,
    pageHeight: Int,
): Matrix {
    require(pageWidth > 0) { "Page width must be greater than 0" }
    require(pageHeight > 0) { "Page height must be greater than 0" }
    return Matrix().apply {
        setScale(scaledPageWidth / pageWidth, scaledPageHeight / pageHeight)
        postTranslate(-left.toFloat(), -top.toFloat())
    }
}

/**
 * Returns the [Bitmap.Config] specified by [RenderParams.bitmapConfig] if supported by the platform
 * apis, falling back to [Bitmap.Config.ARGB_8888].
 */
@OptIn(ExperimentalPdfApi::class)
internal fun RenderParams.toBitmapConfig(): Bitmap.Config =
    if (isRgb565RenderingAvailable() && bitmapConfig == RenderParams.BITMAP_CONFIG_RGB_565) {
        Bitmap.Config.RGB_565
    } else {
        Bitmap.Config.ARGB_8888
    }
