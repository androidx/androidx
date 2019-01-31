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

/**
 * The format in which image bytes should be returned when using
 * [Image.toByteData].
 */
enum class ImageByteFormat {
    /**
     * Raw RGBA format.
     *
     * Unencoded bytes, in RGBA row-primary form, 8 bits per channel.
     */
    rawRgba,

    /**
     * Raw unmodified format.
     *
     * Unencoded bytes, in the image's existing format. For example, a grayscale
     * image may use a single 8-bit channel for each pixel.
     */
    rawUnmodified,

    /**
     * PNG format.
     *
     * A loss-less compression format for images. This format is well suited for
     * images with hard edges, such as screenshots or sprites, and images with
     * text. Transparency is supported. The PNG format supports images up to
     * 2,147,483,647 pixels in either dimension, though in practice available
     * memory provides a more immediate limitation on maximum image size.
     *
     * PNG images normally use the `.png` file extension and the `image/png` MIME
     * type.
     *
     * See also:
     *
     *  * <https://en.wikipedia.org/wiki/Portable_Network_Graphics>, the Wikipedia page on PNG.
     *  * <https://tools.ietf.org/rfc/rfc2083.txt>, the PNG standard.
     */
    png
}