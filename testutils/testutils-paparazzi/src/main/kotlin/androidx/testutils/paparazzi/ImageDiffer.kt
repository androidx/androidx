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

package androidx.testutils.paparazzi

import androidx.testutils.paparazzi.ImageDiffer.DiffResult.Similar
import java.awt.image.BufferedImage
import kotlin.math.pow

@JvmDefaultWithCompatibility
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

    /**
     * Image comparison using Structural Similarity Index, developed by Wang, Bovik, Sheikh, and
     * Simoncelli. Details can be read in their paper:
     * https://ece.uwaterloo.ca/~z70wang/publications/ssim.pdf
     */
    object MSSIMMatcher : ImageDiffer {
        override fun diff(a: BufferedImage, b: BufferedImage): DiffResult {
            val aIntArray = a.toIntArray()
            val bIntArray = b.toIntArray()
            val SSIMTotal = calculateSSIM(aIntArray, bIntArray, a.width, a.height)

            val stats = "[MSSIM] Required SSIM: $SSIM_THRESHOLD, Actual " +
                "SSIM: " + "%.3f".format(SSIMTotal)
            if (SSIMTotal >= SSIM_THRESHOLD) {
                return DiffResult.Similar(stats)
            }
            return PixelPerfect.diff(a, b)
        }

        internal fun calculateSSIM(
            ideal: IntArray,
            given: IntArray,
            width: Int,
            height: Int
        ): Double {
            return calculateSSIM(ideal, given, 0, width, width, height)
        }

        private fun calculateSSIM(
            ideal: IntArray,
            given: IntArray,
            offset: Int,
            stride: Int,
            width: Int,
            height: Int
        ): Double {
            var SSIMTotal = 0.0
            var windows = 0
            var currentWindowY = 0
            while (currentWindowY < height) {
                val windowHeight = computeWindowSize(currentWindowY, height)
                var currentWindowX = 0
                while (currentWindowX < width) {
                    val windowWidth = computeWindowSize(currentWindowX, width)
                    val start: Int =
                        indexFromXAndY(currentWindowX, currentWindowY, stride, offset)
                    if (isWindowWhite(ideal, start, stride, windowWidth, windowHeight) &&
                        isWindowWhite(given, start, stride, windowWidth, windowHeight)
                    ) {
                        currentWindowX += WINDOW_SIZE
                        continue
                    }
                    windows++
                    val means =
                        getMeans(ideal, given, start, stride, windowWidth, windowHeight)
                    val meanX = means[0]
                    val meanY = means[1]
                    val variances = getVariances(
                        ideal, given, meanX, meanY, start, stride,
                        windowWidth, windowHeight
                    )
                    val varX = variances[0]
                    val varY = variances[1]
                    val stdBoth = variances[2]
                    val SSIM = SSIM(meanX, meanY, varX, varY, stdBoth)
                    SSIMTotal += SSIM
                    currentWindowX += WINDOW_SIZE
                }
                currentWindowY += WINDOW_SIZE
            }
            if (windows == 0) {
                return 1.0
            }
            return SSIMTotal / windows.toDouble()
        }

        /**
         * Compute the size of the window. The window defaults to WINDOW_SIZE, but
         * must be contained within dimension.
         */
        private fun computeWindowSize(coordinateStart: Int, dimension: Int): Int {
            return if (coordinateStart + WINDOW_SIZE <= dimension) {
                WINDOW_SIZE
            } else {
                dimension - coordinateStart
            }
        }

        private fun isWindowWhite(
            colors: IntArray,
            start: Int,
            stride: Int,
            windowWidth: Int,
            windowHeight: Int
        ): Boolean {
            for (y in 0 until windowHeight) {
                for (x in 0 until windowWidth) {
                    if (colors[indexFromXAndY(x, y, stride, start)] != WHITE) {
                        return false
                    }
                }
            }
            return true
        }

        /**
         * This calculates the position in an array that would represent a bitmap given the parameters.
         */
        private fun indexFromXAndY(x: Int, y: Int, stride: Int, offset: Int): Int {
            return x + y * stride + offset
        }

        private fun SSIM(
            muX: Double,
            muY: Double,
            sigX: Double,
            sigY: Double,
            sigXY: Double
        ): Double {
            var SSIM = (2 * muX * muY + CONSTANT_C1) * (2 * sigXY + CONSTANT_C2)
            val denom = ((muX * muX + muY * muY + CONSTANT_C1) * (sigX + sigY + CONSTANT_C2))
            SSIM /= denom
            return SSIM
        }

        /**
         * This method will find the mean of a window in both sets of pixels. The return is an array
         * where the first double is the mean of the first set and the second double is the mean of the
         * second set.
         */
        private fun getMeans(
            pixels0: IntArray,
            pixels1: IntArray,
            start: Int,
            stride: Int,
            windowWidth: Int,
            windowHeight: Int
        ): DoubleArray {
            var avg0 = 0.0
            var avg1 = 0.0
            for (y in 0 until windowHeight) {
                for (x in 0 until windowWidth) {
                    val index: Int = indexFromXAndY(x, y, stride, start)
                    avg0 += getIntensity(pixels0[index])
                    avg1 += getIntensity(pixels1[index])
                }
            }
            avg0 /= windowWidth * windowHeight.toDouble()
            avg1 /= windowWidth * windowHeight.toDouble()
            return doubleArrayOf(avg0, avg1)
        }

        /**
         * Finds the variance of the two sets of pixels, as well as the covariance of the windows. The
         * return value is an array of doubles, the first is the variance of the first set of pixels,
         * the second is the variance of the second set of pixels, and the third is the covariance.
         */
        private fun getVariances(
            pixels0: IntArray,
            pixels1: IntArray,
            mean0: Double,
            mean1: Double,
            start: Int,
            stride: Int,
            windowWidth: Int,
            windowHeight: Int
        ): DoubleArray {
            if (windowHeight == 1 && windowWidth == 1) {
                // There is only one item. The variance of a single item would be 0.
                // Since Bessel's correction is used below, it will return NaN instead of 0.
                return doubleArrayOf(0.0, 0.0, 0.0)
            }

            var var0 = 0.0
            var var1 = 0.0
            var varBoth = 0.0
            for (y in 0 until windowHeight) {
                for (x in 0 until windowWidth) {
                    val index: Int = indexFromXAndY(x, y, stride, start)
                    val v0 = getIntensity(pixels0[index]) - mean0
                    val v1 = getIntensity(pixels1[index]) - mean1
                    var0 += v0 * v0
                    var1 += v1 * v1
                    varBoth += v0 * v1
                }
            }
            // Using Bessel's correction. Hence, subtracting one.
            val denominatorWithBesselsCorrection = windowWidth * windowHeight - 1.0
            var0 /= denominatorWithBesselsCorrection
            var1 /= denominatorWithBesselsCorrection
            varBoth /= denominatorWithBesselsCorrection
            return doubleArrayOf(var0, var1, varBoth)
        }

        /**
         * Gets the intensity of a given pixel in RGB using luminosity formula
         *
         * l = 0.21R' + 0.72G' + 0.07B'
         *
         * The prime symbols dictate a gamma correction of 1.
         */
        private fun getIntensity(pixel: Int): Double {
            val gamma = 1.0
            var l = 0.0
            l += 0.21f * (red(pixel) / 255f.toDouble()).pow(gamma)
            l += 0.72f * (green(pixel) / 255f.toDouble()).pow(gamma)
            l += 0.07f * (blue(pixel) / 255f.toDouble()).pow(gamma)
            return l
        }

        /**
         * Return the red component of a color int. This is the same as saying
         * (color >> 16) & 0xFF
         */
        fun red(color: Int): Int {
            return color shr 16 and 0xFF
        }

        /**
         * Return the green component of a color int. This is the same as saying
         * (color >> 8) & 0xFF
         */
        fun green(color: Int): Int {
            return color shr 8 and 0xFF
        }

        /**
         * Return the blue component of a color int. This is the same as saying
         * color & 0xFF
         */
        fun blue(color: Int): Int {
            return color and 0xFF
        }

        private fun BufferedImage.toIntArray(): IntArray {
            val bitmapArray = IntArray(width * height)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmapArray[y * width + x] = getRGB(x, y)
                }
            }
            return bitmapArray
        }
    }

    private companion object {
        const val MAGENTA = 0xFF_FF_00_FFu
        const val TRANSPARENT = 0x00_FF_FF_FFu
        const val WHITE = 0xFFFFFF

        // These values were taken from the publication
        private const val CONSTANT_L = 254.0
        private const val CONSTANT_K1 = 0.00001
        private const val CONSTANT_K2 = 0.00003
        private val CONSTANT_C1 = (CONSTANT_L * CONSTANT_K1).pow(2.0)
        private val CONSTANT_C2 = (CONSTANT_L * CONSTANT_K2).pow(2.0)
        private const val WINDOW_SIZE = 10

        private val SSIM_THRESHOLD: Double = 0.98
    }
}
