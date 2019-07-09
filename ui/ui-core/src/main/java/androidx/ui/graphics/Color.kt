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
package androidx.ui.graphics

import androidx.annotation.AnyThread
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.annotation.Size
import androidx.ui.lerp
import androidx.ui.util.Float16
import kotlin.math.max
import kotlin.math.min

/**
 * The `Color` class contains color information to be used while painting
 * in [Canvas]. `Color` supports [ColorSpace]s with 3 [components][ColorSpace.componentCount],
 * plus one for [alpha].
 *
 * <h3>Creating</h3>
 *
 * `Color` can be created with one of these methods:
 *
 *     // from 4 separate [Float] components. Alpha and ColorSpace are optional
 *     val rgbaWhiteFloat = Color(red = 1f, green = 1f, blue = 1f, alpha = 1f,
 *         ColorSpace.get(ColorSpace.Named.Srgb))
 *     // from a 32-bit SRGB color integer
 *     val fromIntWhite = Color(android.graphics.Color.WHITE)
 *     // from SRGB integer component values. Alpha is optional
 *     val rgbaWhiteInt = Color(red = 0xFF, green = 0xFF, blue = 0xFF, alpha = 0xFF)
 *     // from a component array
 *     val xyzWhite = Color(xyzComponents, ColorSpace.get(ColorSpace.Named.Xyz))
 *
 * <h3>Representation</h3>
 *
 * A `Color` always defines a color using 4 components packed in a single
 * 64 bit long value. One of these components is always alpha while the other
 * three components depend on the color space's [color model][ColorSpace.Model].
 * The most common color model is the [RGB][ColorSpace.Model.Rgb] model in
 * which the components represent red, green and blue values.
 *
 * **Component ranges:** the ranges defined in the tables
 * below indicate the ranges that can be encoded in a color long. They do not
 * represent the actual ranges as they may differ per color space. For instance,
 * the RGB components of a color in the [Display P3][ColorSpace.Named.DisplayP3]
 * color space use the \([0..1]\) range. Please refer to the documentation of the
 * various [color spaces][ColorSpace.Named] to find their respective ranges.
 *
 * **Alpha range:** while alpha is encoded in a color long using
 * a 10 bit integer (thus using a range of \([0..1023]\)), it is converted to and
 * from \([0..1]\) float values when decoding and encoding color longs.
 *
 * **sRGB color space:** for compatibility reasons and ease of
 * use, color longs encoding [sRGB][ColorSpace.Named.Srgb] colors do not
 * use the same encoding as other color longs.
 *
 * <table summary="Color definition">
 * <tr><th>Component</th><th>Name</th><th>Size</th><th>Range</th></tr>
 * <tr><td colspan="4">[RGB][ColorSpace.Model.Rgb] color model</td></tr>
 * <tr><td>R</td><td>Red</td><td>16 bits</td><td>\([-65504.0, 65504.0]\)</td></tr>
 * <tr><td>G</td><td>Green</td><td>16 bits</td><td>\([-65504.0, 65504.0]\)</td></tr>
 * <tr><td>B</td><td>Blue</td><td>16 bits</td><td>\([-65504.0, 65504.0]\)</td></tr>
 * <tr><td>A</td><td>Alpha</td><td>10 bits</td><td>\([0..1023]\)</td></tr>
 * <tr><td></td><td>Color space</td><td>6 bits</td><td>\([0..63]\)</td></tr>
 * <tr><td colspan="4">[sRGB][ColorSpace.Named.Srgb] color space</td></tr>
 * <tr><td>A</td><td>Alpha</td><td>8 bits</td><td>\([0..255]\)</td></tr>
 * <tr><td>R</td><td>Red</td><td>8 bits</td><td>\([0..255]\)</td></tr>
 * <tr><td>G</td><td>Green</td><td>8 bits</td><td>\([0..255]\)</td></tr>
 * <tr><td>B</td><td>Blue</td><td>8 bits</td><td>\([0..255]\)</td></tr>
 * <tr><td>X</td><td>Unused</td><td>32 bits</td><td>\(0\)</td></tr>
 * <tr><td colspan="4">[XYZ][ColorSpace.Model.Xyz] color model</td></tr>
 * <tr><td>X</td><td>X</td><td>16 bits</td><td>\([-65504.0, 65504.0]\)</td></tr>
 * <tr><td>Y</td><td>Y</td><td>16 bits</td><td>\([-65504.0, 65504.0]\)</td></tr>
 * <tr><td>Z</td><td>Z</td><td>16 bits</td><td>\([-65504.0, 65504.0]\)</td></tr>
 * <tr><td>A</td><td>Alpha</td><td>10 bits</td><td>\([0..1023]\)</td></tr>
 * <tr><td></td><td>Color space</td><td>6 bits</td><td>\([0..63]\)</td></tr>
 * <tr><td colspan="4">[Lab][ColorSpace.Model.Xyz] color model</td></tr>
 * <tr><td>L</td><td>L</td><td>16 bits</td><td>\([-65504.0, 65504.0]\)</td></tr>
 * <tr><td>a</td><td>a</td><td>16 bits</td><td>\([-65504.0, 65504.0]\)</td></tr>
 * <tr><td>b</td><td>b</td><td>16 bits</td><td>\([-65504.0, 65504.0]\)</td></tr>
 * <tr><td>A</td><td>Alpha</td><td>10 bits</td><td>\([0..1023]\)</td></tr>
 * <tr><td></td><td>Color space</td><td>6 bits</td><td>\([0..63]\)</td></tr>
 * <tr><td colspan="4">[CMYK][ColorSpace.Model.Cmyk] color model</td></tr>
 * <tr><td colspan="4">Unsupported</td></tr>
 * </table>
 *
 * The components in this table are listed in encoding order (see below),
 * which is why color longs in the RGB model are called RGBA colors (even if
 * this doesn't quite hold for the special case of sRGB colors).
 *
 * The color long encoding relies on half-precision float values (fp16). If you
 * wish to know more about the limitations of half-precision float values, please
 * refer to the documentation of the [Float16] class.
 *
 * The values returned by these methods depend on the color space encoded
 * in the color long. The values are however typically in the \([0..1]\) range
 * for RGB colors. Please refer to the documentation of the various
 * [color spaces][ColorSpace.Named] for the exact ranges.
 */
@UseExperimental(kotlin.ExperimentalUnsignedTypes::class)
@AnyThread
class Color constructor(private val value: ULong) {
    /**
     * Returns this color's color space.
     *
     * @return A non-null instance of [ColorSpace]
     */
    val colorSpace: ColorSpace
        get() = ColorSpace[(value and 0x3fUL).toInt()]

    /**
     * Returns the number of components that form a color value according
     * to this color space's color model, plus one extra component for
     * alpha.
     *
     * @return The integer 4
     */
    val componentCount: Int get() = 4

    /**
     * Returns the color model of this color.
     *
     * @return A non-null [ColorSpace.Model]
     */
    val model: ColorSpace.Model
        get() = colorSpace.model

    /**
     * Indicates whether this color color is in a wide-gamut color space.
     * See [ColorSpace.isWideGamut] for a definition of a wide-gamut
     * color space.
     *
     * @return `true` if this color is in a wide-gamut color space, `false` otherwise
     *
     * @see isSrgb
     * @see ColorSpace.isWideGamut
     */
    val isWideGamut: Boolean
        get() = colorSpace.isWideGamut

    /**
     * Indicates whether this color is in the [sRGB][ColorSpace.Named.Srgb]
     * color space.
     *
     * @return `true` if this color is in the sRGB color space, `false` otherwise
     *
     * @see isWideGamut
     */
    val isSrgb: Boolean
        get() = colorSpace.isSrgb

    /**
     * Returns this color's components as a new array. The last element of the
     * array is always the alpha component.
     *
     * @return A new, non-null array whose size is 4
     *
     * @see getComponent
     * @see getComponents
     */
    @Size(value = 4)
    fun getComponents(): FloatArray = floatArrayOf(red, green, blue, alpha)

    /**
     * Converts this color from its color space to the specified color space.
     * The conversion is done using the default rendering intent as specified
     * by [ColorSpace.connect].
     *
     * @param colorSpace The destination color space, cannot be null
     *
     * @return A non-null color instance in the specified color space
     */
    fun convert(colorSpace: ColorSpace): Color {
        if (colorSpace == this.colorSpace) {
            return this // nothing to convert
        }
        val connector = ColorSpace.connect(this.colorSpace, colorSpace)
        val color = getComponents()
        connector.transform(color)
        return Color(color, colorSpace)
    }

    /**
     * Converts this color to an ARGB color int. A color int is always in
     * the [sRGB][ColorSpace.Named.Srgb] color space. This implies
     * a color space conversion is applied if needed.
     *
     * @return An ARGB color in the sRGB color space
     */
    @ColorInt
    fun toArgb(): Int {
        if (colorSpace.isSrgb) {
            return (alpha * 255.0f + 0.5f).toInt() shl 24 or
                    ((red * 255.0f + 0.5f).toInt() shl 16) or
                    ((green * 255.0f + 0.5f).toInt() shl 8) or
                    (blue * 255.0f + 0.5f).toInt()
        }

        val color = getComponents()
        // The transformation saturates the output
        ColorSpace.connect(colorSpace).transform(color)

        return (color[3] * 255.0f + 0.5f).toInt() shl 24 or
                ((color[0] * 255.0f + 0.5f).toInt() shl 16) or
                ((color[1] * 255.0f + 0.5f).toInt() shl 8) or
                (color[2] * 255.0f + 0.5f).toInt()
    }

    /**
     * Returns the value of the red component in the range defined by this
     * color's color space (see [ColorSpace.getMinValue] and
     * [ColorSpace.getMaxValue]).
     *
     * If this color's color model is not [RGB][ColorSpace.Model.Rgb],
     * calling this method is equivalent to `getComponent(0)`.
     *
     * @see alpha
     * @see blue
     * @see green
     * @see getComponents
     * @see getComponent
     */
    val red: Float
        get() {
            return if ((value and 0x3fUL) == 0UL) {
                ((value shr 48) and 0xffUL).toFloat() / 255.0f
            } else {
                Float16(((value shr 48) and 0xffffUL).toShort()).toFloat()
            }
        }

    /**
     * Returns the value of the green component in the range defined by this
     * color's color space (see [ColorSpace.getMinValue] and
     * [ColorSpace.getMaxValue]).
     *
     * If this color's color model is not [RGB][ColorSpace.Model.Rgb],
     * calling this method is equivalent to `getComponent(1)`.
     *
     * @see alpha
     * @see red
     * @see blue
     * @see getComponents
     * @see getComponent
     */
    val green: Float
        get() {
            return if ((value and 0x3fUL) == 0UL) {
                ((value shr 40) and 0xffUL).toFloat() / 255.0f
            } else {
                Float16(((value shr 32) and 0xffffUL).toShort()).toFloat()
            }
        }

    /**
     * Returns the value of the blue component in the range defined by this
     * color's color space (see [ColorSpace.getMinValue] and
     * [ColorSpace.getMaxValue]).
     *
     * If this color's color model is not [RGB][ColorSpace.Model.Rgb],
     * calling this method is equivalent to `getComponent(2)`.
     *
     * @see alpha
     * @see red
     * @see green
     * @see getComponents
     * @see getComponent
     */
    val blue: Float
        get() {
            return if ((value and 0x3fUL) == 0UL) {
                ((value shr 32) and 0xffUL).toFloat() / 255.0f
            } else {
                Float16(((value shr 16) and 0xffffUL).toShort()).toFloat()
            }
        }

    /**
     * Returns the value of the alpha component in the range \([0..1]\).
     * Calling this method is equivalent to `getComponent(3)`.
     *
     * @see red
     * @see green
     * @see blue
     * @see getComponents
     * @see getComponent
     */
    val alpha: Float
        get() {
            return if ((value and 0x3fUL) == 0UL) {
                ((value shr 56) and 0xffUL).toFloat() / 255.0f
            } else {
                ((value shr 6) and 0x3ffUL).toFloat() / 1023.0f
            }
        }

    /**
     * Copies this color's components in the supplied array. The last element of the
     * array is always the alpha component.
     *
     * @param components An array of floats whose size must be 4.
     * @return The array passed as a parameter
     *
     * @see getComponent
     * @throws IllegalArgumentException If the specified array's length is not 4
     */
    @Size(4)
    fun getComponents(@Size(4) components: FloatArray): FloatArray {
        if (components.size != 4) {
            throw IllegalArgumentException("The specified array's length must be 4")
        }

        components[0] = red
        components[1] = green
        components[2] = blue
        components[3] = alpha
        return components
    }

    /**
     * Returns the value of the specified component in the range defined by
     * this color's color space (see [ColorSpace.getMinValue] and
     * [ColorSpace.getMaxValue]).
     *
     * If the requested component index is [componentCount],
     * this method returns the alpha component, always in the range
     * \([0..1]\).
     *
     * @see getComponents
     * @throws ArrayIndexOutOfBoundsException If the specified component index
     * is < 0 or >= 4
     */
    fun getComponent(@IntRange(from = 0, to = 3) component: Int): Float {
        return when (component) {
            0 -> red
            1 -> green
            2 -> blue
            3 -> alpha
            else -> throw IndexOutOfBoundsException("component index must be between 0 and 3")
        }
    }

    /**
     * Returns the relative luminance of this color.
     *
     * Based on the formula for relative luminance defined in WCAG 2.0,
     * W3C Recommendation 11 December 2008.
     *
     * @return A value between 0 (darkest black) and 1 (lightest white)
     *
     * @throws IllegalArgumentException If the this color's color space
     * does not use the [RGB][ColorSpace.Model.Rgb] color model
     */
    fun luminance(): Float {
        if (colorSpace.model != ColorSpace.Model.Rgb) {
            throw IllegalArgumentException(
                    ("The specified color must be encoded in an RGB " +
                            "color space. The supplied color space is " + colorSpace.model)
            )
        }

        val eotf = (colorSpace as ColorSpace.Rgb).eotf
        val r = eotf(red.toDouble())
        val g = eotf(green.toDouble())
        val b = eotf(blue.toDouble())

        return saturate(((0.2126 * r) + (0.7152 * g) + (0.0722 * b)).toFloat())
    }

    /**
     * Copies the existing color, changing only the provided values. The [ColorSpace][colorSpace]
     * of the returned [Color] is the same as this [colorSpace].
     */
    fun copy(
        alpha: Float = this.alpha,
        red: Float = this.red,
        green: Float = this.green,
        blue: Float = this.blue
    ): Color = Color(
        red = red,
        green = green,
        blue = blue,
        alpha = alpha,
        colorSpace = this.colorSpace
    )

    operator fun compareTo(other: Color): Int {
        return value.compareTo(other.value)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Color) {
            return false
        }
        return other.value == value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    /**
     * Returns a string representation of the object. This method returns
     * a string equal to the value of:
     *
     *     "Color($r, $g, $b, $a, ${colorSpace.name})"
     *
     * For instance, the string representation of opaque black in the sRGB
     * color space is equal to the following value:
     *
     *     Color(0.0, 0.0, 0.0, 1.0, sRGB IEC61966-2.1)
     *
     * @return A non-null string representation of the object
     */
    override fun toString(): String {
        return "Color($red, $green, $blue, $alpha, ${colorSpace.name})"
    }

    companion object {
        @ColorInt
        private const val BLACK: Int = 0xFF000000.toInt()
        @ColorInt
        private const val DKGRAY: Int = 0xFF444444.toInt()
        @ColorInt
        private const val GRAY: Int = 0xFF888888.toInt()
        @ColorInt
        private const val LTGRAY: Int = 0xFFCCCCCC.toInt()
        @ColorInt
        private const val WHITE: Int = 0xFFFFFFFF.toInt()
        @ColorInt
        private const val RED: Int = 0xFFFF0000.toInt()
        @ColorInt
        private const val GREEN: Int = 0xFF00FF00.toInt()
        @ColorInt
        private const val BLUE: Int = 0xFF0000FF.toInt()
        @ColorInt
        private const val YELLOW: Int = 0xFFFFFF00.toInt()
        @ColorInt
        private const val CYAN: Int = 0xFF00FFFF.toInt()
        @ColorInt
        private const val MAGENTA: Int = 0xFFFF00FF.toInt()
        @ColorInt
        private const val TRANSPARENT: Int = 0
        @ColorInt
        private const val AQUA: Int = 0xFF00FFFF.toInt()
        @ColorInt
        private const val FUCHSIA: Int = 0xFFFF00FF.toInt()
        @ColorInt
        private const val LIME: Int = 0xFF00FF00.toInt()
        @ColorInt
        private const val MAROON: Int = 0xFF800000.toInt()
        @ColorInt
        private const val NAVY: Int = 0xFF000080.toInt()
        @ColorInt
        private const val OLIVE: Int = 0xFF808000.toInt()
        @ColorInt
        private const val PURPLE: Int = 0xFF800080.toInt()
        @ColorInt
        private const val SILVER: Int = 0xFFC0C0C0.toInt()
        @ColorInt
        private const val TEAL: Int = 0xFF008080.toInt()

        val Black = Color(BLACK)
        val DarkGray = Color(DKGRAY)
        val Gray = Color(GRAY)
        val LightGray = Color(LTGRAY)
        val White = Color(WHITE)
        val Red = Color(RED)
        val Green = Color(GREEN)
        val Blue = Color(BLUE)
        val Yellow = Color(YELLOW)
        val Cyan = Color(CYAN)
        val Magenta = Color(MAGENTA)
        val Transparent = Color(TRANSPARENT)
        val Aqua = Color(AQUA)
        val Fuchsia = Color(FUCHSIA)
        val Lime = Color(LIME)
        val Maroon = Color(MAROON)
        val Navy = Color(NAVY)
        val Olive = Color(OLIVE)
        val Purple = Color(PURPLE)
        val Silver = Color(SILVER)
        val Teal = Color(TEAL)

        private fun saturate(v: Float): Float {
            return if (v <= 0.0f) 0.0f else (if (v >= 1.0f) 1.0f else v)
        }

        /**
         * Parse the color string, and return the corresponding color-int.
         * If the string cannot be parsed, throws an IllegalArgumentException
         * exception. Supported formats are:
         *
         *  * `#RRGGBB`
         *  * `#AARRGGBB`
         *
         * The following names are also accepted: `red`, `blue`,
         * `green`, `black`, `white`, `gray`,
         * `cyan`, `magenta`, `yellow`, `lightgray`,
         * `darkgray`, `grey`, `lightgrey`, `darkgrey`,
         * `aqua`, `fuchsia`, `lime`, `maroon`,
         * `navy`, `olive`, `purple`, `silver`,
         * `teal`, and `transparent`.
         */
        fun parse(@Size(min = 3) colorString: String): Color {
            return if (colorString[0] == '#') {
                // Use a long to avoid rollovers on #ffXXXXXX
                var color = colorString.substring(1).toULong(16)
                if (colorString.length == 7) {
                    // Set the alpha value
                    color = color or 0x00000000ff000000UL
                } else if (colorString.length != 9) {
                    throw IllegalArgumentException("Unknown color")
                }
                Color(color.toInt())
            } else {
                ColorNameMap[colorString.toLowerCase()]
                        ?: throw IllegalArgumentException("Unknown color")
            }
        }

        private val ColorNameMap = mapOf(
                "black" to Black,
                "darkgray" to DarkGray,
                "gray" to Gray,
                "lightgray" to LightGray,
                "white" to White,
                "red" to Red,
                "green" to Green,
                "blue" to Blue,
                "yellow" to Yellow,
                "cyan" to Cyan,
                "magenta" to Magenta,
                "aqua" to Aqua,
                "fuchsia" to Fuchsia,
                "darkgrey" to DarkGray,
                "grey" to Gray,
                "lightgrey" to LightGray,
                "lime" to Lime,
                "maroon" to Maroon,
                "navy" to Navy,
                "olive" to Olive,
                "purple" to Purple,
                "silver" to Silver,
                "teal" to Teal,
                "transparent" to Transparent
        )
    }
}

/**
 * Create a [Color] by passing individual [red], [green], [blue], [alpha], and [colorSpace]
 * components. The default [color space][ColorSpace] is [SRGB][ColorSpace.Named.Srgb] and
 * the default [alpha] is `1.0` (opaque).
 */
@UseExperimental(ExperimentalUnsignedTypes::class)
fun Color(
    red: Float,
    green: Float,
    blue: Float,
    alpha: Float = 1f,
    colorSpace: ColorSpace = ColorSpace.Named.Srgb.colorSpace
): Color {
    if (colorSpace.componentCount != 3) {
        throw IllegalArgumentException("Color only works with ColorSpaces with 3 components")
    }
    if (colorSpace.isSrgb) {
        val argb = (((alpha * 255.0f + 0.5f).toInt() shl 24) or
                ((red * 255.0f + 0.5f).toInt() shl 16) or
                ((green * 255.0f + 0.5f).toInt() shl 8) or
                (blue * 255.0f + 0.5f).toInt())
        return Color(value = (argb.toULong() and 0xffffffffUL) shl 32)
    }

    val id = colorSpace.id
    if (id == ColorSpace.MinId) {
        throw IllegalArgumentException(
                "Unknown color space, please use a color space returned by ColorSpace.get()"
        )
    }

    val r = Float16(red)
    val g = Float16(green)
    val b = Float16(blue)

    val a = (max(0.0f, min(alpha, 1.0f)) * 1023.0f + 0.5f).toInt()

    // Suppress sign extension
    return Color(value = (((r.halfValue.toLong() and 0xffffL) shl 48) or (
            (g.halfValue.toLong() and 0xffffL) shl 32) or (
            (b.halfValue.toLong() and 0xffffL) shl 16) or (
            (a.toLong() and 0x3ffL) shl 6) or (
            id.toLong() and 0x3fL)).toULong())
}

/**
 * Creates a new [Color] in the specified color space with the
 * specified component values. The range of the components is defined by
 * [ColorSpace.getMinValue] and [ColorSpace.getMaxValue].
 * The values passed to this method must be in the proper range. The alpha
 * component is always in the range \([0..1]\).
 *
 * The [ColorSpace] must have a [ColorSpace.componentCount] of 3. The length
 * of the array of [components] must be 4. The component at index 3
 * is always alpha.</p>
 *
 * @param components The components of the color to create, with alpha as the last component
 * @param colorSpace The color space of the color to create
 * @return An instance of [Color]
 *
 * @throws IllegalArgumentException If the [components] array length is not 4
 */
@UseExperimental(ExperimentalUnsignedTypes::class)
fun Color(components: FloatArray, colorSpace: ColorSpace): Color {
    if (components.size != 4) {
        throw IllegalArgumentException("components must have 4 elements")
    }
    return Color(
            red = components[0],
            green = components[1],
            blue = components[2],
            alpha = components[3],
            colorSpace = colorSpace
    )
}

/**
 * Creates a new [Color] instance from an ARGB color int.
 * The resulting color is in the [sRGB][ColorSpace.Named.Srgb]
 * color space.
 *
 * @param color The ARGB color int to create a <code>Color</code> from
 * @return A non-null instance of {@link Color}
 */
@UseExperimental(ExperimentalUnsignedTypes::class)
fun Color(@ColorInt color: Int): Color {
    return Color(value = (color.toULong() and 0xffffffffUL) shl 32)
}

/**
 * Creates a new [Color] instance from an ARGB color components.
 * The resulting color is in the [sRGB][ColorSpace.Named.Srgb]
 * color space. The default alpha value is `0xFF` (opaque).
 *
 * @return A non-null instance of {@link Color}
 */
@UseExperimental(ExperimentalUnsignedTypes::class)
fun Color(
    @IntRange(from = 0, to = 0xFF) red: Int,
    @IntRange(from = 0, to = 0xFF) green: Int,
    @IntRange(from = 0, to = 0xFF) blue: Int,
    @IntRange(from = 0, to = 0xFF) alpha: Int = 0xFF
): Color {
    @ColorInt val color = ((alpha and 0xFF) shl 24) or
            ((red and 0xFF) shl 16) or
            ((green and 0xFF) shl 8) or
            (blue and 0xFF)
    return Color(color)
}

/**
 * Linear interpolate between two [Colors][Color], [a] and [b] with [t] fraction between
 * the two. The [ColorSpace] of the result is always the [ColorSpace][Color.colorSpace] of [b].
 */
fun lerp(a: Color, b: Color, @FloatRange(from = 0.0, to = 1.0) t: Float): Color {
    val linearColorSpace = ColorSpace.Named.LinearExtendedSrgb.colorSpace
    val startColor = a.convert(linearColorSpace)
    val endColor = b.convert(linearColorSpace)

    val startA = startColor.alpha
    val startR = startColor.red
    val startG = startColor.green
    val startB = startColor.blue

    val endA = endColor.alpha
    val endR = endColor.red
    val endG = endColor.green
    val endB = endColor.blue

    val interpolated = Color(
        alpha = lerp(startA, endA, t),
        red = lerp(startR, endR, t),
        green = lerp(startG, endG, t),
        blue = lerp(startB, endB, t),
        colorSpace = linearColorSpace
    )
    return interpolated.convert(b.colorSpace)
}
