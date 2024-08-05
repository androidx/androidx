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

package androidx.ink.brush.color

import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.ink.brush.color.colorspace.ColorModel
import androidx.ink.brush.color.colorspace.ColorSpace
import androidx.ink.brush.color.colorspace.ColorSpaces
import androidx.ink.brush.color.colorspace.connect
import kotlin.math.max
import kotlin.math.min

/**
 * The `Color` class contains color information to be used while painting in Ink. `Color` supports
 * [ColorSpace]s with 3 [components][ColorSpace.componentCount], plus one for [alpha].
 *
 * ### Creating
 *
 * `Color` can be created with one of these methods:
 *
 *     // from 4 separate [Float] components. Alpha and ColorSpace are optional
 *     val rgbaWhiteFloat = Color(red = 1f, green = 1f, blue = 1f, alpha = 1f,
 *         ColorSpace.get(ColorSpaces.Srgb))
 *     // from a 32-bit SRGB color integer
 *     val fromIntWhite = Color(android.graphics.Color.WHITE)
 *     val fromLongBlue = Color(0xFF0000FF)
 *     // from SRGB integer component values. Alpha is optional
 *     val rgbaWhiteInt = Color(red = 0xFF, green = 0xFF, blue = 0xFF, alpha = 0xFF)
 *
 * ### Representation
 *
 * A `Color` always defines a color using 4 components packed in a single 64 bit long value. One of
 * these components is always alpha while the other three components depend on the color space's
 * [color model][ColorModel]. The most common color model is the [RGB][ColorModel.Rgb] model in
 * which the components represent red, green, and blue values.
 *
 * **Component ranges:** the ranges defined in the tables below indicate the ranges that can be
 * encoded in a color long. They do not represent the actual ranges as they may differ per color
 * space. For instance, the RGB components of a color in the [Display P3][ColorSpaces.DisplayP3]
 * color space use the `[0..1]` range. Please refer to the documentation of the various
 * [color spaces][ColorSpaces] to find their respective ranges.
 *
 * **Alpha range:** while alpha is encoded in a color long using a 10 bit integer (thus using a
 * range of `[0..1023]`), it is converted to and from `[0..1]` float values when decoding and
 * encoding color longs.
 *
 * **sRGB color space:** for compatibility reasons and ease of use, `Color` encoded
 * [sRGB][ColorSpaces.Srgb] colors do not use the same encoding as other color longs.
 *
 * ```
 * | Component | Name        | Size    | Range                 |
 * |-----------|-------------|---------|-----------------------|
 * | [RGB][ColorSpace.Model.Rgb] color model                   |
 * | R         | Red         | 16 bits | `[-65504.0, 65504.0]` |
 * | G         | Green       | 16 bits | `[-65504.0, 65504.0]` |
 * | B         | Blue        | 16 bits | `[-65504.0, 65504.0]` |
 * | A         | Alpha       | 10 bits | `[0..1023]`           |
 * |           | Color space | 6 bits  | `[0..63]`             |
 * | [SRGB][ColorSpaces.Srgb] color space                      |
 * | A         | Alpha       | 8 bits  | `[0..255]`            |
 * | R         | Red         | 8 bits  | `[0..255]`            |
 * | G         | Green       | 8 bits  | `[0..255]`            |
 * | B         | Blue        | 8 bits  | `[0..255]`            |
 * | X         | Unused      | 32 bits | `[0]`                 |
 * | [XYZ][ColorSpace.Model.Xyz] color model                   |
 * | X         | X           | 16 bits | `[-65504.0, 65504.0]` |
 * | Y         | Y           | 16 bits | `[-65504.0, 65504.0]` |
 * | Z         | Z           | 16 bits | `[-65504.0, 65504.0]` |
 * | A         | Alpha       | 10 bits | `[0..1023]`           |
 * |           | Color space | 6 bits  | `[0..63]`             |
 * | [Lab][ColorSpace.Model.Lab] color model                   |
 * | L         | L           | 16 bits | `[-65504.0, 65504.0]` |
 * | a         | a           | 16 bits | `[-65504.0, 65504.0]` |
 * | b         | b           | 16 bits | `[-65504.0, 65504.0]` |
 * | A         | Alpha       | 10 bits | `[0..1023]`           |
 * |           | Color space | 6 bits  | `[0..63]`             |
 * ```
 *
 * The components in this table are listed in encoding order, which is why color longs in the RGB
 * model are called RGBA colors (even if this doesn't quite hold for the special case of sRGB
 * colors).
 *
 * The color encoding relies on half-precision float values (fp16). If you wish to know more about
 * the limitations of half-precision float values, please refer to the documentation of the
 * [Float16] class.
 *
 * The values returned by these methods depend on the color space encoded in the color long. The
 * values are however typically in the `[0..1]` range for RGB colors. Please refer to the
 * documentation of the various [color spaces][ColorSpaces] for the exact ranges.
 */
@JvmInline
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public value class Color(public val value: ULong) {
    /**
     * Returns this color's color space.
     *
     * @return A non-null instance of [ColorSpace]
     */
    public val colorSpace: ColorSpace
        get() = ColorSpaces.getColorSpace((value and 0x3fUL).toInt())

    /**
     * Converts this color from its color space to the specified color space. The conversion is done
     * using the default rendering intent as specified by [ColorSpace.connect].
     *
     * @param colorSpace The destination color space, cannot be null
     * @return A non-null color instance in the specified color space
     */
    public fun convert(colorSpace: ColorSpace): Color {
        // If the destination color space is the same as this color's color space,
        // the connector we get will be the identity connector
        val connector = this.colorSpace.connect(colorSpace)
        return connector.transformToColor(this)
    }

    /**
     * Returns the value of the red component in the range defined by this color's color space (see
     * [ColorSpace.getMinValue] and [ColorSpace.getMaxValue]).
     *
     * If this color's color model is not [RGB][ColorModel.Rgb], calling this is the first component
     * of the ColorSpace.
     *
     * @see alpha
     * @see blue
     * @see green
     */
    public val red: Float
        get() {
            return if ((value and 0x3fUL) == 0UL) {
                ((value shr 48) and 0xffUL).toFloat() / 255.0f
            } else {
                halfToFloat(((value shr 48) and 0xffffUL).toShort())
            }
        }

    /**
     * Returns the value of the green component in the range defined by this color's color space
     * (see [ColorSpace.getMinValue] and [ColorSpace.getMaxValue]).
     *
     * If this color's color model is not [RGB][ColorModel.Rgb], calling this is the second
     * component of the ColorSpace.
     *
     * @see alpha
     * @see red
     * @see blue
     */
    public val green: Float
        get() {
            return if ((value and 0x3fUL) == 0UL) {
                ((value shr 40) and 0xffUL).toFloat() / 255.0f
            } else {
                halfToFloat(((value shr 32) and 0xffffUL).toShort())
            }
        }

    /**
     * Returns the value of the blue component in the range defined by this color's color space (see
     * [ColorSpace.getMinValue] and [ColorSpace.getMaxValue]).
     *
     * If this color's color model is not [RGB][ColorModel.Rgb], calling this is the third component
     * of the ColorSpace.
     *
     * @see alpha
     * @see red
     * @see green
     */
    public val blue: Float
        get() {
            return if ((value and 0x3fUL) == 0UL) {
                ((value shr 32) and 0xffUL).toFloat() / 255.0f
            } else {
                halfToFloat(((value shr 16) and 0xffffUL).toShort())
            }
        }

    /**
     * Returns the value of the alpha component in the range `[0..1]`.
     *
     * @see red
     * @see green
     * @see blue
     */
    public val alpha: Float
        get() {
            return if ((value and 0x3fUL) == 0UL) {
                ((value shr 56) and 0xffUL).toFloat() / 255.0f
            } else {
                ((value shr 6) and 0x3ffUL).toFloat() / 1023.0f
            }
        }

    @Suppress("NOTHING_TO_INLINE") public inline operator fun component1(): Float = red

    @Suppress("NOTHING_TO_INLINE") public inline operator fun component2(): Float = green

    @Suppress("NOTHING_TO_INLINE") public inline operator fun component3(): Float = blue

    @Suppress("NOTHING_TO_INLINE") public inline operator fun component4(): Float = alpha

    @Suppress("NOTHING_TO_INLINE") public inline operator fun component5(): ColorSpace = colorSpace

    /**
     * Copies the existing color, changing only the provided values. The [ColorSpace][colorSpace] of
     * the returned [Color] is the same as this [colorSpace].
     */
    public fun copy(
        alpha: Float = this.alpha,
        red: Float = this.red,
        green: Float = this.green,
        blue: Float = this.blue
    ): Color =
        Color(red = red, green = green, blue = blue, alpha = alpha, colorSpace = this.colorSpace)

    /**
     * Returns a string representation of the object. This method returns a string equal to the
     * value of:
     *
     *     "Color($r, $g, $b, $a, ${colorSpace.name})"
     *
     * For instance, the string representation of opaque black in the sRGB color space is equal to
     * the following value:
     *
     *     Color(0.0, 0.0, 0.0, 1.0, sRGB IEC61966-2.1)
     *
     * @return A non-null string representation of the object
     */
    override fun toString(): String {
        return "Color($red, $green, $blue, $alpha, ${colorSpace.name})"
    }

    public companion object {
        public val Black: Color = Color(0xFF000000.toInt())

        public val DarkGray: Color = Color(0xFF444444.toInt())

        public val Gray: Color = Color(0xFF888888.toInt())

        public val LightGray: Color = Color(0xFFCCCCCC.toInt())

        public val White: Color = Color(0xFFFFFFFF.toInt())

        public val Red: Color = Color(0xFFFF0000.toInt())

        public val Green: Color = Color(0xFF00FF00.toInt())

        public val Blue: Color = Color(0xFF0000FF.toInt())

        public val Yellow: Color = Color(0xFFFFFF00.toInt())

        public val Cyan: Color = Color(0xFF00FFFF.toInt())

        public val Magenta: Color = Color(0xFFFF00FF.toInt())

        public val Transparent: Color = Color(0x00000000)

        /**
         * Because Color is an inline class, this represents an unset value without having to box
         * the Color. It will be treated as [Transparent] when drawn. A Color can compare with
         * [Unspecified] for equality or use [isUnspecified] to check for the unset value or
         * [isSpecified] for any color that isn't [Unspecified].
         */
        public val Unspecified: Color = Color(0f, 0f, 0f, 0f, ColorSpaces.Unspecified)
    }
}

/**
 * Create a [Color] by passing individual [red], [green], [blue], [alpha], and [colorSpace]
 * components. The default [color space][ColorSpace] is [sRGB][ColorSpaces.Srgb] and the default
 * [alpha] is `1.0` (opaque).
 *
 * If the [red], [green], or [blue] values are outside of the range defined by [colorSpace] (see
 * [ColorSpace.getMinValue] and [ColorSpace.getMaxValue], these values get clamped appropriately to
 * be within range.
 *
 * @throws IllegalArgumentException If [colorSpace] does not have [ColorSpace.componentCount] equal
 *   to 3.
 * @throws IllegalArgumentException If [colorSpace] has an [ColorSpace.id] set to
 *   [ColorSpace.MinId].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Color(
    red: Float,
    green: Float,
    blue: Float,
    alpha: Float = 1f,
    colorSpace: ColorSpace = ColorSpaces.Srgb
): Color {
    if (colorSpace.isSrgb) {
        val argb =
            (((alpha.fastCoerceIn(0.0f, 1.0f) * 255.0f + 0.5f).toInt() shl 24) or
                ((red.fastCoerceIn(0.0f, 1.0f) * 255.0f + 0.5f).toInt() shl 16) or
                ((green.fastCoerceIn(0.0f, 1.0f) * 255.0f + 0.5f).toInt() shl 8) or
                (blue.fastCoerceIn(0.0f, 1.0f) * 255.0f + 0.5f).toInt())
        return Color(argb.toULong() shl 32)
    }

    requirePrecondition(colorSpace.componentCount == 3) {
        "Color only works with ColorSpaces with 3 components"
    }

    val id = colorSpace.id
    requirePrecondition(id != ColorSpace.MinId) {
        "Unknown color space, please use a color space in ColorSpaces"
    }

    val r = floatToHalf(red.fastCoerceIn(colorSpace.getMinValue(0), colorSpace.getMaxValue(0)))
    val g = floatToHalf(green.fastCoerceIn(colorSpace.getMinValue(1), colorSpace.getMaxValue(1)))
    val b = floatToHalf(blue.fastCoerceIn(colorSpace.getMinValue(2), colorSpace.getMaxValue(2)))
    val a = (alpha.fastCoerceIn(0.0f, 1.0f) * 1023.0f + 0.5f).toInt()

    return Color(
        (((r.toLong() and 0xffffL) shl 48) or
                ((g.toLong() and 0xffffL) shl 32) or
                ((b.toLong() and 0xffffL) shl 16) or
                ((a.toLong() and 0x03ffL) shl 6) or
                (id.toLong() and 0x003fL))
            .toULong()
    )
}

/**
 * Create a [Color] by passing individual [red], [green], [blue], [alpha], and [colorSpace]
 * components. This function is equivalent to [Color] but doesn't perform any check/validation of
 * the parameters. It is meant to be used when the color space and values are known to be valid by
 * construction, for instance when lerping colors.
 */
internal fun UncheckedColor(
    red: Float,
    green: Float,
    blue: Float,
    alpha: Float = 1f,
    colorSpace: ColorSpace = ColorSpaces.Srgb
): Color {
    if (colorSpace.isSrgb) {
        val argb =
            (((alpha * 255.0f + 0.5f).toInt() shl 24) or
                ((red * 255.0f + 0.5f).toInt() shl 16) or
                ((green * 255.0f + 0.5f).toInt() shl 8) or
                (blue * 255.0f + 0.5f).toInt())
        return Color(argb.toULong() shl 32)
    }

    val r = floatToHalf(red)
    val g = floatToHalf(green)
    val b = floatToHalf(blue)

    val a = (max(0.0f, min(alpha, 1.0f)) * 1023.0f + 0.5f).toInt()

    val id = colorSpace.id

    return Color(
        (((r.toLong() and 0xffffL) shl 48) or
                ((g.toLong() and 0xffffL) shl 32) or
                ((b.toLong() and 0xffffL) shl 16) or
                ((a.toLong() and 0x03ffL) shl 6) or
                (id.toLong() and 0x003fL))
            .toULong()
    )
}

/**
 * Creates a new [Color] instance from an ARGB color int. The resulting color is in the
 * [sRGB][ColorSpaces.Srgb] color space.
 *
 * @param color The ARGB color int to create a <code>Color</code> from.
 * @return A non-null instance of {@link Color}
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Color(@ColorInt color: Int): Color {
    return Color(color.toULong() shl 32)
}

/**
 * Creates a new [Color] instance from an ARGB color components. The resulting color is in the
 * [sRGB][ColorSpaces.Srgb] color space. The default alpha value is `0xFF` (opaque).
 *
 * @param red The red component of the color, between 0 and 255.
 * @param green The green component of the color, between 0 and 255.
 * @param blue The blue component of the color, between 0 and 255.
 * @param alpha The alpha component of the color, between 0 and 255.
 * @return A non-null instance of {@link Color}
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Color(
    @IntRange(from = 0, to = 0xFF) red: Int,
    @IntRange(from = 0, to = 0xFF) green: Int,
    @IntRange(from = 0, to = 0xFF) blue: Int,
    @IntRange(from = 0, to = 0xFF) alpha: Int = 0xFF
): Color {
    val color =
        ((alpha and 0xFF) shl 24) or
            ((red and 0xFF) shl 16) or
            ((green and 0xFF) shl 8) or
            (blue and 0xFF)
    return Color(color)
}

/**
 * Converts this color to an ARGB color int. A color int is always in the [sRGB][ColorSpaces.Srgb]
 * color space. This implies a color space conversion is applied if needed.
 *
 * @return An ARGB color in the sRGB color space
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ColorInt
public fun Color.toArgb(): Int {
    return (convert(ColorSpaces.Srgb).value shr 32).toInt()
}

/** `false` when this is [Color.Unspecified]. */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline val Color.isSpecified: Boolean
    get() = value != Color.Unspecified.value

/** `true` when this is [Color.Unspecified]. */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline val Color.isUnspecified: Boolean
    get() = value == Color.Unspecified.value

/**
 * If this color [isSpecified] then this is returned, otherwise [block] is executed and its result
 * is returned.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun Color.takeOrElse(block: () -> Color): Color = if (isSpecified) this else block()
