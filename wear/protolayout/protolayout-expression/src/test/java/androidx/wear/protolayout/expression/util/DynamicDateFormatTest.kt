/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.protolayout.expression.util

import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.icu.util.ULocale
import android.text.format.DateFormat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInstant
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.expression.pipeline.DynamicTypeBindingRequest
import androidx.wear.protolayout.expression.pipeline.DynamicTypeEvaluator
import androidx.wear.protolayout.expression.pipeline.DynamicTypeValueReceiver
import com.google.common.truth.Expect
import com.google.common.truth.Truth.assertWithMessage
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters

@RunWith(AndroidJUnit4::class)
class DynamicDateFormatTest {
    // Using expectation loops instead of parameterized tests because there are too many tests (note
    // the multiplication in all available locales), too expensive with test infra boilerplate.
    @get:Rule val expect = Expect.create()

    enum class Case(
        val pattern: String,
        val instant: Instant,
        val expectedEnglish: String,
    ) {
        EMPTY(
            pattern = "",
            instant = MIDNIGHT,
            expectedEnglish = "",
        ),
        // Constant patterns.
        CONSTANT_AT_START(
            pattern = ":::h",
            instant = MIDNIGHT,
            expectedEnglish = ":::12",
        ),
        CONSTANT_AT_MIDDLE(
            pattern = "h:::h",
            instant = MIDNIGHT,
            expectedEnglish = "12:::12",
        ),
        CONSTANT_AT_END(
            pattern = "h:::",
            instant = MIDNIGHT,
            expectedEnglish = "12:::",
        ),
        // Escape patterns.
        ESCAPED_AT_START(
            pattern = "'Time is' h",
            instant = MIDNIGHT.plus((3.hours + 20.minutes).toJavaDuration()),
            expectedEnglish = "Time is 3",
        ),
        ESCAPED_AT_MIDDLE(
            pattern = "h 'h' m",
            instant = MIDNIGHT.plus((3.hours + 20.minutes).toJavaDuration()),
            expectedEnglish = "3 h 20",
        ),
        ESCAPED_AT_END(
            pattern = "h 'is the time'",
            instant = MIDNIGHT.plus((3.hours + 20.minutes).toJavaDuration()),
            expectedEnglish = "3 is the time",
        ),
        ESCAPED_QUOTE_AT_START(
            pattern = "''h",
            instant = MIDNIGHT.plus((3.hours + 20.minutes).toJavaDuration()),
            expectedEnglish = "'3",
        ),
        ESCAPED_QUOTE_AT_MIDDLE(
            pattern = "h''m",
            instant = MIDNIGHT.plus((3.hours + 20.minutes).toJavaDuration()),
            expectedEnglish = "3'20",
        ),
        ESCAPED_QUOTE_AT_END(
            pattern = "h''",
            instant = MIDNIGHT.plus((3.hours + 20.minutes).toJavaDuration()),
            expectedEnglish = "3'",
        ),
        ESCAPED_QUOTE_IN_ESCAPED(
            pattern = "'Time''s' h",
            instant = MIDNIGHT.plus((3.hours + 20.minutes).toJavaDuration()),
            expectedEnglish = "Time's 3",
        ),
        // Hours patterns.
        HOURS_PADDED_MIDNIGHT(
            pattern = "HHHH:KKKK:hhhh:kkkk:aaaa",
            instant = MIDNIGHT,
            expectedEnglish = "0000:0000:0012:0024:AM",
        ),
        HOURS_PADDED_NOON(
            pattern = "HHHH:KKKK:hhhh:kkkk:aaaa",
            instant = MIDNIGHT.plus(Duration.ofHours(12)),
            expectedEnglish = "0012:0000:0012:0012:PM",
        ),
        HOURS_PADDED_A_M(
            pattern = "HHHH:KKKK:hhhh:kkkk:aaaa",
            instant = MIDNIGHT.plus(Duration.ofHours(6)),
            expectedEnglish = "0006:0006:0006:0006:AM",
        ),
        HOURS_PADDED_P_M(
            pattern = "H:K:h:k:a",
            instant = MIDNIGHT.plus(Duration.ofHours(18)),
            expectedEnglish = "18:6:6:18:PM",
        ),
        HOURS_NOT_PADDED_MIDNIGHT(
            pattern = "H:K:h:k:a",
            instant = MIDNIGHT,
            expectedEnglish = "0:0:12:24:AM",
        ),
        HOURS_NOT_PADDED_NOON(
            pattern = "H:K:h:k:a",
            instant = MIDNIGHT.plus(Duration.ofHours(12)),
            expectedEnglish = "12:0:12:12:PM",
        ),
        HOURS_NOT_PADDED_A_M(
            pattern = "H:K:h:k:a",
            instant = MIDNIGHT.plus(Duration.ofHours(6)),
            expectedEnglish = "6:6:6:6:AM",
        ),
        HOURS_NOT_PADDED_P_M(
            pattern = "H:K:h:k:a",
            instant = MIDNIGHT.plus(Duration.ofHours(18)),
            expectedEnglish = "18:6:6:18:PM",
        ),
        // Minute patterns.
        MINUTES_PADDED_ZERO(
            pattern = "mmmm",
            instant = MIDNIGHT,
            expectedEnglish = "0000",
        ),
        MINUTES_PADDED_SINGLE_DIGIT(
            pattern = "mmmm",
            instant = MIDNIGHT.plus(Duration.ofMinutes(5)),
            expectedEnglish = "0005",
        ),
        MINUTES_PADDED_DOUBLE_DIGIT(
            pattern = "mmmm",
            instant = MIDNIGHT.plus(Duration.ofMinutes(15)),
            expectedEnglish = "0015",
        ),
        MINUTES_NOT_PADDED_ZERO(
            pattern = "m",
            instant = MIDNIGHT,
            expectedEnglish = "0",
        ),
        MINUTES_NOT_PADDED_SINGLE_DIGIT(
            pattern = "m",
            instant = MIDNIGHT.plus(Duration.ofMinutes(5)),
            expectedEnglish = "5",
        ),
        MINUTES_NOT_PADDED_DOUBLE_DIGIT(
            pattern = "m",
            instant = MIDNIGHT.plus(Duration.ofMinutes(15)),
            expectedEnglish = "15",
        ),
    }

    @Test
    fun matchesExpectedEnglish() {
        for (case in Case.values()) {
            expectFormatsTo(
                message = case.name,
                pattern = case.pattern,
                instant = case.instant,
                locale = EN,
                expected = case.expectedEnglish,
            )
        }
    }

    /**
     * Tests that [Case.pattern] formats to the same value [SimpleDateFormat] does, in all available
     * locales.
     */
    @Test
    fun matchesSimpleDateFormat() {
        for (case in Case.values()) {
            for (locale in Locale.getAvailableLocales()) {
                expectMatchesSimpleDateFormat(
                    message = "${case.name}_$locale",
                    pattern = case.pattern,
                    instant = case.instant,
                    locale = locale,
                )
            }
        }
    }

    /**
     * Tests that the best date time pattern for "hmm" matches [SimpleDateFormat], in all available
     * locales.
     */
    @Test
    fun bestDateTimePattern_matchesSimpleDateFormat() {
        for (locale in Locale.getAvailableLocales()) {
            val pattern = DateFormat.getBestDateTimePattern(locale, "hmm")
            expectMatchesSimpleDateFormat(
                message = "${pattern}_$locale",
                pattern = pattern,
                instant = MIDNIGHT,
                locale = locale,
            )
        }
    }

    /** Expects the [pattern] formats to [expected], in the given [locale] and [instant]. */
    private fun expectFormatsTo(
        message: String,
        pattern: String,
        instant: Instant,
        locale: Locale,
        expected: String,
    ) {
        val formatter = DynamicDateFormat(pattern, locale, TIME_ZONE)

        val actual: String =
            formatter.format(DynamicInstant.withSecondsPrecision(instant)).evaluate(locale)

        expect.withMessage(message).that(actual).isEqualTo(expected)
    }

    /**
     * Expects the [pattern] formats to the same value [SimpleDateFormat] does, in the given
     * [locale] and [instant].
     */
    private fun expectMatchesSimpleDateFormat(
        message: String,
        pattern: String,
        instant: Instant,
        locale: Locale,
    ) {
        expectFormatsTo(
            message = message,
            pattern = pattern,
            instant = instant,
            locale = locale,
            expected =
                SimpleDateFormat(pattern, locale)
                    .also { it.timeZone = TimeZone.getTimeZone(TIME_ZONE.id) }
                    .format(Date.from(instant))
        )
    }

    /** Synchronously evaluates the [DynamicString] using [DynamicTypeEvaluator]. */
    private fun DynamicString.evaluate(locale: Locale): String {
        lateinit var result: String
        val evaluator = DynamicTypeEvaluator(DynamicTypeEvaluator.Config.Builder().build())
        evaluator
            .bind(
                DynamicTypeBindingRequest.forDynamicString(
                    this@evaluate,
                    ULocale.forLocale(locale),
                    { it.run() }, // Synchronous executor
                    object : DynamicTypeValueReceiver<String> {
                        override fun onData(newData: String) {
                            result = newData
                        }

                        override fun onInvalidated() {
                            throw AssertionError("DynamicString invalidated: ${this@evaluate}")
                        }
                    }
                )
            )
            .startEvaluation()
        return result
    }

    companion object {
        private val TIME_ZONE = ZoneId.of("UTC")
        private val MIDNIGHT = Instant.EPOCH // Epoch is midnight in UTC.
        private val EN = Locale.ENGLISH
    }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
class DynamicDateFormatFailingTest(private val case: Case) {
    enum class Case(val expected: Exception, val pattern: String) {
        UNSUPPORTED_LETTER(
            IllegalArgumentException("Illegal pattern character 'y'"),
            "hh:mm yyyy-MM-dd"
        ),
        ODD_QUOTES(IllegalArgumentException("Unterminated quote"), "'''"),
    }

    @Test
    fun fails() {
        val exception =
            assertFailsWith(case.expected::class, case.name) { DynamicDateFormat(case.pattern) }

        assertWithMessage(case.name)
            .that(exception)
            .hasMessageThat()
            .isEqualTo(case.expected.message)
    }

    companion object {
        @Parameters @JvmStatic fun parameters() = Case.values()
    }
}
