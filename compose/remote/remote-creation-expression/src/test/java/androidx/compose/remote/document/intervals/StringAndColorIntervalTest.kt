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

import androidx.compose.remote.core.operations.ColorAttribute
import androidx.compose.remote.core.operations.TextTransform
import androidx.compose.remote.document.ColorAttributeType
import androidx.compose.remote.document.RemoteColorMath
import androidx.compose.remote.document.TextFromFloatFlags
import androidx.compose.remote.document.TextFromFloatFormatter
import androidx.compose.remote.document.TextTransformOperation
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.nextDown
import kotlin.math.nextUp
import kotlin.test.Test

class StringAndColorIntervalTest {

    @Test
    fun protocolConstantParityWithRemoteCore() {
        assertThat(ColorAttributeType.HUE).isEqualTo(ColorAttribute.COLOR_HUE.toInt())
        assertThat(ColorAttributeType.SATURATION).isEqualTo(ColorAttribute.COLOR_SATURATION.toInt())
        assertThat(ColorAttributeType.BRIGHTNESS).isEqualTo(ColorAttribute.COLOR_BRIGHTNESS.toInt())
        assertThat(ColorAttributeType.RED).isEqualTo(ColorAttribute.COLOR_RED.toInt())
        assertThat(ColorAttributeType.GREEN).isEqualTo(ColorAttribute.COLOR_GREEN.toInt())
        assertThat(ColorAttributeType.BLUE).isEqualTo(ColorAttribute.COLOR_BLUE.toInt())
        assertThat(ColorAttributeType.ALPHA).isEqualTo(ColorAttribute.COLOR_ALPHA.toInt())

        assertThat(TextTransformOperation.TO_LOWERCASE).isEqualTo(TextTransform.TEXT_TO_LOWERCASE)
        assertThat(TextTransformOperation.TO_UPPERCASE).isEqualTo(TextTransform.TEXT_TO_UPPERCASE)
        assertThat(TextTransformOperation.TRIM).isEqualTo(TextTransform.TEXT_TRIM)
        assertThat(TextTransformOperation.CAPITALIZE).isEqualTo(TextTransform.TEXT_CAPITALIZE)
        assertThat(TextTransformOperation.UPPERCASE_FIRST_CHAR)
            .isEqualTo(TextTransform.TEXT_UPPERCASE_FIRST_CHAR)
    }

    @Test
    fun stringSummary_textFromFloatMergeSubstringAndTransform() {
        val exact =
            StringAndColorIntervalOps.summarizeTextFromFloat(
                FloatInterval.point(7f),
                digitsBefore = 2,
                digitsAfter = 0,
                flags = TextFromFloatFlags.PAD_PRE_ZERO or TextFromFloatFlags.PAD_AFTER_NONE,
            )
        assertThat(exact.constantValue).isEqualTo("07")
        assertThat(exact.length).isEqualTo(IntInterval.point(2))

        val ranged =
            StringAndColorIntervalOps.summarizeTextFromFloat(
                FloatInterval(0f, 59f),
                digitsBefore = 2,
                digitsAfter = 0,
                flags = TextFromFloatFlags.PAD_PRE_ZERO or TextFromFloatFlags.PAD_AFTER_NONE,
            )
        assertThat(ranged.constantValue).isNull()
        assertThat(ranged.length).isEqualTo(IntInterval.point(2))
        assertThat(ranged.isAsciiOnly).isTrue()

        // Negative or excessively large digitsBefore/digitsAfter return UNKNOWN instead of throwing
        for ((before, after) in
            listOf(
                -1 to 2,
                2 to -1,
                TextFromFloatFormatter.MAX_DIGITS + 1 to 0,
                0 to TextFromFloatFormatter.MAX_DIGITS + 1,
            )) {
            assertThat(
                    StringAndColorIntervalOps.summarizeTextFromFloat(
                        FloatInterval.point(7f),
                        digitsBefore = before,
                        digitsAfter = after,
                        flags = 0,
                    )
                )
                .isEqualTo(StringSummary.UNKNOWN)
            assertThat(
                    StringAndColorIntervalOps.summarizeTextFromFloat(
                        FloatInterval(0f, 59f),
                        digitsBefore = before,
                        digitsAfter = after,
                        flags = 0,
                    )
                )
                .isEqualTo(StringSummary.UNKNOWN)
        }

        val merged =
            StringAndColorIntervalOps.summarizeTextMerge(
                StringSummary.constant("07"),
                StringSummary.constant(":30"),
            )
        assertThat(merged.constantValue).isEqualTo("07:30")
        assertThat(merged.length).isEqualTo(IntInterval.point(5))

        val sub =
            StringAndColorIntervalOps.summarizeSubstring(
                src = StringSummary.constant("HelloWorld"),
                start = FloatInterval.point(5f),
                len = FloatInterval.point(-1f),
            )
        assertThat(sub.constantValue).isEqualTo("World")

        val trimmed =
            StringAndColorIntervalOps.summarizeTextTransform(
                src = StringSummary.constant("  abc  "),
                start = FloatInterval.ZERO,
                len = FloatInterval.point(-1f),
                operation = TextTransformOperation.TRIM,
            )
        assertThat(trimmed.constantValue).isEqualTo("abc")

        val upper =
            StringAndColorIntervalOps.summarizeTextTransform(
                src = StringSummary.constant("abc"),
                start = FloatInterval.ZERO,
                len = FloatInterval.point(-1f),
                operation = TextTransformOperation.TO_UPPERCASE,
            )
        assertThat(upper.length).isEqualTo(IntInterval.point(3))
        assertThat(upper.isAsciiOnly).isFalse()

        // Open adjacent-float interval regression (no Int.MAX_VALUE / Int.MIN_VALUE overflow)
        val openAdjacentPos =
            StringAndColorIntervalOps.summarizeTextFromFloat(
                FloatInterval(10f.nextDown(), 10f, minInclusive = false, maxInclusive = false),
                digitsBefore = 4,
                digitsAfter = 2,
                flags = TextFromFloatFlags.PAD_PRE_NONE or TextFromFloatFlags.PAD_AFTER_NONE,
            )
        assertThat(openAdjacentPos).isEqualTo(StringSummary.UNKNOWN)
        val openAdjacentNeg =
            StringAndColorIntervalOps.summarizeTextFromFloat(
                FloatInterval(-10f, (-10f).nextUp(), minInclusive = false, maxInclusive = false),
                digitsBefore = 4,
                digitsAfter = 2,
                flags = TextFromFloatFlags.PAD_PRE_NONE or TextFromFloatFlags.PAD_AFTER_NONE,
            )
        assertThat(openAdjacentNeg).isEqualTo(StringSummary.UNKNOWN)
        assertThat(
                StringAndColorIntervalOps.summarizeTextFromFloat(
                    FloatInterval.EMPTY,
                    digitsBefore = 2,
                    digitsAfter = 0,
                    flags = 0,
                )
            )
            .isEqualTo(StringSummary.UNKNOWN)

        // TextFromFloat negative parentheses, grouping, rounding, legacy, and FULL_FORMAT
        val negParens =
            StringAndColorIntervalOps.summarizeTextFromFloat(
                FloatInterval(-9999f, -1000f),
                digitsBefore = 6,
                digitsAfter = 2,
                flags =
                    TextFromFloatFlags.PAD_PRE_NONE or
                        TextFromFloatFlags.PAD_AFTER_ZERO or
                        TextFromFloatFlags.GROUPING_BY3 or
                        TextFromFloatFlags.OPTIONS_ROUNDING or
                        TextFromFloatFlags.OPTIONS_NEGATIVE_PARENTHESES,
            )
        assertThat(negParens.length.min).isAtLeast(10)
        assertThat(negParens.isAsciiOnly).isTrue()

        val legacyRange =
            StringAndColorIntervalOps.summarizeTextFromFloat(
                FloatInterval(0f, 999f),
                digitsBefore = 3,
                digitsAfter = 2,
                flags =
                    TextFromFloatFlags.PAD_PRE_NONE or
                        TextFromFloatFlags.PAD_AFTER_NONE or
                        TextFromFloatFlags.LEGACY_MODE,
            )
        assertThat(legacyRange.length).isEqualTo(IntInterval(2, 6))

        val fullFmt =
            StringAndColorIntervalOps.summarizeTextFromFloat(
                FloatInterval(0f, 10f),
                digitsBefore = 2,
                digitsAfter = 2,
                flags = TextFromFloatFlags.FULL_FORMAT,
            )
        assertThat(fullFmt.length).isEqualTo(IntInterval(3, 16))

        // Non-point summarizeSubstring exercising summarizeFiniteSubstringLength
        val finiteSub =
            StringAndColorIntervalOps.summarizeSubstring(
                src = StringSummary(IntInterval(10, 12), isAsciiOnly = true),
                start = FloatInterval(2f, 4f),
                len = FloatInterval(3f, 5f),
            )
        assertThat(3 in finiteSub.length && 5 in finiteSub.length).isTrue()
        assertThat(finiteSub.length.min).isAtLeast(3)
        assertThat(finiteSub.length.max).isAtMost(6)

        val mixedSub =
            StringAndColorIntervalOps.summarizeSubstring(
                src = StringSummary(IntInterval(10, 10), isAsciiOnly = true),
                start = FloatInterval.point(2f),
                len = FloatInterval(-1f, 3f),
            )
        assertThat(8 in mixedSub.length && 0 in mixedSub.length).isTrue()

        // Additional TextTransform operations & StringSummary union/equals
        val cap =
            StringAndColorIntervalOps.summarizeTextTransform(
                src = StringSummary(IntInterval(2, 5), isAsciiOnly = true),
                start = FloatInterval.ZERO,
                len = FloatInterval.point(-1f),
                operation = TextTransformOperation.CAPITALIZE,
            )
        assertThat(cap.length).isEqualTo(IntInterval(2, 5))
        val nonAsciiLower =
            StringAndColorIntervalOps.summarizeTextTransform(
                src = StringSummary(IntInterval(2, 5), isAsciiOnly = false),
                start = FloatInterval.ZERO,
                len = FloatInterval.point(-1f),
                operation = TextTransformOperation.TO_LOWERCASE,
            )
        assertThat(nonAsciiLower.length).isEqualTo(IntInterval(1, 15))
        val dynTrim =
            StringAndColorIntervalOps.summarizeTextTransform(
                src = StringSummary(IntInterval(2, 5), isAsciiOnly = true),
                start = FloatInterval.ZERO,
                len = FloatInterval.point(-1f),
                operation = TextTransformOperation.TRIM,
            )
        assertThat(dynTrim.length).isEqualTo(IntInterval(0, 5))
        assertThat(
                StringAndColorIntervalOps.summarizeTextTransform(
                    src = StringSummary.constant("abc"),
                    start = FloatInterval.ZERO,
                    len = FloatInterval.point(-1f),
                    operation = 99,
                )
            )
            .isEqualTo(StringSummary.UNKNOWN)
        val sUnion = StringSummary.constant("ab").union(StringSummary.constant("cde"))
        assertThat(sUnion.constantValue).isNull()
        assertThat(sUnion.length).isEqualTo(IntInterval(2, 3))
        assertThat(StringSummary.constant("ab").union(StringSummary.constant("ab")))
            .isEqualTo(StringSummary.constant("ab"))
        assertThat(sUnion.hashCode())
            .isEqualTo(StringSummary(IntInterval(2, 3), isAsciiOnly = true).hashCode())
    }

    @Test
    fun colorInterval_argbHsvTweenMultiplyAndAttribute() {
        val red = ColorInterval.constant(0xFFFF0000.toInt())
        val blue = ColorInterval.constant(0xFF0000FF.toInt())

        assertThat(red.toArgbOrNull()).isEqualTo(0xFFFF0000.toInt())
        assertThat(StringAndColorIntervalOps.summarizeColorAttribute(red, ColorAttributeType.RED))
            .isEqualTo(FloatInterval.point(1f))
        assertThat(StringAndColorIntervalOps.summarizeColorAttribute(red, ColorAttributeType.BLUE))
            .isEqualTo(FloatInterval.point(0f))

        val tween = StringAndColorIntervalOps.summarizeColorTween(red, blue, FloatInterval(0f, 1f))
        assertThat(tween.alpha).isEqualTo(IntInterval.point(255))
        assertThat(tween.red).isEqualTo(IntInterval(0, 255))
        assertThat(tween.green).isEqualTo(IntInterval.point(0))
        assertThat(tween.blue).isEqualTo(IntInterval(0, 255))

        // Point t inside [0, 1] and outside [0, 1]
        assertThat(
                StringAndColorIntervalOps.summarizeColorTween(red, blue, FloatInterval.point(0f))
            )
            .isEqualTo(red)
        assertThat(
                StringAndColorIntervalOps.summarizeColorTween(red, blue, FloatInterval.point(1f))
            )
            .isEqualTo(blue)
        val midPoint =
            StringAndColorIntervalOps.summarizeColorTween(red, blue, FloatInterval.point(0.5f))
        assertThat(midPoint.toArgbOrNull())
            .isEqualTo(
                RemoteColorMath.interpolateColor(0xFFFF0000.toInt(), 0xFF0000FF.toInt(), 0.5f)
            )
        assertThat(
                StringAndColorIntervalOps.summarizeColorTween(red, blue, FloatInterval.point(-0.1f))
            )
            .isEqualTo(ColorInterval.UNKNOWN)
        assertThat(
                StringAndColorIntervalOps.summarizeColorTween(red, blue, FloatInterval.point(1.5f))
            )
            .isEqualTo(ColorInterval.UNKNOWN)

        // Non-point ColorInterval soundness near t = 1f.nextDown() (both point and ranged t)
        val c1Range =
            ColorInterval(
                alpha = IntInterval(1, 3),
                red = IntInterval(2, 10),
                green = IntInterval.point(0),
                blue = IntInterval.point(0),
            )
        val c2Range =
            ColorInterval(
                alpha = IntInterval(4, 10),
                red = IntInterval(6, 20),
                green = IntInterval.point(0),
                blue = IntInterval.point(0),
            )
        val nearOnePoint = FloatInterval.point(1f.nextDown())
        val nearOneRange = FloatInterval(1f.nextDown(), 1f)
        val interiorSample =
            RemoteColorMath.interpolateColor(
                RemoteColorMath.argb(2, 3, 0, 0),
                RemoteColorMath.argb(4, 6, 0, 0),
                1f.nextDown(),
            )
        for (tInterval in listOf(nearOnePoint, nearOneRange)) {
            val bound = StringAndColorIntervalOps.summarizeColorTween(c1Range, c2Range, tInterval)
            assertWithMessage("t=$tInterval alpha=${bound.alpha}")
                .that(RemoteColorMath.alpha(interiorSample) in bound.alpha)
                .isTrue()
            assertWithMessage("t=$tInterval red=${bound.red}")
                .that(RemoteColorMath.red(interiorSample) in bound.red)
                .isTrue()
            // Exact constant channels remain un-widened
            assertThat(bound.green).isEqualTo(IntInterval.point(0))
            assertThat(bound.blue).isEqualTo(IntInterval.point(0))
        }
        // Exhaustively verify soundness for all interior pairs in c1Range x c2Range at
        // 1f.nextDown()
        val rangedBound =
            StringAndColorIntervalOps.summarizeColorTween(c1Range, c2Range, nearOneRange)
        for (a1 in 1..3) {
            for (a2 in 4..10) {
                for (r1 in 2..10) {
                    for (r2 in 6..20) {
                        val sample =
                            RemoteColorMath.interpolateColor(
                                RemoteColorMath.argb(a1, r1, 0, 0),
                                RemoteColorMath.argb(a2, r2, 0, 0),
                                1f.nextDown(),
                            )
                        assertThat(RemoteColorMath.alpha(sample) in rangedBound.alpha).isTrue()
                        assertThat(RemoteColorMath.red(sample) in rangedBound.red).isTrue()
                    }
                }
            }
        }

        // Gamma round-trip dip regression across ALL 0..255 channel values at 0f.nextUp() and
        // 1f.nextDown()
        val samples = floatArrayOf(0f, 0f.nextUp(), 0.5f, 1f.nextDown(), 1f)
        for (r in 0..255) {
            val c = RemoteColorMath.argb(255, r, r, r)
            val cInt = ColorInterval.constant(c)
            val selfTween =
                StringAndColorIntervalOps.summarizeColorTween(cInt, cInt, FloatInterval(0f, 1f))
            val toWhiteTween =
                StringAndColorIntervalOps.summarizeColorTween(
                    cInt,
                    ColorInterval.constant(0xFFFFFFFF.toInt()),
                    FloatInterval(0f, 1f),
                )
            for (tau in samples) {
                val selfSample = RemoteColorMath.interpolateColor(c, c, tau)
                assertWithMessage(
                        "r=$r tau=$tau selfTween=${selfTween.red} missed ${RemoteColorMath.red(selfSample)}"
                    )
                    .that(RemoteColorMath.red(selfSample) in selfTween.red)
                    .isTrue()
                val whiteSample = RemoteColorMath.interpolateColor(c, 0xFFFFFFFF.toInt(), tau)
                assertWithMessage(
                        "r=$r tau=$tau toWhiteTween=${toWhiteTween.red} missed ${RemoteColorMath.red(whiteSample)}"
                    )
                    .that(RemoteColorMath.red(whiteSample) in toWhiteTween.red)
                    .isTrue()
            }
        }
        assertThat(StringAndColorIntervalOps.summarizeColorTween(red, blue, FloatInterval.EMPTY))
            .isEqualTo(ColorInterval.UNKNOWN)
        assertThat(
                StringAndColorIntervalOps.summarizeColorTween(red, blue, FloatInterval(-0.5f, 1.5f))
            )
            .isEqualTo(ColorInterval.UNKNOWN)

        // summarizeColorArgb: exact, ranged, open-endpoint rounding, and out-of-range spillover
        val exactArgb =
            StringAndColorIntervalOps.summarizeColorArgb(
                FloatInterval.point(1f),
                FloatInterval.point(0.5f),
                FloatInterval.point(0f),
                FloatInterval.point(1f),
            )
        assertThat(exactArgb.toArgbOrNull()).isEqualTo(RemoteColorMath.toArgb(1f, 0.5f, 0f, 1f))
        val rangedArgb =
            StringAndColorIntervalOps.summarizeColorArgb(
                FloatInterval(0.5f, 1f),
                FloatInterval(0f, 1f, minInclusive = true, maxInclusive = false),
                FloatInterval.point(0.25f),
                FloatInterval.point(0.75f),
            )
        assertThat(rangedArgb.alpha).isEqualTo(IntInterval(128, 255))
        assertThat(255 in rangedArgb.red).isTrue() // (1f.nextDown() * 255f + 0.5f).toInt() == 255!
        val overflowArgb =
            StringAndColorIntervalOps.summarizeColorArgb(
                FloatInterval.point(1f),
                FloatInterval.point(0.5f),
                FloatInterval.point(0.5f),
                FloatInterval(0f, 2f), // overflows blue byte into green/red/alpha
            )
        assertThat(overflowArgb).isEqualTo(ColorInterval.UNKNOWN)

        val hsv =
            StringAndColorIntervalOps.summarizeColorHsv(
                alpha = 0x80,
                hue = FloatInterval(0f, 1f, minInclusive = true, maxInclusive = false),
                saturation = FloatInterval.point(0.5f),
                value = FloatInterval.point(1f),
            )
        assertThat(hsv.alpha).isEqualTo(IntInterval.point(0x80))
        assertThat(hsv.red.min).isAtLeast(127)
        assertThat(hsv.red.max).isEqualTo(255)
        val hsvIncludingOne =
            StringAndColorIntervalOps.summarizeColorHsv(
                alpha = 0xFF,
                hue = FloatInterval(0f, 1f),
                saturation = FloatInterval.point(0.5f),
                value = FloatInterval.point(1f),
            )
        assertThat(hsvIncludingOne.red.min).isEqualTo(0)
        val hsvOutOfRange =
            StringAndColorIntervalOps.summarizeColorHsv(
                alpha = 0xFF,
                hue = FloatInterval(-1f, 2f),
                saturation = FloatInterval.point(0.5f),
                value = FloatInterval.point(1f),
            )
        assertThat(hsvOutOfRange.red).isEqualTo(ColorInterval.BYTE_RANGE)

        val multiplied = StringAndColorIntervalOps.summarizeColorMultiply(red, blue)
        assertThat(multiplied.toArgbOrNull()).isEqualTo(0xFF000000.toInt())
        val rangedColor =
            ColorInterval(
                alpha = IntInterval(128, 255),
                red = IntInterval(200, 255),
                green = IntInterval(0, 50),
                blue = IntInterval(0, 50),
            )
        assertThat(rangedColor.toArgbOrNull()).isNull()
        val nonPointMul = StringAndColorIntervalOps.summarizeColorMultiply(rangedColor, red)
        assertThat(nonPointMul.blue).isEqualTo(IntInterval.point(0))
        assertThat(nonPointMul.red.min).isAtLeast(200)

        // Non-point summarizeColorAttribute across all attributes
        assertThat(
                StringAndColorIntervalOps.summarizeColorAttribute(
                    rangedColor,
                    ColorAttributeType.ALPHA,
                )
            )
            .isEqualTo(FloatInterval(128 / 255f, 1f))
        assertThat(
                StringAndColorIntervalOps.summarizeColorAttribute(
                    rangedColor,
                    ColorAttributeType.BRIGHTNESS,
                )
            )
            .isEqualTo(FloatInterval(200 / 255f, 1f))
        val sat =
            StringAndColorIntervalOps.summarizeColorAttribute(
                rangedColor,
                ColorAttributeType.SATURATION,
            )
        assertThat(sat.min).isGreaterThan(0.7f)
        assertThat(sat.max).isEqualTo(1f)
        assertThat(
                StringAndColorIntervalOps.summarizeColorAttribute(
                    rangedColor,
                    ColorAttributeType.HUE,
                )
            )
            .isEqualTo(FloatInterval(0f, 1f, minInclusive = true, maxInclusive = false))
        assertThat(StringAndColorIntervalOps.summarizeColorAttribute(rangedColor, 99))
            .isEqualTo(FloatInterval.UNBOUNDED)

        // ColorInterval union/equals/hashCode
        val cUnion = red.union(blue)
        assertThat(cUnion.red).isEqualTo(IntInterval(0, 255))
        assertThat(cUnion.blue).isEqualTo(IntInterval(0, 255))
        assertThat(cUnion).isEqualTo(blue.union(red))
        assertThat(cUnion.hashCode()).isEqualTo(blue.union(red).hashCode())
    }
}
