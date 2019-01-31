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
 * Algorithms to use when painting on the canvas.
 *
 * When drawing a shape or image onto a canvas, different algorithms can be
 * used to blend the pixels. The different values of [BlendMode] specify
 * different such algorithms.
 *
 * Each algorithm has two inputs, the _source_, which is the image being drawn,
 * and the _destination_, which is the image into which the source image is
 * being composited. The destination is often thought of as the _background_.
 * The source and destination both have four color channels, the red, green,
 * blue, and alpha channels. These are typically represented as numbers in the
 * range 0.0 to 1.0. The output of the algorithm also has these same four
 * channels, with values computed from the source and destination.
 *
 * The documentation of each value below describes how the algorithm works. In
 * each case, an image shows the output of blending a source image with a
 * destination image. In the images below, the destination is represented by an
 * image with horizontal lines and an opaque landscape photograph, and the
 * source is represented by an image with vertical lines (the same lines but
 * rotated) and a bird clip-art image. The [src] mode shows only the source
 * image, and the [dst] mode shows only the destination image. In the
 * documentation below, the transparency is illustrated by a checkerboard
 * pattern. The [clear] mode drops both the source and destination, resulting
 * in an output that is entirely transparent (illustrated by a solid
 * checkerboard pattern).
 *
 * The horizontal and vertical bars in these images show the red, green, and
 * blue channels with varying opacity levels, then all three color channels
 * together with those same varying opacity levels, then all three color
 * channels set to zero with those varying opacity levels, then two bars showing
 * a red/green/blue repeating gradient, the first with full opacity and the
 * second with partial opacity, and finally a bar with the three color channels
 * set to zero but the opacity varying in a repeating gradient.
 *
 * ## Application to the [Canvas] API
 *
 * When using [Canvas.saveLayer] and [Canvas.restore], the blend mode of the
 * [Paint] given to the [Canvas.saveLayer] will be applied when
 * [Canvas.restore] is called. Each call to [Canvas.saveLayer] introduces a new
 * layer onto which shapes and images are painted; when [Canvas.restore] is
 * called, that layer is then composited onto the parent layer, with the source
 * being the most-recently-drawn shapes and images, and the destination being
 * the parent layer. (For the first [Canvas.saveLayer] call, the parent layer
 * is the canvas itself.)
 *
 * See also:
 *
 *  * [Paint.blendMode], which uses [BlendMode] to define the compositing
 *    strategy.
 */
enum class BlendMode(private val porterDuffMode: android.graphics.PorterDuff.Mode?) {

    // This list comes from Skia's SkXfermode.h and the values (order) should be
    // kept in sync.
    // See: https://skia.org/user/api/skpaint#SkXfermode

    /**
     * Drop both the source and destination images, leaving nothing.
     *
     * This corresponds to the "clear" Porter-Duff operator.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_clear.png)
     */
    clear(android.graphics.PorterDuff.Mode.CLEAR),

    /**
     * Drop the destination image, only paint the source image.
     *
     * Conceptually, the destination is first cleared, then the source image is
     * painted.
     *
     * This corresponds to the "Copy" Porter-Duff operator.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_src.png)
     */
    src(android.graphics.PorterDuff.Mode.SRC),

    /**
     * Drop the source image, only paint the destination image.
     *
     * Conceptually, the source image is discarded, leaving the destination
     * untouched.
     *
     * This corresponds to the "Destination" Porter-Duff operator.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_dst.png)
     */
    dst(android.graphics.PorterDuff.Mode.DST),

    /**
     * Composite the source image over the destination image.
     *
     * This is the default value. It represents the most intuitive case, where
     * shapes are painted on top of what is below, with transparent areas showing
     * the destination layer.
     *
     * This corresponds to the "Source over Destination" Porter-Duff operator,
     * also known as the Painter's Algorithm.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_srcOver.png)
     */
    srcOver(android.graphics.PorterDuff.Mode.SRC_OVER),

    /**
     * Composite the source image under the destination image.
     *
     * This is the opposite of [srcOver].
     *
     * This corresponds to the "Destination over Source" Porter-Duff operator.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_dstOver.png)
     *
     * This is useful when the source image should have been painted before the
     * destination image, but could not be.
     */
    dstOver(android.graphics.PorterDuff.Mode.DST_OVER),

    /**
     * Show the source image, but only where the two images overlap. The
     * destination image is not rendered, it is treated merely as a mask. The
     * color channels of the destination are ignored, only the opacity has an
     * effect.
     *
     * To show the destination image instead, consider [dstIn].
     *
     * To reverse the semantic of the mask (only showing the source where the
     * destination is absent, rather than where it is present), consider
     * [srcOut].
     *
     * This corresponds to the "Source in Destination" Porter-Duff operator.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_srcIn.png)
     */
    srcIn(android.graphics.PorterDuff.Mode.SRC_IN),

    /**
     * Show the destination image, but only where the two images overlap. The
     * source image is not rendered, it is treated merely as a mask. The color
     * channels of the source are ignored, only the opacity has an effect.
     *
     * To show the source image instead, consider [srcIn].
     *
     * To reverse the semantic of the mask (only showing the source where the
     * destination is present, rather than where it is absent), consider [dstOut].
     *
     * This corresponds to the "Destination in Source" Porter-Duff operator.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_dstIn.png)
     */
    dstIn(android.graphics.PorterDuff.Mode.DST_IN),

    /**
     * Show the source image, but only where the two images do not overlap. The
     * destination image is not rendered, it is treated merely as a mask. The color
     * channels of the destination are ignored, only the opacity has an effect.
     *
     * To show the destination image instead, consider [dstOut].
     *
     * To reverse the semantic of the mask (only showing the source where the
     * destination is present, rather than where it is absent), consider [srcIn].
     *
     * This corresponds to the "Source out Destination" Porter-Duff operator.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_srcOut.png)
     */
    srcOut(android.graphics.PorterDuff.Mode.SRC_OUT),

    /**
     * Show the destination image, but only where the two images do not overlap. The
     * source image is not rendered, it is treated merely as a mask. The color
     * channels of the source are ignored, only the opacity has an effect.
     *
     * To show the source image instead, consider [srcOut].
     *
     * To reverse the semantic of the mask (only showing the destination where the
     * source is present, rather than where it is absent), consider [dstIn].
     *
     * This corresponds to the "Destination out Source" Porter-Duff operator.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_dstOut.png)
     */
    dstOut(android.graphics.PorterDuff.Mode.DST_OUT),

    /**
     * Composite the source image over the destination image, but only where it
     * overlaps the destination.
     *
     * This corresponds to the "Source atop Destination" Porter-Duff operator.
     *
     * This is essentially the [srcOver] operator, but with the output's opacity
     * channel being set to that of the destination image instead of being a
     * combination of both image's opacity channels.
     *
     * For a variant with the destination on top instead of the source, see
     * [dstATop].
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_srcATop.png)
     */
    srcATop(android.graphics.PorterDuff.Mode.SRC_ATOP),

    /**
     * Composite the destination image over the source image, but only where it
     * overlaps the source.
     *
     * This corresponds to the "Destination atop Source" Porter-Duff operator.
     *
     * This is essentially the [dstOver] operator, but with the output's opacity
     * channel being set to that of the source image instead of being a
     * combination of both image's opacity channels.
     *
     * For a variant with the source on top instead of the destination, see
     * [srcATop].
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_dstATop.png)
     */
    dstATop(android.graphics.PorterDuff.Mode.DST_ATOP),

    /**
     * Apply a bitwise `xor` operator to the source and destination images. This
     * leaves transparency where they would overlap.
     *
     * This corresponds to the "Source xor Destination" Porter-Duff operator.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_xor.png)
     */
    xor(android.graphics.PorterDuff.Mode.XOR),

    /**
     * Sum the components of the source and destination images.
     *
     * Transparency in a pixel of one of the images reduces the contribution of
     * that image to the corresponding output pixel, as if the color of that
     * pixel in that image was darker.
     *
     * This corresponds to the "Source plus Destination" Porter-Duff operator.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_plus.png)
     */
    plus(android.graphics.PorterDuff.Mode.ADD),

    /**
     * Multiply the color components of the source and destination images.
     *
     * This can only result in the same or darker colors (multiplying by white,
     * 1.0, results in no change; multiplying by black, 0.0, results in black).
     *
     * When compositing two opaque images, this has similar effect to overlapping
     * two transparencies on a projector.
     *
     * For a variant that also multiplies the alpha channel, consider [multiply].
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_modulate.png)
     *
     * See also:
     *
     *  * [screen], which does a similar computation but inverted.
     *  * [overlay], which combines [modulate] and [screen] to favor the
     *    destination image.
     *  * [hardLight], which combines [modulate] and [screen] to favor the
     *    source image.
     */
    modulate(null), // Not supported

    // Following blend modes are defined in the CSS Compositing standard.

    /**
     * Multiply the inverse of the components of the source and destination
     * images, and inverse the result.
     *
     * Inverting the components means that a fully saturated channel (opaque
     * white) is treated as the value 0.0, and values normally treated as 0.0
     * (black, transparent) are treated as 1.0.
     *
     * This is essentially the same as [modulate] blend mode, but with the values
     * of the colors inverted before the multiplication and the result being
     * inverted back before rendering.
     *
     * This can only result in the same or lighter colors (multiplying by black,
     * 1.0, results in no change; multiplying by white, 0.0, results in white).
     * Similarly, in the alpha channel, it can only result in more opaque colors.
     *
     * This has similar effect to two projectors displaying their images on the
     * same screen simultaneously.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_screen.png)
     *
     * See also:
     *
     *  * [modulate], which does a similar computation but without inverting the
     *    values.
     *  * [overlay], which combines [modulate] and [screen] to favor the
     *    destination image.
     *  * [hardLight], which combines [modulate] and [screen] to favor the
     *    source image.
     */
    screen(android.graphics.PorterDuff.Mode.SCREEN), // The last coeff mode.

    /**
     * Multiply the components of the source and destination images after
     * adjusting them to favor the destination.
     *
     * Specifically, if the destination value is smaller, this multiplies it with
     * the source value, whereas is the source value is smaller, it multiplies
     * the inverse of the source value with the inverse of the destination value,
     * then inverts the result.
     *
     * Inverting the components means that a fully saturated channel (opaque
     * white) is treated as the value 0.0, and values normally treated as 0.0
     * (black, transparent) are treated as 1.0.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_overlay.png)
     *
     * See also:
     *
     *  * [modulate], which always multiplies the values.
     *  * [screen], which always multiplies the inverses of the values.
     *  * [hardLight], which is similar to [overlay] but favors the source image
     *    instead of the destination image.
     */
    overlay(android.graphics.PorterDuff.Mode.OVERLAY),

    /**
     * Composite the source and destination image by choosing the lowest value
     * from each color channel.
     *
     * The opacity of the output image is computed in the same way as for
     * [srcOver].
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_darken.png)
     */
    darken(android.graphics.PorterDuff.Mode.DARKEN),

    /**
     * Composite the source and destination image by choosing the highest value
     * from each color channel.
     *
     * The opacity of the output image is computed in the same way as for
     * [srcOver].
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_lighten.png)
     */
    lighten(android.graphics.PorterDuff.Mode.LIGHTEN),

    /**
     * Divide the destination by the inverse of the source.
     *
     * Inverting the components means that a fully saturated channel (opaque
     * white) is treated as the value 0.0, and values normally treated as 0.0
     * (black, transparent) are treated as 1.0.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_colorDodge.png)
     */
    colorDodge(null), // Not supported

    /**
     * Divide the inverse of the destination by the the source, and inverse the result.
     *
     * Inverting the components means that a fully saturated channel (opaque
     * white) is treated as the value 0.0, and values normally treated as 0.0
     * (black, transparent) are treated as 1.0.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_colorBurn.png)
     */
    colorBurn(null), // Not supported

    /**
     * Multiply the components of the source and destination images after
     * adjusting them to favor the source.
     *
     * Specifically, if the source value is smaller, this multiplies it with the
     * destination value, whereas is the destination value is smaller, it
     * multiplies the inverse of the destination value with the inverse of the
     * source value, then inverts the result.
     *
     * Inverting the components means that a fully saturated channel (opaque
     * white) is treated as the value 0.0, and values normally treated as 0.0
     * (black, transparent) are treated as 1.0.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_hardLight.png)
     *
     * See also:
     *
     *  * [modulate], which always multiplies the values.
     *  * [screen], which always multiplies the inverses of the values.
     *  * [overlay], which is similar to [hardLight] but favors the destination
     *    image instead of the source image.
     */
    hardLight(null), // Not supported

    /**
     * Use [colorDodge] for source values below 0.5 and [colorBurn] for source
     * values above 0.5.
     *
     * This results in a similar but softer effect than [overlay].
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_softLight.png)
     *
     * See also:
     *
     *  * [color], which is a more subtle tinting effect.
     */
    softLight(null), // Not supported

    /**
     * Subtract the smaller value from the bigger value for each channel.
     *
     * Compositing black has no effect; compositing white inverts the colors of
     * the other image.
     *
     * The opacity of the output image is computed in the same way as for
     * [srcOver].
     *
     * The effect is similar to [exclusion] but harsher.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_difference.png)
     */
    difference(null), // Not supported

    /**
     * Subtract double the product of the two images from the sum of the two
     * images.
     *
     * Compositing black has no effect; compositing white inverts the colors of
     * the other image.
     *
     * The opacity of the output image is computed in the same way as for
     * [srcOver].
     *
     * The effect is similar to [difference] but softer.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_exclusion.png)
     */
    exclusion(null), // Not supported

    /**
     * Multiply the components of the source and destination images, including
     * the alpha channel.
     *
     * This can only result in the same or darker colors (multiplying by white,
     * 1.0, results in no change; multiplying by black, 0.0, results in black).
     *
     * Since the alpha channel is also multiplied, a fully-transparent pixel
     * (opacity 0.0) in one image results in a fully transparent pixel in the
     * output. This is similar to [dstIn], but with the colors combined.
     *
     * For a variant that multiplies the colors but does not multiply the alpha
     * channel, consider [modulate].
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_multiply.png)
     */
    multiply(android.graphics.PorterDuff.Mode.MULTIPLY), // The last separable mode.

    /**
     * Take the hue of the source image, and the saturation and luminosity of the
     * destination image.
     *
     * The effect is to tint the destination image with the source image.
     *
     * The opacity of the output image is computed in the same way as for
     * [srcOver]. Regions that are entirely transparent in the source image take
     * their hue from the destination.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_hue.png)
     *
     * See also:
     *
     *  * [color], which is a similar but stronger effect as it also applies the
     *    saturation of the source image.
     *  * [HSVColor], which allows colors to be expressed using Hue rather than
     *    the red/green/blue channels of [Color].
     */
    hue(null), // Not Supported

    /**
     * Take the saturation of the source image, and the hue and luminosity of the
     * destination image.
     *
     * The opacity of the output image is computed in the same way as for
     * [srcOver]. Regions that are entirely transparent in the source image take
     * their saturation from the destination.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_hue.png)
     *
     * See also:
     *
     *  * [color], which also applies the hue of the source image.
     *  * [luminosity], which applies the luminosity of the source image to the
     *    destination.
     */
    saturation(null), // Not supported

    /**
     * Take the hue and saturation of the source image, and the luminosity of the
     * destination image.
     *
     * The effect is to tint the destination image with the source image.
     *
     * The opacity of the output image is computed in the same way as for
     * [srcOver]. Regions that are entirely transparent in the source image take
     * their hue and saturation from the destination.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_color.png)
     *
     * See also:
     *
     *  * [hue], which is a similar but weaker effect.
     *  * [softLight], which is a similar tinting effect but also tints white.
     *  * [saturation], which only applies the saturation of the source image.
     */
    color(null), // Not supported

    /**
     * Take the luminosity of the source image, and the hue and saturation of the
     * destination image.
     *
     * The opacity of the output image is computed in the same way as for
     * [srcOver]. Regions that are entirely transparent in the source image take
     * their luminosity from the destination.
     *
     * ![](https://flutter.github.io/assets-for-api-docs/assets/dart-ui/blend_mode_luminosity.png)
     *
     * See also:
     *
     *  * [saturation], which applies the saturation of the source image to the
     *    destination.
     *  * [ImageFilter.blur], which can be used with [BackdropFilter] for a
     *    related effect.
     */
    luminosity(null); // Not supported

    fun toPorterDuffMode(): android.graphics.PorterDuff.Mode {
        if (porterDuffMode == null) {
            TODO("Migration/njawad: " + this + " does not have equivalent PorterDuff mode")
        } else {
            return porterDuffMode
        }
    }
}