/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.painting

import androidx.ui.graphics.ColorSpace

// Opaque handle to raw decoded image data (pixels).
/**
 *
 * To obtain an [Image] object, use [instantiateImageCodec].
 *
 * To draw an [Image], use one of the methods on the [Canvas] class, such as
 * [Canvas.drawImage].
 */

/**
 * This class is created by the engine, and should not be instantiated
 * or extended directly.
 *
 * To obtain an [Image] object, use [instantiateImageCodec].
 */

// TODO njawad/aelias uncomment implementation when host side testing support is enabled +
//  uncomment `expect`
// expect fun Image(
//    width: Int,
//    height: Int,
//    config: ImageConfig = ImageConfig.Argb8888,
//    hasAlpha: Boolean = true,
//    colorSpace: ColorSpace = ColorSpace.get(ColorSpace.Named.Srgb)
// ): Image

/* expect */ typealias NativeImage = android.graphics.Bitmap

interface Image {

    /** The number of image pixels along the image's horizontal axis. */
    val width: Int

    /** The number of image pixels along the image's vertical axis. */
    val height: Int

    /** ColorSpace the Image renders in **/
    val colorSpace: ColorSpace

    /** Determines whether or not the Image contains an alpha channel **/
    val hasAlpha: Boolean

    /**
     * Returns the current configuration of this Image, either:
     * @see ImageConfig.Argb8888
     * @see ImageConfig.Rgb565
     * @see ImageConfig.Alpha8
     * @see ImageConfig.Gpu
     */
    val config: ImageConfig

    /**
     * Return backing object that implements the Image interface
     */
    val nativeImage: NativeImage

    /**
     * Builds caches associated with the bitmap that are used for drawing it. This method can
     * be used as a signal to upload images to the GPU to eventually be rendered
     */
    fun prepareToDraw()
}

/**
 * Possible Image configurations. An Image configuration describes
 * how pixels are stored. This affects the quality (color depth) as
 * well as the ability to display transparent/translucent colors.
 */
enum class ImageConfig {
    /**
     * Each pixel is stored on 4 bytes. Each channel (RGB and alpha
     * for translucency) is stored with 8 bits of precision (256
     * possible values.)
     *
     * This configuration is very flexible and offers the best
     * quality. It should be used whenever possible.
     *
     *      Use this formula to pack into 32 bits:
     *
     * ```
     * val color =
     *    ((A and 0xff) shl 24) or
     *    ((B and 0xff) shl 16) or
     *    ((G and 0xff) shl 8) or
     *    (R and 0xff)
     * ```
     */
    Argb8888,

    /**
     * Each pixel is stored as a single translucency (alpha) channel.
     * This is very useful to efficiently store masks for instance.
     * No color information is stored.
     * With this configuration, each pixel requires 1 byte of memory.
     */
    Alpha8,

    /**
     * Each pixel is stored on 2 bytes and only the RGB channels are
     * encoded: red is stored with 5 bits of precision (32 possible
     * values), green is stored with 6 bits of precision (64 possible
     * values) and blue is stored with 5 bits of precision.
     *
     * This configuration can produce slight visual artifacts depending
     * on the configuration of the source. For instance, without
     * dithering, the result might show a greenish tint. To get better
     * results dithering should be applied.
     *
     * This configuration may be useful when using opaque bitmaps
     * that do not require high color fidelity.
     *
     *      Use this formula to pack into 16 bits:
     * ```
     *  val color =
     *      ((R and 0x1f) shl 11) or
     *      ((G and 0x3f) shl 5) or
     *      (B and 0x1f)
     * ```
     */
    Rgb565,

    /**
     * Each pixel is stored on 8 bytes. Each channel (RGB and alpha
     * for translucency) is stored as a
     * half-precision floating point value.
     *
     * This configuration is particularly suited for wide-gamut and
     * HDR content.
     *
     *      Use this formula to pack into 64 bits:
     * ```
     *    val color =
     *      ((A and 0xffff) shl 48) or
     *      ((B and 0xffff) shl 32) or
     *      ((G and 0xffff) shl 16) or
     *      (R and 0xffff)
     * ```
     */
    F16,

    /**
     * Special configuration, when an Image is stored only in graphic memory.
     * Images in this configuration are always immutable.
     *
     * It is optimal for cases, when the only operation with the Image is to draw it on a
     * screen.
     */
    Gpu
}
