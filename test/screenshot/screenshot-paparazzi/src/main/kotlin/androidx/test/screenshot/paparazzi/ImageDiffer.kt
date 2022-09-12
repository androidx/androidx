/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.test.screenshot.paparazzi

import androidx.test.screenshot.paparazzi.ImageDiffer.DiffResult.Similar
import java.awt.image.BufferedImage

/**
 *  Functional interface to compare two images and returns a [ImageDiffer.DiffResult] ADT containing
 *  comparison statistics and a difference image, if applicable.
 */
fun interface ImageDiffer {
    /**
     * Compare image [a] to image [b]. Implementations may assume [a] and [b] have the same
     * dimensions.
     */
    fun diff(a: BufferedImage, b: BufferedImage): DiffResult

    /** A name to be used in logs for this differ, defaulting to the class's simple name. */
    val name
        get() = requireNotNull(this::class.simpleName) {
            "Could not determine ImageDiffer.name reflectively. Please override ImageDiffer.name."
        }

    /**
     * Result ADT returned from [diff].
     *
     * A differ may permit a small amount of difference, even for [Similar] results. Similar results
     * must include a [description], even if it's trivial, but may omit the [highlights] image if
     * it would be fully transparent.
     *
     * @property description A human-readable description of how the images differed, such as the
     * count of different pixels or percentage changed. Displayed in test failure messages and in
     * CI.
     *
     * @property highlights An image with a transparent background, highlighting where the compared
     * images differ, typically in shades of magenta. Displayed in CI.
     */
    sealed interface DiffResult {
        val description: String
        val highlights: BufferedImage?

        data class Similar(
            override val description: String,
            override val highlights: BufferedImage? = null
        ) : DiffResult

        data class Different(
            override val description: String,
            override val highlights: BufferedImage
        ) : DiffResult
    }

    /**
     * Pixel perfect image differ requiring images to be identical.
     *
     * The alpha channel is treated as pre-multiplied, meaning RGB channels may differ if the alpha
     * channel is 0 (fully transparent).
     */
    // TODO(b/244752233): Support wide gamut images.
    object PixelPerfect : ImageDiffer {
        override fun diff(a: BufferedImage, b: BufferedImage): DiffResult {
            check(a.width == b.width && a.height == b.height) { "Images are different sizes" }
            val width = a.width
            val height = b.height
            val highlights = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            var count = 0

            for (x in 0 until width) {
                for (y in 0 until height) {
                    val aPixel = a.getRGB(x, y)
                    val bPixel = b.getRGB(x, y)

                    // Compare full ARGB pixels, but allow other channels to differ if alpha is 0
                    if (aPixel == bPixel || (aPixel ushr 24 == 0 && bPixel ushr 24 == 0)) {
                        highlights.setRGB(x, y, TRANSPARENT.toInt())
                    } else {
                        count++
                        highlights.setRGB(x, y, MAGENTA.toInt())
                    }
                }
            }

            val description = "$count of ${width * height} pixels different"
            return if (count > 0) {
                DiffResult.Different(description, highlights)
            } else {
                DiffResult.Similar(description)
            }
        }
    }

    private companion object {
        const val MAGENTA = 0xFF_FF_00_FFu
        const val TRANSPARENT = 0x00_FF_FF_FFu
    }
}