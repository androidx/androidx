/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.document.intervals

import androidx.annotation.RestrictTo
import androidx.compose.remote.document.ColorAttributeType
import androidx.compose.remote.document.FloatFormatSpec
import androidx.compose.remote.document.RemoteColorMath
import androidx.compose.remote.document.TextFromFloatFormatter
import androidx.compose.remote.document.TextTransformOperation
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.nextDown
import kotlin.math.nextUp

/**
 * Conservative summary of a RemoteCompose string value across all reachable runtime states,
 * guaranteed to encompass every string value the expression can produce at runtime.
 *
 * @property length Non-negative character (UTF-16 code unit) length range `[minLen, maxLen]`.
 * @property isAsciiOnly Whether every reachable character is guaranteed to lie in `0x00..0x7F`
 *   (used so case conversions preserve UTF-16 length).
 * @property constantValue The exact constant string if all reachable executions yield one string,
 *   or `null` otherwise.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class StringSummary
@JvmOverloads
constructor(
    public val length: IntInterval,
    public val isAsciiOnly: Boolean,
    public val constantValue: String? = null,
) {
    /** Joins this string summary with [other] over alternative runtime paths. */
    public fun union(other: StringSummary): StringSummary =
        StringSummary(
            length = length.union(other.length),
            isAsciiOnly = isAsciiOnly && other.isAsciiOnly,
            constantValue =
                if (constantValue != null && constantValue == other.constantValue) {
                    constantValue
                } else {
                    null
                },
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StringSummary) return false
        return length == other.length &&
            isAsciiOnly == other.isAsciiOnly &&
            constantValue == other.constantValue
    }

    override fun hashCode(): Int {
        var result = length.hashCode()
        result = 31 * result + isAsciiOnly.hashCode()
        result = 31 * result + (constantValue?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "StringSummary(length=$length, isAsciiOnly=$isAsciiOnly, constantValue=$constantValue)"

    public companion object {
        /** Conservative summary for an unknown or host-overridden string (length is still >= 0). */
        public val UNKNOWN: StringSummary =
            StringSummary(
                length = IntInterval.NON_NEGATIVE,
                isAsciiOnly = false,
                constantValue = null,
            )

        /** Returns an exact summary for a known literal string [value]. */
        @JvmStatic
        public fun constant(value: String): StringSummary =
            StringSummary(
                length = IntInterval.point(value.length),
                isAsciiOnly = value.all { it.code < 128 },
                constantValue = value,
            )
    }
}

/**
 * Conservative per-channel 8-bit ARGB interval `[0, 255]^4` guaranteed to enclose every color value
 * a RemoteCompose expression can produce at runtime.
 *
 * Matches wire `RemoteComposeState.mColorMap`, which stores every color as a packed 32-bit
 * `0xAARRGGBB` integer.
 *
 * @property alpha 8-bit alpha channel interval in `[0, 255]`.
 * @property red 8-bit red channel interval in `[0, 255]`.
 * @property green 8-bit green channel interval in `[0, 255]`.
 * @property blue 8-bit blue channel interval in `[0, 255]`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ColorInterval(
    public val alpha: IntInterval,
    public val red: IntInterval,
    public val green: IntInterval,
    public val blue: IntInterval,
) {
    /** Whether all four ARGB channels represent single constant integers. */
    public val isPoint: Boolean
        get() = alpha.isPoint && red.isPoint && green.isPoint && blue.isPoint

    /** Returns the packed `0xAARRGGBB` integer if all four channels are single points, or null. */
    public fun toArgbOrNull(): Int? {
        if (!isPoint) return null
        return RemoteColorMath.argb(alpha.min, red.min, green.min, blue.min)
    }

    /** Component-wise channel union over alternative runtime colors. */
    public fun union(other: ColorInterval): ColorInterval =
        ColorInterval(
            alpha = alpha.union(other.alpha),
            red = red.union(other.red),
            green = green.union(other.green),
            blue = blue.union(other.blue),
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ColorInterval) return false
        return alpha == other.alpha &&
            red == other.red &&
            green == other.green &&
            blue == other.blue
    }

    override fun hashCode(): Int {
        var result = alpha.hashCode()
        result = 31 * result + red.hashCode()
        result = 31 * result + green.hashCode()
        result = 31 * result + blue.hashCode()
        return result
    }

    override fun toString(): String =
        "ColorInterval(alpha=$alpha, red=$red, green=$green, blue=$blue)"

    public companion object {
        /** Full 8-bit unsigned channel range `[0, 255]`. */
        public val BYTE_RANGE: IntInterval = IntInterval(0, 255)

        /** Conservative interval covering every possible 32-bit ARGB color. */
        public val UNKNOWN: ColorInterval =
            ColorInterval(BYTE_RANGE, BYTE_RANGE, BYTE_RANGE, BYTE_RANGE)

        /** Returns an exact point [ColorInterval] for a packed 32-bit `0xAARRGGBB` color int. */
        @JvmStatic
        public fun constant(argb: Int): ColorInterval =
            ColorInterval(
                alpha = IntInterval.point(RemoteColorMath.alpha(argb)),
                red = IntInterval.point(RemoteColorMath.red(argb)),
                green = IntInterval.point(RemoteColorMath.green(argb)),
                blue = IntInterval.point(RemoteColorMath.blue(argb)),
            )
    }
}

/**
 * Pure-Kotlin abstract transfer functions for RemoteCompose string and color operations.
 *
 * Uses [TextFromFloatFormatter] and [RemoteColorMath] directly so both document-level and
 * expression-level interval analyzers share identical semantics without depending on `remote-core`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object StringAndColorIntervalOps {
    private const val EXACT_INT_FLOAT_LIMIT = FloatInterval.MAX_EXACT_INT.toFloat()

    /**
     * Computes a conservative [StringSummary] for a `TextFromFloat` formatting operation.
     *
     * @param valueInterval Conservative bounding range of the input float value.
     * @param digitsBefore Maximum digits before the decimal point.
     * @param digitsAfter Maximum digits after the decimal point.
     * @param flags Wire formatting flags from `TextFromFloatFlags`.
     * @return Exact constant summary for point intervals, or a conservative length/ASCII summary.
     */
    @JvmStatic
    public fun summarizeTextFromFloat(
        valueInterval: FloatInterval,
        digitsBefore: Int,
        digitsAfter: Int,
        flags: Int,
    ): StringSummary {
        if (
            valueInterval.isEmpty ||
                digitsBefore !in 0..TextFromFloatFormatter.MAX_DIGITS ||
                digitsAfter !in 0..TextFromFloatFormatter.MAX_DIGITS
        ) {
            return StringSummary.UNKNOWN
        }
        val spec = TextFromFloatFormatter.parseSpec(digitsBefore, digitsAfter, flags)
        if (valueInterval.isPoint) {
            return StringSummary.constant(TextFromFloatFormatter.format(valueInterval.min, spec))
        }
        if (spec.isFullFormat) {
            return StringSummary(length = IntInterval(3, 16), isAsciiOnly = true)
        }

        val signWidth = if (!spec.isLegacyMode && spec.isNegativeParentheses) 2 else 1
        val signMin = if (valueInterval.max < 0f) signWidth else 0
        val signMax = if (valueInterval.min < 0f) signWidth else 0
        val mag = valueInterval.abs()
        val effMagMin = if (mag.minInclusive || mag.min.isInfinite()) mag.min else mag.min.nextUp()
        val effMagMax =
            if (mag.maxInclusive || mag.max.isInfinite()) mag.max else mag.max.nextDown()
        if (effMagMin > effMagMax) return StringSummary.UNKNOWN
        val bodyLen =
            if (spec.isLegacyMode) {
                computeLegacyFloatLengthRange(effMagMin, effMagMax, spec)
            } else {
                computeStandardFloatLengthRange(effMagMin, effMagMax, spec)
            }
        return StringSummary(
            length =
                IntInterval.fromLongBounds(
                    signMin.toLong() + bodyLen.min.toLong(),
                    signMax.toLong() + bodyLen.max.toLong(),
                ),
            isAsciiOnly = true,
        )
    }

    /** Computes a conservative [StringSummary] for string concatenation (`TextMerge`). */
    @JvmStatic
    public fun summarizeTextMerge(s1: StringSummary, s2: StringSummary): StringSummary {
        val constant =
            if (s1.constantValue != null && s2.constantValue != null) {
                s1.constantValue + s2.constantValue
            } else {
                null
            }
        return StringSummary(
            length = (s1.length + s2.length).clampToRange(0, IntInterval.MAX_BOUND),
            isAsciiOnly = s1.isAsciiOnly && s2.isAsciiOnly,
            constantValue = constant,
        )
    }

    /** Computes a conservative [StringSummary] for a `TextTransform` case or trim operation. */
    @JvmStatic
    public fun summarizeTextTransform(
        src: StringSummary,
        start: FloatInterval,
        len: FloatInterval,
        operation: Int,
    ): StringSummary {
        val sub = summarizeSubstring(src = src, start = start, len = len)
        return when (operation) {
            TextTransformOperation.CAPITALIZE,
            TextTransformOperation.UPPERCASE_FIRST_CHAR ->
                StringSummary(length = sub.length, isAsciiOnly = sub.isAsciiOnly)
            TextTransformOperation.TO_LOWERCASE,
            TextTransformOperation.TO_UPPERCASE -> {
                if (sub.isAsciiOnly) {
                    StringSummary(length = sub.length, isAsciiOnly = false)
                } else {
                    val minLen = if (sub.length.min > 0) 1 else 0
                    val maxLen = (sub.length * IntInterval.point(3)).max
                    StringSummary(length = IntInterval(minLen, maxLen), isAsciiOnly = false)
                }
            }
            TextTransformOperation.TRIM -> {
                val constant = sub.constantValue?.trim { it <= ' ' }
                if (constant != null) {
                    StringSummary.constant(constant)
                } else {
                    StringSummary(
                        length = IntInterval(0, sub.length.max),
                        isAsciiOnly = sub.isAsciiOnly,
                    )
                }
            }
            else -> StringSummary.UNKNOWN
        }
    }

    /** Computes a conservative [StringSummary] for a `TextSubtext` slice `(start, len)`. */
    @JvmStatic
    public fun summarizeSubstring(
        src: StringSummary,
        start: FloatInterval,
        len: FloatInterval,
    ): StringSummary {
        if (src.constantValue != null && start.isPoint && len.isPoint) {
            val s = start.min.toInt()
            val end =
                if (len.min == -1f) src.constantValue.length else (start.min + len.min).toInt()
            if (s in 0..src.constantValue.length && end in s..src.constantValue.length) {
                return StringSummary.constant(src.constantValue.substring(s, end))
            }
        }

        val srcLen = src.length
        val maxOut = if (srcLen.hasFiniteMax) srcLen.max else IntInterval.MAX_BOUND
        val startInt = SlotInterval.fromFloat(start).toIntInterval()
        val clampedStart = startInt.clampToRange(0, maxOut)
        val remainingLen = (srcLen - clampedStart).clampToRange(0, maxOut)
        val remainderLen = if (-1f in len) remainingLen else null
        val finiteLen =
            summarizeFiniteSubstringLength(start, len, clampedStart, remainingLen, maxOut)

        val combinedLen =
            when {
                remainderLen != null && finiteLen != null -> remainderLen.union(finiteLen)
                remainderLen != null -> remainderLen
                finiteLen != null -> finiteLen
                else -> IntInterval(0, maxOut)
            }
        return StringSummary(length = combinedLen, isAsciiOnly = src.isAsciiOnly)
    }

    /** Computes a conservative [ColorInterval] for normalized `[0, 1]` ARGB float inputs. */
    @JvmStatic
    public fun summarizeColorArgb(
        alpha: FloatInterval,
        red: FloatInterval,
        green: FloatInterval,
        blue: FloatInterval,
    ): ColorInterval {
        if (alpha.isPoint && red.isPoint && green.isPoint && blue.isPoint) {
            return ColorInterval.constant(
                RemoteColorMath.toArgb(alpha.min, red.min, green.min, blue.min)
            )
        }
        val qa = quantizeColorChannel(alpha)
        val qr = quantizeColorChannel(red)
        val qg = quantizeColorChannel(green)
        val qb = quantizeColorChannel(blue)

        val bOk = qb.min >= 0 && qb.max <= 255
        val gOk = bOk && qg.min >= 0 && qg.max <= 255
        val rOk = gOk && qr.min >= 0 && qr.max <= 255
        val aOk = rOk && qa.min >= 0 && qa.max <= 255
        return ColorInterval(
            alpha = if (aOk) qa else ColorInterval.BYTE_RANGE,
            red = if (rOk) qr else ColorInterval.BYTE_RANGE,
            green = if (gOk) qg else ColorInterval.BYTE_RANGE,
            blue = if (bOk) qb else ColorInterval.BYTE_RANGE,
        )
    }

    /**
     * Computes a conservative [ColorInterval] for an HSV color expression with fixed alpha byte.
     */
    @JvmStatic
    public fun summarizeColorHsv(
        alpha: Int,
        hue: FloatInterval,
        saturation: FloatInterval,
        value: FloatInterval,
    ): ColorInterval {
        val alphaByte = alpha and 0xFF
        val alphaInterval = IntInterval.point(alphaByte)
        if (hue.isPoint && saturation.isPoint && value.isPoint) {
            val rgb = RemoteColorMath.hsvToRgb(hue.min, saturation.min, value.min)
            val argb = (alphaByte shl 24) or (rgb and 0x00FFFFFF)
            return ColorInterval.constant(argb)
        }
        if (
            !hue.isEmpty &&
                !saturation.isEmpty &&
                !value.isEmpty &&
                hue.min >= 0f &&
                saturation.min >= 0f &&
                saturation.max <= 1f &&
                value.min >= 0f &&
                value.max <= 1f
        ) {
            val rgbMax = (0.5f + 255f * value.max).toInt().coerceIn(0, 255)
            val hueAlwaysInUnit =
                hue.min >= 0f && (hue.max < 1f || (hue.max == 1f && !hue.maxInclusive))
            val rgbMin =
                if (hueAlwaysInUnit) {
                    (0.5f + 255f * value.min * (1f - saturation.max)).toInt().coerceIn(0, 255)
                } else {
                    0
                }
            val ch = IntInterval(min(rgbMin, rgbMax), max(rgbMin, rgbMax))
            return ColorInterval(alpha = alphaInterval, red = ch, green = ch, blue = ch)
        }
        return ColorInterval(
            alpha = alphaInterval,
            red = ColorInterval.BYTE_RANGE,
            green = ColorInterval.BYTE_RANGE,
            blue = ColorInterval.BYTE_RANGE,
        )
    }

    /**
     * Computes a conservative [ColorInterval] for `RemoteColorMath.interpolateColor(c1, c2, t)`.
     *
     * Samples the exact adjacent floats `0f.nextUp()` and `1f.nextDown()` when `[t]` touches `0f`
     * or `1f` to capture gamma-2.2 round-trip truncation dips inside `(0, 1)`.
     *
     * @param c1 Start color interval.
     * @param c2 End color interval.
     * @param t Interpolation fraction interval; values outside `[0, 1]` or empty widen to
     *   [ColorInterval.UNKNOWN].
     * @return Conservative bounding [ColorInterval].
     */
    @JvmStatic
    public fun summarizeColorTween(
        c1: ColorInterval,
        c2: ColorInterval,
        t: FloatInterval,
    ): ColorInterval {
        if (
            t.isEmpty ||
                t.min.isNaN() ||
                t.max.isNaN() ||
                t.min < 0f ||
                t.max > 1f ||
                c1.alpha.isEmpty ||
                c1.red.isEmpty ||
                c1.green.isEmpty ||
                c1.blue.isEmpty ||
                c2.alpha.isEmpty ||
                c2.red.isEmpty ||
                c2.green.isEmpty ||
                c2.blue.isEmpty
        ) {
            return ColorInterval.UNKNOWN
        }
        if (t.isPoint) {
            if (t.min == 0f) return c1
            if (t.min == 1f) return c2
        }
        val c1Min = RemoteColorMath.argb(c1.alpha.min, c1.red.min, c1.green.min, c1.blue.min)
        val c1Max = RemoteColorMath.argb(c1.alpha.max, c1.red.max, c1.green.max, c1.blue.max)
        val c2Min = RemoteColorMath.argb(c2.alpha.min, c2.red.min, c2.green.min, c2.blue.min)
        val c2Max = RemoteColorMath.argb(c2.alpha.max, c2.red.max, c2.green.max, c2.blue.max)

        fun evalFourCorners(tau: Float): ColorInterval =
            ColorInterval.constant(RemoteColorMath.interpolateColor(c1Min, c2Min, tau))
                .union(ColorInterval.constant(RemoteColorMath.interpolateColor(c1Min, c2Max, tau)))
                .union(ColorInterval.constant(RemoteColorMath.interpolateColor(c1Max, c2Min, tau)))
                .union(ColorInterval.constant(RemoteColorMath.interpolateColor(c1Max, c2Max, tau)))

        val result =
            if (t.isPoint) {
                evalFourCorners(t.min)
            } else {
                var acc = evalFourCorners(t.min).union(evalFourCorners(t.max))
                if (t.min == 0f && t.max > 0f) {
                    acc = acc.union(evalFourCorners(min(t.max, 0f.nextUp())))
                }
                if (t.max == 1f && t.min < 1f) {
                    acc = acc.union(evalFourCorners(max(t.min, 1f.nextDown())))
                }
                acc
            }

        // For any channel where c1 or c2 is non-point, IEEE-754 float32 rounding in
        // c1f + t * (c2f - c1f) is non-monotonic at 1-ULP boundaries (error <= 2^-23, well below
        // the minimum 8-bit channel step), which can shift an interior channel output by at most 1.
        fun widenIfNonPoint(
            ch: IntInterval,
            c1Ch: IntInterval,
            c2Ch: IntInterval,
        ): IntInterval =
            if (c1Ch.isPoint && c2Ch.isPoint) {
                ch
            } else {
                IntInterval(ch.min - 1, ch.max + 1).clampToRange(0, 255)
            }

        return ColorInterval(
            alpha = widenIfNonPoint(result.alpha, c1.alpha, c2.alpha),
            red = widenIfNonPoint(result.red, c1.red, c2.red),
            green = widenIfNonPoint(result.green, c1.green, c2.green),
            blue = widenIfNonPoint(result.blue, c1.blue, c2.blue),
        )
    }

    /** Computes a conservative [FloatInterval] for a [ColorAttributeType] extraction on [color]. */
    @JvmStatic
    public fun summarizeColorAttribute(color: ColorInterval, attributeType: Int): FloatInterval {
        val attrType = attributeType and 0xFF
        color.toArgbOrNull()?.let { exactArgb ->
            return summarizeExactColorAttribute(exactArgb, attrType)
        }
        return when (attrType) {
            ColorAttributeType.RED -> FloatInterval(color.red.min / 255f, color.red.max / 255f)
            ColorAttributeType.GREEN ->
                FloatInterval(color.green.min / 255f, color.green.max / 255f)
            ColorAttributeType.BLUE -> FloatInterval(color.blue.min / 255f, color.blue.max / 255f)
            ColorAttributeType.ALPHA ->
                FloatInterval(color.alpha.min / 255f, color.alpha.max / 255f)
            ColorAttributeType.BRIGHTNESS ->
                FloatInterval(
                    maxOf(color.red.min, color.green.min, color.blue.min) / 255f,
                    maxOf(color.red.max, color.green.max, color.blue.max) / 255f,
                )
            ColorAttributeType.SATURATION -> summarizeSaturationInterval(color)
            ColorAttributeType.HUE ->
                FloatInterval(0f, 1f, minInclusive = true, maxInclusive = false)
            else -> FloatInterval.UNBOUNDED
        }
    }

    /** Component-wise color multiplication over normalized `[0, 1]` channels. */
    @JvmStatic
    public fun summarizeColorMultiply(c1: ColorInterval, c2: ColorInterval): ColorInterval =
        summarizeColorArgb(
            alpha =
                FloatInterval(c1.alpha.min / 255f, c1.alpha.max / 255f) *
                    FloatInterval(c2.alpha.min / 255f, c2.alpha.max / 255f),
            red =
                FloatInterval(c1.red.min / 255f, c1.red.max / 255f) *
                    FloatInterval(c2.red.min / 255f, c2.red.max / 255f),
            green =
                FloatInterval(c1.green.min / 255f, c1.green.max / 255f) *
                    FloatInterval(c2.green.min / 255f, c2.green.max / 255f),
            blue =
                FloatInterval(c1.blue.min / 255f, c1.blue.max / 255f) *
                    FloatInterval(c2.blue.min / 255f, c2.blue.max / 255f),
        )

    private fun computeLegacyFloatLengthRange(
        effMagMin: Float,
        effMagMax: Float,
        spec: FloatFormatSpec,
    ): IntInterval {
        val minRawDigits = decimalDigitCount(effMagMin.toInt().toLong())
        val maxRawDigits =
            if (effMagMax.isInfinite() || effMagMax >= Int.MAX_VALUE.toFloat()) {
                10
            } else {
                decimalDigitCount(effMagMax.toInt().toLong())
            }
        val intMin =
            if (spec.padBefore != TextFromFloatFormatter.PAD_NONE) {
                spec.digitsBefore
            } else {
                min(minRawDigits, spec.digitsBefore)
            }
        val intMax =
            if (spec.padBefore != TextFromFloatFormatter.PAD_NONE) {
                spec.digitsBefore
            } else {
                min(maxRawDigits, spec.digitsBefore)
            }
        val fracMin =
            when {
                spec.digitsAfter == 0 -> 0
                spec.padAfter != TextFromFloatFormatter.PAD_NONE -> 1 + spec.digitsAfter
                else -> 1
            }
        val fracMax = if (spec.digitsAfter == 0) 0 else 1 + spec.digitsAfter
        return IntInterval(intMin + fracMin, intMax + fracMax)
    }

    private fun computeStandardFloatLengthRange(
        effMagMin: Float,
        effMagMax: Float,
        spec: FloatFormatSpec,
    ): IntInterval {
        val roundingOffset = if (spec.isRounding) roundingFactor(spec.digitsAfter) else 0f
        val minRounded = effMagMin + roundingOffset
        val maxRounded =
            if (effMagMax.isInfinite()) Float.POSITIVE_INFINITY else effMagMax + roundingOffset

        val minRawDigits = longMagnitudeDigitCount(minRounded)
        val maxRawDigits = longMagnitudeDigitCount(maxRounded)
        val iLenMin = groupedDigitLength(min(spec.digitsBefore, minRawDigits), spec.grouping)
        val iLenMax = groupedDigitLength(min(spec.digitsBefore, maxRawDigits), spec.grouping)

        var minTotal = Int.MAX_VALUE
        var maxTotal = 0
        for (iLen in iLenMin..iLenMax) {
            val finalIntLen =
                if (spec.padBefore != TextFromFloatFormatter.PAD_NONE) {
                    spec.digitsBefore
                } else {
                    min(iLen, spec.digitsBefore)
                }
            val trimAfter = if (iLen + spec.digitsAfter > 9) max(1, 9 - iLen) else spec.digitsAfter
            val effAfter =
                if (spec.padAfter == TextFromFloatFormatter.PAD_NONE) {
                    trimAfter
                } else {
                    spec.digitsAfter
                }
            val fracMin =
                when {
                    effAfter == 0 -> 0
                    spec.padAfter != TextFromFloatFormatter.PAD_NONE -> 1 + effAfter
                    else -> 2
                }
            val fracMax = if (effAfter == 0) 0 else 1 + effAfter
            minTotal = min(minTotal, finalIntLen + fracMin)
            maxTotal = max(maxTotal, finalIntLen + fracMax)
        }
        return if (minTotal > maxTotal) IntInterval.NON_NEGATIVE
        else IntInterval(minTotal, maxTotal)
    }

    private fun longMagnitudeDigitCount(value: Float): Int =
        if (value.isInfinite() || value >= Long.MAX_VALUE.toFloat()) {
            19
        } else {
            decimalDigitCount(value.toLong())
        }

    private fun summarizeFiniteSubstringLength(
        start: FloatInterval,
        len: FloatInterval,
        clampedStart: IntInterval,
        remainingLen: IntInterval,
        maxOut: Int,
    ): IntInterval? {
        if (len.isPoint && len.min == -1f) return null
        val clampedEnd = SlotInterval.fromFloat(start + len).toIntInterval().clampToRange(0, maxOut)
        val fromDiff = clampedEnd - clampedStart
        val rawLen =
            if (
                !start.isUnbounded &&
                    !len.isUnbounded &&
                    start.min >= 0f &&
                    len.min >= 0f &&
                    start.max.toDouble() + len.max.toDouble() + 1.0 <=
                        EXACT_INT_FLOAT_LIMIT.toDouble()
            ) {
                val fromLen =
                    IntInterval.fromLongBounds(floor(len.min).toLong(), ceil(len.max).toLong() + 1L)
                val intersected = fromLen.intersect(fromDiff)
                if (intersected.isEmpty) fromDiff else intersected
            } else {
                fromDiff
            }
        return rawLen.clampToRange(0, maxOut).min(remainingLen)
    }

    private fun quantizeColorChannel(channel: FloatInterval): IntInterval {
        val scaled = channel * FloatInterval.point(255f) + FloatInterval.point(0.5f)
        return SlotInterval.fromFloat(scaled).toIntInterval()
    }

    private fun summarizeExactColorAttribute(exactArgb: Int, attrType: Int): FloatInterval {
        val exact =
            when (attrType) {
                ColorAttributeType.HUE -> RemoteColorMath.hue(exactArgb)
                ColorAttributeType.SATURATION -> RemoteColorMath.saturation(exactArgb)
                ColorAttributeType.BRIGHTNESS -> RemoteColorMath.brightness(exactArgb)
                ColorAttributeType.RED -> RemoteColorMath.red(exactArgb) / 255f
                ColorAttributeType.GREEN -> RemoteColorMath.green(exactArgb) / 255f
                ColorAttributeType.BLUE -> RemoteColorMath.blue(exactArgb) / 255f
                ColorAttributeType.ALPHA -> RemoteColorMath.alpha(exactArgb) / 255f
                else -> return FloatInterval.UNBOUNDED
            }
        return FloatInterval.point(exact)
    }

    private fun summarizeSaturationInterval(c: ColorInterval): FloatInterval {
        val maxLo = maxOf(c.red.min, c.green.min, c.blue.min)
        val maxHi = maxOf(c.red.max, c.green.max, c.blue.max)
        val minLo = minOf(c.red.min, c.green.min, c.blue.min)
        val minHi = minOf(c.red.max, c.green.max, c.blue.max)
        val allCanBeEqual = !c.red.intersect(c.green).intersect(c.blue).isEmpty
        val minSat =
            if (allCanBeEqual || maxLo <= minHi || maxLo == 0) {
                0f
            } else {
                (((maxLo / 255f) - (minHi / 255f)) / (maxLo / 255f)).coerceIn(0f, 1f)
            }
        val maxSat =
            if (maxHi == 0) {
                0f
            } else {
                (((maxHi / 255f) - (minLo / 255f)) / (maxHi / 255f)).coerceIn(0f, 1f)
            }
        return FloatInterval(min(minSat, maxSat), max(minSat, maxSat))
    }

    private fun decimalDigitCount(nonNegativeValue: Long): Int {
        var v = nonNegativeValue
        if (v <= 0L) return 1
        var digits = 0
        while (v > 0L) {
            v /= 10L
            digits++
        }
        return digits
    }

    private fun groupedDigitLength(rawDigits: Int, grouping: Int): Int {
        if (rawDigits <= 0) return 0
        return when (grouping) {
            TextFromFloatFormatter.GROUPING_BY3 -> rawDigits + (rawDigits - 1) / 3
            TextFromFloatFormatter.GROUPING_BY4 -> rawDigits + (rawDigits - 1) / 4
            TextFromFloatFormatter.GROUPING_BY32 ->
                if (rawDigits <= 3) rawDigits else rawDigits + 1 + (rawDigits - 4) / 2
            else -> rawDigits
        }
    }

    private fun roundingFactor(afterDigits: Int): Float {
        var factor = 0.5f
        // Float32 subnormals underflow to 0f after 45 divisions by 10.
        repeat(min(afterDigits.coerceAtLeast(0), 46)) { factor /= 10.0f }
        return factor
    }
}
