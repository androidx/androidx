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

package androidx.compose.ui.text

import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.MAX_LINE_BREAK_STRATEGY_VALUE
import androidx.compose.ui.text.style.MAX_LINE_BREAK_STRICTNESS_VALUE
import androidx.compose.ui.text.style.MAX_LINE_BREAK_WORD_BREAK_VALUE
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.text.style.isSpecified
import androidx.compose.ui.text.style.takeOrElse
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Fast JVM tests for each value class packed into [ParagraphStyle.bits]. Each test checks that all
 * valid values fit the `MAX_*` bound and round-trip through bit pack and unpack.
 *
 * If you add a new value or flag to one of these classes, you must bump its `MAX_*` bound and bit
 * width. If you do not, the test here fails and tells you what to update.
 */
@RunWith(JUnit4::class)
class StyleDomainLockTest {

    private fun intSweepProbes(): IntArray = buildSet {
        addAll(-64..256)
        for (bit in 0..31) {
            val shifted = 1 shl bit
            add(shifted)
            add(shifted - 1)
            add(shifted.inv())
        }
        add(0xFFFF)
        add(Int.MIN_VALUE)
        add(Int.MAX_VALUE)
    }
        .toIntArray()

    private fun outOfRangeProbes(maxValid: Int): IntArray = buildSet {
        addAll(
            listOf(
                Int.MIN_VALUE,
                -64,
                -2,
                -1,
                maxValid + 1,
                maxValid + 2,
                16,
                255,
                0xFFFF,
                Int.MAX_VALUE,
            )
        )
        for (bit in 0..30) {
            val shifted = 1 shl bit
            if (shifted > maxValid) add(shifted)
        }
    }
        .filter { it !in 0..maxValid }
        .toIntArray()

    private fun <T> assertValidFromToString(
        maxValue: Int,
        uncheckedConstruct: (Int) -> T,
    ): Set<Int> {
        val validValues =
            intSweepProbes().filter { uncheckedConstruct(it).toString() != "Invalid" }.toSet()
        assertThat(validValues).containsExactlyElementsIn(0..maxValue)
        return validValues
    }

    private fun <T> assertLockedValueDomain(
        maxValue: Int,
        expectedValues: Iterable<T>? = null,
        rawValueOf: (T) -> Int,
        uncheckedConstruct: ((Int) -> T)? = null,
        checkedValueOf: (Int) -> T,
        roundTripInStyle: (T) -> T?,
    ) {
        val expectedDomain = 0..maxValue
        if (expectedValues != null) {
            assertThat(expectedValues.map(rawValueOf)).containsExactlyElementsIn(expectedDomain)
        }
        if (uncheckedConstruct != null) {
            assertValidFromToString(maxValue, uncheckedConstruct)
        }
        for (rawValue in expectedDomain) {
            val instance = checkedValueOf(rawValue)
            assertThat(rawValueOf(instance)).isEqualTo(rawValue)
            assertThat(roundTripInStyle(instance)).isEqualTo(instance)
        }
        for (invalidValue in outOfRangeProbes(maxValue)) {
            assertFailsWith<IllegalArgumentException> { checkedValueOf(invalidValue) }
        }
    }

    @Test
    fun textAlign_domainMatchesMaxConstant_andPacksLosslessly() {
        assertLockedValueDomain(
            maxValue = TextAlign.MAX_TEXT_ALIGN_VALUE,
            expectedValues = TextAlign.values() + TextAlign.Unspecified,
            rawValueOf = { it.value },
            uncheckedConstruct = ::TextAlign,
            checkedValueOf = TextAlign::valueOf,
            roundTripInStyle = { ParagraphStyle(textAlign = it).textAlign },
        )
    }

    @Test
    fun textDirection_domainMatchesMaxConstant_andPacksLosslessly() {
        assertLockedValueDomain(
            maxValue = TextDirection.MAX_TEXT_DIRECTION_VALUE,
            rawValueOf = { it.value },
            uncheckedConstruct = ::TextDirection,
            checkedValueOf = TextDirection::valueOf,
            roundTripInStyle = { ParagraphStyle(textDirection = it).textDirection },
        )
    }

    @Test
    fun hyphens_domainMatchesMaxConstant_andPacksLosslessly() {
        assertLockedValueDomain(
            maxValue = Hyphens.MAX_HYPHENS_VALUE,
            rawValueOf = { it.value },
            uncheckedConstruct = ::Hyphens,
            checkedValueOf = Hyphens::valueOf,
            roundTripInStyle = { ParagraphStyle(hyphens = it).hyphens },
        )
    }

    @Test
    fun lineBreakSubfields_domainsMatchMaxConstants_andPackLosslessly() {
        val validStrategies =
            assertValidFromToString(MAX_LINE_BREAK_STRATEGY_VALUE, LineBreak::Strategy)
        val validStrictnesses =
            assertValidFromToString(MAX_LINE_BREAK_STRICTNESS_VALUE, LineBreak::Strictness)
        val validWordBreaks =
            assertValidFromToString(MAX_LINE_BREAK_WORD_BREAK_VALUE, LineBreak::WordBreak)

        for (strategy in validStrategies) {
            for (strictness in validStrictnesses) {
                for (wordBreak in validWordBreaks) {
                    val lineBreak =
                        if (strategy == 0 && strictness == 0 && wordBreak == 0) {
                            LineBreak.Unspecified
                        } else {
                            LineBreak(
                                strategy = LineBreak.Strategy(strategy.coerceAtLeast(1)),
                                strictness = LineBreak.Strictness(strictness.coerceAtLeast(1)),
                                wordBreak = LineBreak.WordBreak(wordBreak.coerceAtLeast(1)),
                            )
                        }
                    assertThat(ParagraphStyle(lineBreak = lineBreak).lineBreak).isEqualTo(lineBreak)
                }
            }
        }
    }

    @Test
    fun textMotion_presetsAvoidExtras_customUsesExtras() {
        val staticStyle = ParagraphStyle(textMotion = TextMotion.Static)
        val animatedStyle = ParagraphStyle(textMotion = TextMotion.Animated)
        assertThat(staticStyle.textMotion).isEqualTo(TextMotion.Static)
        assertThat(animatedStyle.textMotion).isEqualTo(TextMotion.Animated)
        assertThat(staticStyle.extras).isNull()
        assertThat(animatedStyle.extras).isNull()
    }

    @Test
    fun valueClassHelpers_valueOfFlagsIsSpecifiedAndTakeOrElse() {
        // LineBreak.isSpecified & copy
        assertThat(LineBreak.Unspecified.isSpecified).isFalse()
        for (specified in listOf(LineBreak.Simple, LineBreak.Heading, LineBreak.Paragraph)) {
            assertThat(specified.isSpecified).isTrue()
        }
        val copiedLineBreak =
            LineBreak.Simple.copy(
                strategy = LineBreak.Strategy.Balanced,
                strictness = LineBreak.Strictness.Loose,
                wordBreak = LineBreak.WordBreak.Phrase,
            )
        assertThat(copiedLineBreak).isEqualTo(LineBreak.Heading)

        // valueOf(0) and takeOrElse non-invocation when specified
        assertThat(TextAlign.valueOf(0)).isEqualTo(TextAlign.Unspecified)
        assertThat(TextDirection.valueOf(0)).isEqualTo(TextDirection.Unspecified)
        assertThat(Hyphens.valueOf(0)).isEqualTo(Hyphens.Unspecified)
        var fallbackInvoked = false
        assertThat(
                TextAlign.Center.takeOrElse {
                    fallbackInvoked = true
                    TextAlign.End
                }
            )
            .isEqualTo(TextAlign.Center)
        assertThat(
                TextDirection.Rtl.takeOrElse {
                    fallbackInvoked = true
                    TextDirection.Ltr
                }
            )
            .isEqualTo(TextDirection.Rtl)
        assertThat(
                Hyphens.Auto.takeOrElse {
                    fallbackInvoked = true
                    Hyphens.None
                }
            )
            .isEqualTo(Hyphens.Auto)
        assertThat(fallbackInvoked).isFalse()
    }
}
