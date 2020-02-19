/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.foundation

import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Draw
import androidx.ui.geometry.Rect
import androidx.ui.graphics.BlendMode
import androidx.ui.graphics.Color
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.FilterQuality
import androidx.ui.graphics.Image
import androidx.ui.graphics.Paint
import androidx.ui.layout.Container

// TODO(Andrey) Temporary. Should be replaced with our proper Image component when it available
@Composable
fun SimpleImage(
    image: Image,
    tint: Color? = null
) {
    with(DensityAmbient.current) {
        Container(width = image.width.toDp(), height = image.height.toDp()) {
            DrawImage(image, tint)
        }
    }
}

/**
 * Fits an image into the parent container while maintaining the image aspect ratio.
 * The image will be clipped if the aspect ratios of the image and the parent don't match.
 *
 * This component has the same behavior as ImageView.ScaleType.CENTER_CROP currently.
 *
 * @param image The image to draw.
 * @param tint The tint color to apply for the image.
 */
// TODO(Andrey, Matvei, Nader): Support other scale types b/141741141
@Composable
fun DrawImage(image: Image, tint: Color? = null) {
    val paint = remember { Paint().apply {
        filterQuality = FilterQuality.low // we only support low currently
    } }
    paint.colorFilter = tint?.let { ColorFilter(it, BlendMode.srcIn) }
    Draw { canvas, parentSize ->
        val inputWidth = image.width.toFloat()
        val inputHeight = image.height.toFloat()
        val inputAspectRatio = inputWidth / inputHeight

        val outputWidth = parentSize.width.value
        val outputHeight = parentSize.height.value
        val outputAspectRatio = outputWidth / outputHeight

        val fittedWidth = if (outputAspectRatio > inputAspectRatio) {
            inputWidth
        } else {
            inputHeight * outputAspectRatio
        }
        val fittedHeight = if (outputAspectRatio > inputAspectRatio) {
            inputWidth / outputAspectRatio
        } else {
            inputHeight
        }

        val srcRect = Rect(
            left = (inputWidth - fittedWidth) / 2,
            top = (inputHeight - fittedHeight) / 2,
            right = (inputWidth + fittedWidth) / 2,
            bottom = (inputHeight + fittedHeight) / 2
        )

        val dstRect = Rect(
            left = 0f,
            top = 0f,
            right = outputWidth,
            bottom = outputHeight
        )
        canvas.drawImageRect(image, srcRect, dstRect, paint)
    }
}
