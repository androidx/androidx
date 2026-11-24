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

package androidx.compose.remote.document

import androidx.annotation.RestrictTo
import java.util.Objects
import kotlin.math.min

/**
 * Decoded formatting specification for `TextFromFloatNode` matching `TextFromFloat` and
 * `StringUtils` in `remote-core`.
 *
 * @property digitsBefore maximum number of integer digits (and grouping separators) before the
 *   decimal point (in `0..TextFromFloatFormatter.MAX_DIGITS`)
 * @property digitsAfter maximum number of fractional digits after the decimal point (in
 *   `0..TextFromFloatFormatter.MAX_DIGITS`)
 * @property padBefore decoded leading padding character ([TextFromFloatFormatter.PAD_NONE],
 *   [TextFromFloatFormatter.PAD_ZERO], or [TextFromFloatFormatter.PAD_SPACE])
 * @property padAfter decoded trailing padding character ([TextFromFloatFormatter.PAD_NONE],
 *   [TextFromFloatFormatter.PAD_ZERO], or [TextFromFloatFormatter.PAD_SPACE])
 * @property grouping decoded grouping mode (`TextFromFloatFormatter.GROUPING_*`, unshifted `0..3`)
 * @property separator decoded separator style (`TextFromFloatFormatter.SEPARATOR_*`, unshifted
 *   `0..3`)
 * @property isRounding whether half-up rounding is applied before digit extraction
 * @property isNegativeParentheses whether negative numbers are wrapped in parentheses instead of a
 *   leading minus sign
 * @property isLegacyMode whether legacy `StringUtils.floatToString` formatting is used
 * @property isFullFormat whether raw [Float.toString] output is used directly
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FloatFormatSpec(
    public val digitsBefore: Int,
    public val digitsAfter: Int,
    public val padBefore: Char,
    public val padAfter: Char,
    public val grouping: Int,
    public val separator: Int,
    public val isRounding: Boolean,
    public val isNegativeParentheses: Boolean,
    public val isLegacyMode: Boolean,
    public val isFullFormat: Boolean,
) {
    init {
        require(digitsBefore in 0..TextFromFloatFormatter.MAX_DIGITS) {
            "digitsBefore must be in 0..${TextFromFloatFormatter.MAX_DIGITS}, was $digitsBefore"
        }
        require(digitsAfter in 0..TextFromFloatFormatter.MAX_DIGITS) {
            "digitsAfter must be in 0..${TextFromFloatFormatter.MAX_DIGITS}, was $digitsAfter"
        }
    }

    override fun equals(other: Any?): Boolean =
        other is FloatFormatSpec &&
            digitsBefore == other.digitsBefore &&
            digitsAfter == other.digitsAfter &&
            padBefore == other.padBefore &&
            padAfter == other.padAfter &&
            grouping == other.grouping &&
            separator == other.separator &&
            isRounding == other.isRounding &&
            isNegativeParentheses == other.isNegativeParentheses &&
            isLegacyMode == other.isLegacyMode &&
            isFullFormat == other.isFullFormat

    override fun hashCode(): Int =
        Objects.hash(
            digitsBefore,
            digitsAfter,
            padBefore,
            padAfter,
            grouping,
            separator,
            isRounding,
            isNegativeParentheses,
            isLegacyMode,
            isFullFormat,
        )

    override fun toString(): String =
        "FloatFormatSpec(digitsBefore=$digitsBefore, digitsAfter=$digitsAfter, " +
            "padBefore=${padBefore.code}, padAfter=${padAfter.code}, grouping=$grouping, " +
            "separator=$separator, isRounding=$isRounding, " +
            "isNegativeParentheses=$isNegativeParentheses, isLegacyMode=$isLegacyMode, " +
            "isFullFormat=$isFullFormat)"
}

/**
 * Pure-Kotlin port of `TextFromFloat` flag decoding and `StringUtils.floatToString` matching
 * `remote-core` bit-for-bit.
 *
 * Note: The `GROUPING_*`, `SEPARATOR_*`, and `PAD_*` constants on this object represent **decoded,
 * unshifted** values stored on [FloatFormatSpec]. Do **not** pass them directly into
 * `TextFromFloatNode.flags`; use the shifted bitfield constants on [TextFromFloatFlags] instead.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object TextFromFloatFormatter {
    /**
     * Maximum allowed value for `digitsBefore` and `digitsAfter`, guarding against excessive
     * padding allocations and unbounded power-of-10 loops on untrusted wire inputs.
     */
    public const val MAX_DIGITS: Int = 256

    /**
     * Decoded grouping mode (`0`): no grouping separators. Do not pass into [TextFromFloatFlags].
     */
    public const val GROUPING_NONE: Int = 0

    /** Decoded grouping mode (`1`): group by 3 digits. Do not pass into [TextFromFloatFlags]. */
    public const val GROUPING_BY3: Int = 1

    /** Decoded grouping mode (`2`): group by 4 digits. Do not pass into [TextFromFloatFlags]. */
    public const val GROUPING_BY4: Int = 2

    /**
     * Decoded grouping mode (`3`): Indian 3-then-2 grouping. Do not pass into [TextFromFloatFlags].
     */
    public const val GROUPING_BY32: Int = 3

    /**
     * Decoded separator (`0`): comma grouping, period decimal. Do not pass into
     * [TextFromFloatFlags].
     */
    public const val SEPARATOR_COMMA_PERIOD: Int = 0

    /**
     * Decoded separator (`1`): period grouping, comma decimal. Do not pass into
     * [TextFromFloatFlags].
     */
    public const val SEPARATOR_PERIOD_COMMA: Int = 1

    /**
     * Decoded separator (`2`): space grouping, comma decimal. Do not pass into
     * [TextFromFloatFlags].
     */
    public const val SEPARATOR_SPACE_COMMA: Int = 2

    /**
     * Decoded separator (`3`): underscore grouping, period decimal. Do not pass into
     * [TextFromFloatFlags].
     */
    public const val SEPARATOR_UNDER_PERIOD: Int = 3

    /** Decoded padding character (`\u0000`): omit padding. */
    public const val PAD_NONE: Char = '\u0000'

    /** Decoded padding character (`'0'`): pad with zeros. */
    public const val PAD_ZERO: Char = '0'

    /** Decoded padding character (`' '`): pad with spaces. */
    public const val PAD_SPACE: Char = ' '

    /**
     * Decodes [digitsBefore], [digitsAfter], and [TextFromFloatFlags] bitfield [flags] into a
     * [FloatFormatSpec].
     *
     * @throws IllegalArgumentException if [digitsBefore] or [digitsAfter] is outside
     *   `0..MAX_DIGITS`
     */
    @JvmStatic
    public fun parseSpec(digitsBefore: Int, digitsAfter: Int, flags: Int): FloatFormatSpec {
        require(digitsBefore in 0..MAX_DIGITS) {
            "digitsBefore must be in 0..$MAX_DIGITS, was $digitsBefore"
        }
        require(digitsAfter in 0..MAX_DIGITS) {
            "digitsAfter must be in 0..$MAX_DIGITS, was $digitsAfter"
        }
        val padAfter =
            when (flags and 3) {
                TextFromFloatFlags.PAD_AFTER_SPACE -> PAD_SPACE
                TextFromFloatFlags.PAD_AFTER_NONE -> PAD_NONE
                TextFromFloatFlags.PAD_AFTER_ZERO -> PAD_ZERO
                else -> PAD_SPACE
            }
        val padBefore =
            when (flags and (3 shl 2)) {
                TextFromFloatFlags.PAD_PRE_SPACE -> PAD_SPACE
                TextFromFloatFlags.PAD_PRE_NONE -> PAD_NONE
                TextFromFloatFlags.PAD_PRE_ZERO -> PAD_ZERO
                else -> PAD_SPACE
            }
        val grouping =
            when (flags and (3 shl 4)) {
                TextFromFloatFlags.GROUPING_BY3 -> GROUPING_BY3
                TextFromFloatFlags.GROUPING_BY4 -> GROUPING_BY4
                TextFromFloatFlags.GROUPING_BY32 -> GROUPING_BY32
                else -> GROUPING_NONE
            }
        val separator =
            when (flags and (3 shl 6)) {
                TextFromFloatFlags.SEPARATOR_PERIOD_COMMA -> SEPARATOR_PERIOD_COMMA
                TextFromFloatFlags.SEPARATOR_COMMA_PERIOD -> SEPARATOR_COMMA_PERIOD
                TextFromFloatFlags.SEPARATOR_SPACE_COMMA -> SEPARATOR_SPACE_COMMA
                else -> SEPARATOR_UNDER_PERIOD
            }
        val isRounding = (flags and TextFromFloatFlags.OPTIONS_ROUNDING) != 0
        val isNegativeParentheses = (flags and TextFromFloatFlags.OPTIONS_NEGATIVE_PARENTHESES) != 0
        val isLegacyMode = (flags and TextFromFloatFlags.LEGACY_MODE) != 0
        val isFullFormat = (flags and TextFromFloatFlags.FULL_FORMAT) != 0
        return FloatFormatSpec(
            digitsBefore = digitsBefore,
            digitsAfter = digitsAfter,
            padBefore = padBefore,
            padAfter = padAfter,
            grouping = grouping,
            separator = separator,
            isRounding = isRounding,
            isNegativeParentheses = isNegativeParentheses,
            isLegacyMode = isLegacyMode,
            isFullFormat = isFullFormat,
        )
    }

    /** Formats [value] according to [digitsBefore], [digitsAfter], and [flags]. */
    @JvmStatic
    public fun format(value: Float, digitsBefore: Int, digitsAfter: Int, flags: Int): String =
        format(value, parseSpec(digitsBefore, digitsAfter, flags))

    /** Formats [value] according to a parsed [FloatFormatSpec]. */
    @JvmStatic
    public fun format(value: Float, spec: FloatFormatSpec): String {
        if (spec.isFullFormat) {
            return value.toString()
        }
        return if (spec.isLegacyMode) {
            formatLegacy(value, spec.digitsBefore, spec.digitsAfter, spec.padBefore, spec.padAfter)
        } else {
            formatModern(
                rawValue = value,
                beforeDecimalPoint = spec.digitsBefore,
                afterDecimalPoint = spec.digitsAfter,
                pre = spec.padBefore,
                post = spec.padAfter,
                separator = spec.separator,
                grouping = spec.grouping,
                rounding = spec.isRounding,
                useParenthesesForNeg = spec.isNegativeParentheses,
            )
        }
    }

    private fun formatLegacy(
        rawValue: Float,
        beforeDecimalPoint: Int,
        afterDecimalPoint: Int,
        pre: Char,
        post: Char,
    ): String {
        val isNeg = rawValue < 0f
        val value = if (isNeg) -rawValue else rawValue
        val integerPart = value.toInt()
        var fractionalPart = value % 1f

        var integerPartString = integerPart.toString()
        val iLen = integerPartString.length
        if (iLen < beforeDecimalPoint) {
            val spacesToPad = beforeDecimalPoint - iLen
            if (pre != PAD_NONE) {
                integerPartString =
                    CharArray(spacesToPad) { pre }.concatToString() + integerPartString
            }
        } else if (iLen > beforeDecimalPoint) {
            integerPartString = integerPartString.substring(iLen - beforeDecimalPoint)
        }
        if (afterDecimalPoint == 0) {
            return (if (isNeg) "-" else "") + integerPartString
        }

        repeat(afterDecimalPoint) { fractionalPart *= 10f }
        fractionalPart = Math.round(fractionalPart).toFloat()
        repeat(afterDecimalPoint) { fractionalPart *= 0.1f }

        var fact = fractionalPart.toString()
        fact = fact.substring(2, min(fact.length, afterDecimalPoint + 2))
        var trim = fact.length
        for (i in fact.length - 1 downTo 0) {
            if (fact[i] != '0') break
            trim--
        }
        if (trim != fact.length) {
            fact = fact.substring(0, trim)
        }
        val len = fact.length
        if (post != PAD_NONE && len < afterDecimalPoint) {
            fact += CharArray(afterDecimalPoint - len) { post }.concatToString()
        }
        return (if (isNeg) "-" else "") + integerPartString + "." + fact
    }

    private fun formatModern(
        rawValue: Float,
        beforeDecimalPoint: Int,
        afterDecimalPoint: Int,
        pre: Char,
        post: Char,
        separator: Int,
        grouping: Int,
        rounding: Boolean,
        useParenthesesForNeg: Boolean,
    ): String {
        val groupSep: Char
        val decSep: Char
        when (separator) {
            SEPARATOR_PERIOD_COMMA -> {
                groupSep = '.'
                decSep = ','
            }
            SEPARATOR_SPACE_COMMA -> {
                groupSep = ' '
                decSep = ','
            }
            SEPARATOR_UNDER_PERIOD -> {
                groupSep = '_'
                decSep = '.'
            }
            else -> {
                groupSep = ','
                decSep = '.'
            }
        }
        val isNeg = rawValue < 0f
        val value = if (isNeg) -rawValue else rawValue
        val chars = toChars(value, beforeDecimalPoint, afterDecimalPoint, rounding)
        val str = chars.concatToString()
        var fractionalPart = value % 1f

        var integerPartString = str.substring(0, str.indexOf('.'))
        if (grouping != GROUPING_NONE) {
            val len = integerPartString.length
            when (grouping) {
                GROUPING_BY3 -> {
                    var i = len - 3
                    while (i > 0) {
                        integerPartString =
                            integerPartString.substring(0, i) +
                                groupSep +
                                integerPartString.substring(i)
                        i -= 3
                    }
                }
                GROUPING_BY4 -> {
                    var i = len - 4
                    while (i > 0) {
                        integerPartString =
                            integerPartString.substring(0, i) +
                                groupSep +
                                integerPartString.substring(i)
                        i -= 4
                    }
                }
                GROUPING_BY32 -> {
                    var i = len - 3
                    while (i > 0) {
                        integerPartString =
                            integerPartString.substring(0, i) +
                                groupSep +
                                integerPartString.substring(i)
                        i -= 2
                    }
                }
            }
        }
        val iLen = integerPartString.length
        if (iLen < beforeDecimalPoint) {
            val spacesToPad = beforeDecimalPoint - iLen
            if (pre != PAD_NONE) {
                integerPartString =
                    CharArray(spacesToPad) { pre }.concatToString() + integerPartString
            }
        } else if (iLen > beforeDecimalPoint) {
            integerPartString = integerPartString.substring(iLen - beforeDecimalPoint)
        }
        var effectiveAfter = afterDecimalPoint
        var trimAfter = effectiveAfter
        if (iLen + effectiveAfter > 9) {
            trimAfter = maxOf(1, 9 - iLen)
        }
        if (post == PAD_NONE) {
            effectiveAfter = trimAfter
        }

        if (effectiveAfter == 0) {
            if (!isNeg) return integerPartString
            return if (useParenthesesForNeg) "($integerPartString)" else "-$integerPartString"
        }

        repeat(trimAfter) { fractionalPart *= 10f }
        fractionalPart = Math.round(fractionalPart).toFloat()
        repeat(trimAfter) { fractionalPart *= 0.1f }

        var fact = fractionalPart.toString()
        fact = fact.substring(2, min(fact.length, effectiveAfter + 2))
        var trim = fact.length
        for (i in fact.length - 1 downTo 1) {
            if (fact[i] != '0') break
            trim--
        }
        if (trim != fact.length) {
            fact = fact.substring(0, trim)
        }
        val len = fact.length
        if (post != PAD_NONE && len < effectiveAfter) {
            fact += CharArray(effectiveAfter - len) { post }.concatToString()
        }
        if (!isNeg) return "$integerPartString$decSep$fact"
        return if (useParenthesesForNeg) {
            "($integerPartString$decSep$fact)"
        } else {
            "-$integerPartString$decSep$fact"
        }
    }

    private fun toChars(
        rawValue: Float,
        beforeDecimalPoint: Int,
        afterDecimalPoint: Int,
        rounding: Boolean,
    ): CharArray {
        val isNegative = rawValue < 0f
        var value = if (isNegative) -rawValue else rawValue

        var powerOf10 = 1L
        repeat(afterDecimalPoint) { powerOf10 *= 10L }

        if (rounding) {
            var roundingFactor = 0.5f
            repeat(afterDecimalPoint) { roundingFactor /= 10.0f }
            value += roundingFactor
        }

        val integerPart = value.toLong()
        val fractionalPart = value - integerPart

        var intLength = 1
        if (integerPart > 0L) {
            var tempInt = integerPart
            intLength = 0
            while (tempInt > 0L) {
                tempInt /= 10L
                intLength++
            }
        }
        val actualBefore = min(beforeDecimalPoint, intLength)

        val integerChars = CharArray(actualBefore)
        var tempInt = integerPart
        for (i in actualBefore - 1 downTo 0) {
            integerChars[i] = ('0'.code + (tempInt % 10L).toInt()).toChar()
            tempInt /= 10L
        }

        var tempFrac = (fractionalPart * powerOf10).toLong()
        var fracLength = 0
        if (afterDecimalPoint > 0) {
            var temp = tempFrac
            if (temp == 0L) {
                fracLength = 1
            } else {
                while (temp > 0L && temp % 10L == 0L) {
                    temp /= 10L
                }
                if (temp > 0L) {
                    var t = temp
                    while (t > 0L) {
                        t /= 10L
                        fracLength++
                    }
                }
            }
        }

        val actualAfter = min(afterDecimalPoint, fracLength)
        val fractionalChars = CharArray(actualAfter)
        tempFrac = (fractionalPart * powerOf10).toLong()
        for (i in actualAfter - 1 downTo 0) {
            fractionalChars[i] = ('0'.code + (tempFrac % 10L).toInt()).toChar()
            tempFrac /= 10L
        }

        val totalLength = (if (isNegative) 1 else 0) + actualBefore + 1 + actualAfter
        val result = CharArray(totalLength)
        var currentIndex = 0
        if (isNegative) {
            result[currentIndex++] = '-'
        }
        for (i in 0 until actualBefore) {
            result[currentIndex++] = integerChars[i]
        }
        result[currentIndex++] = '.'
        for (i in 0 until actualAfter) {
            result[currentIndex++] = fractionalChars[i]
        }
        return result
    }
}
