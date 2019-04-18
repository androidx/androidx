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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.ui.painting

import androidx.ui.toHexString
import kotlin.math.pow
import kotlin.math.roundToInt

// TODO(mount): Move to Color long
/**
 * An immutable 32 bit color value in ARGB format.
 *
 * Consider the light teal of the Flutter logo. It is fully opaque, with a red
 * channel value of 0x42 (66), a green channel value of 0xA5 (165), and a blue
 * channel value of 0xF5 (245). In the common "hash syntax" for colour values,
 * it would be described as `#42A5F5`.
 *
 * Here are some ways it could be constructed:
 *
 * ```dart
 * Color c = const Color(0xFF42A5F5);
 * Color c = const Color.fromARGB(0xFF, 0x42, 0xA5, 0xF5);
 * Color c = const Color.fromARGB(255, 66, 165, 245);
 * Color c = const Color.fromRGBO(66, 165, 245, 1.0);
 * ```
 *
 * If you are having a problem with `Color` wherein it seems your color is just
 * not painting, check to make sure you are specifying the full 8 hexadecimal
 * digits. If you only specify six, then the leading two digits are assumed to
 * be zero, which means fully-transparent:
 *
 * ```dart
 * Color c1 = const Color(0xFFFFFF); // fully transparent white (invisible)
 * Color c2 = const Color(0xFFFFFFFF); // fully opaque white (visible)
 * ```
 *
 * See also:
 *
 *  * [Colors](https://docs.flutter.io/flutter/material/Colors-class.html), which
 *    defines the colors found in the Material Design specification.
 *
 * Ctor comment:
 * Construct a color from the lower 32 bits of an [int].
 *
 * The bits are interpreted as follows:
 *
 * * Bits 24-31 are the alpha value.
 * * Bits 16-23 are the red value.
 * * Bits 8-15 are the green value.
 * * Bits 0-7 are the blue value.
 *
 * In other words, if AA is the alpha value in hex, RR the red value in hex,
 * GG the green value in hex, and BB the blue value in hex, a color can be
 * expressed as `const Color(0xAARRGGBB)`.
 *
 * For example, to get a fully opaque orange, you would use `const
 * Color(0xFFFF9000)` (`FF` for the alpha, `FF` for the red, `90` for the
 * green, and `00` for the blue).
 */
class Color(colorValue: Int) {
    /**
     * A 32 bit value representing this color.
     *
     * The bits are assigned as follows:
     *
     * * Bits 24-31 are the alpha value.
     * * Bits 16-23 are the red value.
     * * Bits 8-15 are the green value.
     * * Bits 0-7 are the blue value.
     */
    val value: Int = colorValue and 0xFFFFFFFF.toInt()

    companion object {
        /**
         * Construct a color from the lower 8 bits of four integers.
         *
         * * `a` is the alpha value, with 0 being transparent and 255 being fully
         *   opaque.
         * * `r` is [red], from 0 to 255.
         * * `g` is [green], from 0 to 255.
         * * `b` is [blue], from 0 to 255.
         *
         * Out of range values are brought into range using modulo 255.
         *
         * See also [fromRGBO], which takes the alpha value as a floating point
         * value.
         */
        fun fromARGB(a: Int, r: Int, g: Int, b: Int): Color {
            return Color((((a and 0xff) shl 24) or
                    ((r and 0xff) shl 16) or
                    ((g and 0xff) shl 8) or
                    ((b and 0xff) shl 0)) and 0xFFFFFFFF.toInt())
        }

        /**
         * Create a color from red, green, blue, and opacity, similar to `rgba()` in CSS.
         *
         * * `r` is [red], from 0 to 255.
         * * `g` is [green], from 0 to 255.
         * * `b` is [blue], from 0 to 255.
         * * `opacity` is alpha channel of this color as a Float, with 0.0 being
         *   transparent and 1.0 being fully opaque.
         *
         * Out of range values are brought into range using modulo 255.
         *
         * See also [fromARGB], which takes the opacity as an integer value.
         */
        fun fromRGBO(r: Int, g: Int, b: Int, opacity: Float): Color {
            return Color(
                (
                        ((((opacity * 0xff.toFloat()).toInt()) and 0xff) shl 24) or
                                ((r and 0xff) shl 16) or
                                ((g and 0xff) shl 8) or
                                ((b and 0xff) shl 0)
                        ) and 0xFFFFFFFF.toInt()
            )
        }

        // See <https://www.w3.org/TR/WCAG20/#relativeluminancedef>
        fun _linearizeColorComponent(component: Float): Float {
            if (component <= 0.03928f)
                return component / 12.92f
            return ((component + 0.055f) / 1.055f).pow(2.4f)
        }

        /**
         * Linearly interpolate between two colors.
         *
         * This is intended to be fast but as a result may be ugly. Consider
         * [HSVColor] or writing custom logic for interpolating colors.
         *
         * If either color is null, this function linearly interpolates from a
         * transparent instance of the other color. This is usually preferable to
         * interpolating from [material.Colors.transparent] (`const
         * Color(0x00000000)`), which is specifically transparent _black_.
         *
         * The `t` argument represents position on the timeline, with 0.0 meaning
         * that the interpolation has not started, returning `a` (or something
         * equivalent to `a`), 1.0 meaning that the interpolation has finished,
         * returning `b` (or something equivalent to `b`), and values in between
         * meaning that the interpolation is at the relevant point on the timeline
         * between `a` and `b`. The interpolation can be extrapolated beyond 0.0 and
         * 1.0, so negative values and values greater than 1.0 are valid (and can
         * easily be generated by curves such as [Curves.elasticInOut]). Each channel
         * will be clamped to the range 0 to 255.
         *
         * Values for `t` are usually obtained from an [Animation<Float>], such as
         * an [AnimationController].
         */
        fun lerp(a: Color?, b: Color?, t: Float?): Color? {
            assert(t != null)
            if (a == null && b == null)
                return null
            if (a == null)
                return _scaleAlpha(b!!, t!!)
            if (b == null)
                return _scaleAlpha(a, 1.0f - t!!)
            return blend(a, b, t!!.toFloat())
        }

        fun blend(startColor: Color, endColor: Color, fraction: Float): Color {

            val startA = (startColor.alpha and 0xff) / 255.0f
            var startR = (startColor.red and 0xff) / 255.0f
            var startG = (startColor.green and 0xff) / 255.0f
            var startB = (startColor.blue and 0xff) / 255.0f

            val endA = (endColor.alpha and 0xff) / 255.0f
            var endR = (endColor.red and 0xff) / 255.0f
            var endG = (endColor.green and 0xff) / 255.0f
            var endB = (endColor.blue and 0xff) / 255.0f

            // convert from sRGB to linear
            startR = startR.pow(2.2f)
            startG = startG.pow(2.2f)
            startB = startB.pow(2.2f)
            endR = endR.pow(2.2f)
            endG = endG.pow(2.2f)
            endB = endB.pow(2.2f)

            // compute the interpolated color in linear space
            var a = startA + fraction * (endA - startA)
            var r = startR + fraction * (endR - startR)
            var g = startG + fraction * (endG - startG)
            var b = startB + fraction * (endB - startB)

            // convert back to sRGB in the [0..255] range
            a *= 255f
            r = r.pow(1.0f / 2.2f) * 255.0f
            g = g.pow(1.0f / 2.2f) * 255.0f
            b = b.pow(1.0f / 2.2f) * 255.0f

            return Color.fromARGB(Math.round(a), Math.round(r), Math.round(g), Math.round(b))
        }

        /**
         * Combine the foreground color as a transparent color over top
         * of a background color, and return the resulting combined color.
         *
         * This uses standard alpha blending ("SRC over DST") rules to produce a
         * blended color from two colors. This can be used as a performance
         * enhancement when trying to avoid needless alpha blending compositing
         * operations for two things that are solid colors with the same shape, but
         * overlay each other: instead, just paint one with the combined color.
         */
        fun alphaBlend(foreground: Color, background: Color): Color {
            val alpha = foreground.alpha
            if (alpha == 0x00) { // Foreground completely transparent.
                return background
            }
            val invAlpha = 0xff - alpha
            var backAlpha = background.alpha
            if (backAlpha == 0xff) { // Opaque background case
                return fromARGB(
                    0xff,
                    (alpha * foreground.red + invAlpha * background.red) / 0xff,
                    (alpha * foreground.green + invAlpha * background.green) / 0xff,
                    (alpha * foreground.blue + invAlpha * background.blue) / 0xff
                )
            } else { // General case
                backAlpha = (backAlpha * invAlpha) / 0xff
                val outAlpha = alpha + backAlpha
                assert(outAlpha != 0x00)
                return fromARGB(
                    outAlpha,
                    (foreground.red * alpha + background.red * backAlpha) / outAlpha,
                    (foreground.green * alpha + background.green * backAlpha) / outAlpha,
                    (foreground.blue * alpha + background.blue * backAlpha) / outAlpha
                )
            }
        }

        /**
         * Fully transparent color.
         */
        val Transparent = Color(0)
    }

    /**
     * The alpha channel of this color in an 8 bit value.
     *
     * A value of 0 means this color is fully transparent. A value of 255 means
     * this color is fully opaque.
     */
    val alpha get() = (0xff000000.toInt() and value) ushr 24

    /**
     * The alpha channel of this color as a Float.
     *
     * A value of 0.0 means this color is fully transparent. A value of 1.0 means
     * this color is fully opaque.
     */
    val opacity: Float = alpha.toFloat() / 0xFF.toFloat()

    /** The red channel of this color in an 8 bit value. */
    val red = (0x00ff0000 and value) shr 16

    /** The green channel of this color in an 8 bit value. */
    val green = (0x0000ff00 and value) shr 8

    /** The blue channel of this color in an 8 bit value. */
    val blue = (0x000000ff and value) shr 0

    /**
     * Returns a new color that matches this color with the alpha channel
     * replaced with `a` (which ranges from 0 to 255).
     *
     * Out of range values will have unexpected effects.
     */
    fun withAlpha(a: Int): Color {
        return fromARGB(a, red, green, blue)
    }

    /**
     * Returns a new color that matches this color with the alpha channel
     * replaced with the given `opacity` (which ranges from 0.0 to 1.0).
     *
     * Out of range values will have unexpected effects.
     */
    fun withOpacity(opacity: Float): Color {
        assert(opacity >= 0.0f && opacity <= 1.0f)
        return withAlpha((255.0 * opacity).roundToInt())
    }

    /**
     * Returns a new color that matches this color with the red channel replaced
     * with `r` (which ranges from 0 to 255).
     *
     * Out of range values will have unexpected effects.
     */
    fun withRed(r: Int): Color {
        return fromARGB(alpha, r, green, blue)
    }

    /**
     * Returns a new color that matches this color with the green channel
     * replaced with `g` (which ranges from 0 to 255).
     *
     * Out of range values will have unexpected effects.
     */
    fun withGreen(g: Int): Color {
        return fromARGB(alpha, red, g, blue)
    }

    /**
     * Returns a new color that matches this color with the blue channel replaced
     * with `b` (which ranges from 0 to 255).
     *
     * Out of range values will have unexpected effects.
     */
    fun withBlue(b: Int): Color {
        return fromARGB(alpha, red, green, b)
    }

    /**
     * Returns a brightness value between 0 for darkest and 1 for lightest.
     *
     * Represents the relative luminance of the color. This value is computationally
     * expensive to calculate.
     *
     * See <https://en.wikipedia.org/wiki/Relative_luminance>.
     */
    fun computeLuminance(): Float {
        // See <https://www.w3.org/TR/WCAG20/#relativeluminancedef>
        val R = _linearizeColorComponent(red / 0xFF.toFloat())
        val G = _linearizeColorComponent(green / 0xFF.toFloat())
        val B = _linearizeColorComponent(blue / 0xFF.toFloat())
        return 0.2126f * R + 0.7152f * G + 0.0722f * B
    }

    // Autogenerated by AS
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Color

        if (value != other.value) return false

        return true
    }

    // Autogenerated by AS
    override fun hashCode(): Int {
        return value
    }

    override fun toString() = "Color(${value.toHexString()})"
}

fun _scaleAlpha(a: Color, factor: Float): Color {
    return a.withAlpha((a.alpha * factor).roundToInt().coerceIn(0, 255))
}

/**
 * Returns a new color that matches this color with the alpha channel
 * replaced with [a] defined in percents (ranges from 0 to 100 which
 * translates to the range of alpha value from 0 to 255).
 *
 * Out of range values will have unexpected effects.
 */
inline fun Color.withAlphaPercent(a: Float): Color = withAlpha((a * 2.55f).toInt())
